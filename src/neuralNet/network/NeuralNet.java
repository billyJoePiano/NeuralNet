package neuralNet.network;

import neuralNet.evolve.*;
import neuralNet.neuron.*;
import neuralNet.util.*;

import java.io.*;
import java.util.*;

public abstract class NeuralNet<S extends Sensable<S>,
                            N extends NeuralNet<S, N, C>,
                            C extends DecisionConsumer<S, C, ?>>
        implements DecisionProvider<S, N, C> {

    private enum Generation {
        CURRENT;
        private long generation = 0;
    }

    public static long getCurrentGeneration() { return Generation.CURRENT.generation; }
    public static long nextGeneration() { return ++Generation.CURRENT.generation; }

    private final Generation current = Generation.CURRENT; //ensures that the current generation value will be serialized/deserialized

    private Set<SignalProvider> neurons = new SerializableWeakHashSet<>();
    private SerializableWeakRef<NoOp> noOp;

    private transient Set<SignalProvider> neuronsView = ((SerializableWeakHashSet<SignalProvider>)this.neurons).getView();
    private transient S sensedObjected;
    private transient long round = 0;

    protected Object readResolve() throws ObjectStreamException {
        this.neuronsView = ((SerializableWeakHashSet<SignalProvider>)this.neurons).getView();
        return this;
    }

    protected NeuralNet() { }

    /**
     * Makes clones of all the neurons from the cloned NeuralNet, mapping the old neurons to the new ones,
     * and then replaces the inputs and consumers of each copied neuron using the map of old to new.
     *
     * IMPORTANT: It is best to ensure that the NeuralNet being cloned has a valid neurons set BEFORE
     * undertaking the cloneNeurons operation.  If there are unaccounted neurons that are reachable
     * from accounted neurons (including decisionNodes and sensorNodes), they will still be found, but
     * at a performance cost.
     *
     * Note that this operation can't be done immediately in a NeuralNet clone constructor, because the
     * subclass' constructor may need to perform some initialization tasks before the full cloning operation
     * is ready to commence (e.g. populate sensor and decision nodes). This method should be called from the
     * subclass's constructor after it has finished those initializations.
     *
     *
     * @param cloneFrom the neural net we are cloning
     * @param providersMap any special signalProviders substitutions to use.  NOTE THAT ANY PASSED MAP WILL BE MUTATED.
     *                      If null, an empty HashMap will be constructed and used to map neurons from the old
     *                      net to cloned copies for the new one
     *
     * @param consumersMap any special signalConsumers substitutions to use.  NOTE THAT ANY PASSED MAP WILL BE MUTATED.
     *                      If null, an empty HashMap will be constructed and used to map neurons from the old
     *                      net to cloned copies for the new one
     */
    protected void cloneNeurons(NeuralNet cloneFrom,
                                Map<SignalProvider, SignalProvider> providersMap,
                                Map<SignalConsumer, SignalConsumer> consumersMap) {
        //cloneFrom.validateNeuronsSet();
        if (providersMap != null) {
            if (consumersMap != null) Util.syncMaps(providersMap, consumersMap);
            else consumersMap = Util.convertProvidersMap(providersMap);

        } else if (consumersMap != null) providersMap = Util.convertConsumersMap(consumersMap);

        providersMap = this.populateSensorCloneMap(cloneFrom, providersMap);
        consumersMap = this.populateDecisionCloneMap(cloneFrom, consumersMap);

        // clone allProviders from the old instance, and put into the providersMap
        for (SignalProvider old : (Set<SignalProvider>)cloneFrom.neurons) {
            if (providersMap.containsKey(old)) continue;

            SignalProvider mine = old.clone();
            providersMap.put(old, mine);

            if (old instanceof SignalConsumer oldConsumer) {
                if (mine instanceof SignalConsumer myConsumer) {
                    consumersMap.put(oldConsumer, myConsumer);

                } else throw new IllegalStateException();
            } else if (mine instanceof SignalConsumer) throw new IllegalStateException();
        }


        // check for Neurons/SignalProvider dependencies missing from cloneFrom.allProviders
        // We track allProviders where replaceInputs() was already called, in case of re-iteration due to StackOverFlow...
        // If replaceInputs() is called on the same neuron twice, it will throw NoSuchElementException
        boolean repeatIteration;

        // 'providersFinished' is this.neurons
        Set<SignalConsumer> consumersFinished = new LinkedHashSet<>();

        Set<SignalProvider> providersInvoked = new LinkedHashSet<>();
        Set<SignalConsumer> consumersInvoked = new LinkedHashSet<>();

        Set<SignalProvider> allProviders = new HashSet<>(providersMap.values()); //make a copy, because the map will be mutated

        do {

            repeatIteration = false;

            for (SignalProvider neuron : allProviders) try {
                if (neuron instanceof SignalConsumer consumer) {
                    this.cloneConsumersInputs(consumer, providersMap, consumersMap, providersInvoked, consumersInvoked, consumersFinished);
                }

                this.cloneProvidersConsumers(neuron, providersMap, consumersMap, providersInvoked, consumersInvoked, consumersFinished);

            } catch (StackOverflowError e) {
                repeatIteration = true;
                System.err.println("\tRecoverable error:"); System.err.println(e);
                // if this happens, just continue iterating on allProviders, and then repeat the entire
                // process again.  As allProviders becomes populated, the two functions will not need to recurse
                // as deeply the next time around
            }


            for (DecisionNode<N, C> node : this.getDecisionNodes()) try {
                this.cloneConsumersInputs(node, providersMap, consumersMap, providersInvoked, consumersInvoked, consumersFinished);

            } catch (StackOverflowError e) {
                repeatIteration = true;
                System.err.println("\tRecoverable error:"); System.err.println(e);
            }

            if (repeatIteration) {
                allProviders.addAll(providersMap.values());

                providersInvoked.retainAll(this.neurons);
                consumersInvoked.retainAll(consumersFinished);
            }

        } while (repeatIteration);
    }

    /**
     * Recursively checks for inputs which haven't been cloned yet.  Also calls neuron.replaceInputs(providersMap)
     * once we've ensured all the inputs have been properly cloned
     *
     * @param neuron
     * @param providersMap
     * @param consumersMap
     * @throws StackOverflowError if it recurses too deeply
     */
    private void cloneConsumersInputs(SignalConsumer neuron,
                                      Map<SignalProvider, SignalProvider> providersMap,
                                      Map<SignalConsumer, SignalConsumer> consumersMap,
                                      Set<SignalProvider> providersInvoked,
                                      Set<SignalConsumer> consumersInvoked,
                                      Set<SignalConsumer> consumersFinished)
            throws StackOverflowError {

        if (consumersInvoked.contains(neuron)) return;

        // We assume that all members of a complex neuron share the same inputs list, and therefore
        // this method only needs to be run once for the entire complex.
        // Note that this method also ensures the other method is invoked on every member (see the end)
        if (neuron instanceof ComplexNeuronMember complex) consumersInvoked.addAll(complex.getMembers());
        else consumersInvoked.add(neuron);

        for (SignalProvider old : neuron.getInputs()) {
            SignalProvider newProvider = providersMap.get(old);
            boolean isNew = newProvider == null;
            if (isNew) newProvider = old.clone();

            if (newProvider instanceof SensorNode provider && provider.getDecisionProvider() != this)
                throw new IllegalStateException();

            if (!(newProvider instanceof SignalConsumer newConsumer)) {
                if (old instanceof SignalConsumer) throw new IllegalStateException();

                if (isNew) providersMap.put(old, newProvider);
                cloneProvidersConsumers(newProvider, providersMap, consumersMap, providersInvoked, consumersInvoked, consumersFinished);
                continue;

            }
            if (!(old instanceof SignalConsumer oldConsumer)) throw new IllegalStateException();

            if (isNew) {
                providersMap.put(old, newProvider);
                consumersMap.put(oldConsumer, newConsumer);
            }

            cloneProvidersConsumers(newProvider, providersMap, consumersMap, providersInvoked, consumersInvoked, consumersFinished);
            cloneConsumersInputs(newConsumer, providersMap, consumersMap, providersInvoked, consumersInvoked, consumersFinished);
        }

        if (neuron instanceof ComplexNeuronMember complex) {
            for (ComplexNeuronMember member : complex.getMembers()) {
                cloneProvidersConsumers(member, providersMap, consumersMap, providersInvoked, consumersInvoked, consumersFinished);
            }
            neuron.replaceInputs(providersMap);
            consumersFinished.addAll(complex.getMembers());

        } else {
            neuron.replaceInputs(providersMap);
            consumersFinished.add(neuron);
        }
    }

    private void cloneProvidersConsumers(SignalProvider neuron,
                                         Map<SignalProvider, SignalProvider> providersMap,
                                         Map<SignalConsumer, SignalConsumer> consumersMap,
                                         Set<SignalProvider> providersInvoked,
                                         Set<SignalConsumer> consumersInvoked,
                                         Set<SignalConsumer> consumersFinished)
            throws StackOverflowError {

        if (providersInvoked.contains(neuron)) return;
        providersInvoked.add(neuron);

        if (neuron instanceof ComplexNeuronMember complex) {
            cloneConsumersInputs(complex, providersMap, consumersMap, providersInvoked, consumersInvoked, consumersFinished);
            // cloneConsumersInputs (other method) ensures that each member has this method invoked on it
        }

        for (SignalConsumer old : neuron.getConsumers()) {
            SignalConsumer newConsumer = consumersMap.get(old);
            boolean isNew = newConsumer == null;
            if (isNew) newConsumer = old.clone();

            if (newConsumer instanceof DecisionNode decision && decision.getDecisionProvider() != this)
                throw new IllegalStateException();

            if (!(newConsumer instanceof SignalProvider newProvider)) {
                if (old instanceof SignalProvider) throw new IllegalStateException();
                if (isNew) consumersMap.put(old, newConsumer);
                cloneConsumersInputs(newConsumer, providersMap, consumersMap, providersInvoked, consumersInvoked, consumersFinished);
                continue;

            }
            if (!(old instanceof SignalProvider oldProvider)) throw new IllegalStateException();

            if (isNew) {
                consumersMap.put(old, newConsumer);
                providersMap.put(oldProvider, newProvider);
            }

            cloneProvidersConsumers(newProvider, providersMap, consumersMap, providersInvoked, consumersInvoked, consumersFinished);
            cloneConsumersInputs(newConsumer, providersMap, consumersMap, providersInvoked, consumersInvoked, consumersFinished);
        }

        neuron.replaceConsumers(consumersMap);
        this.neurons.add(neuron); // this.neurons functions as "providersFinished"
    }

    @Override
    public abstract List<? extends SensorNode<S, N>> getSensors();

    @Override
    public abstract List<? extends DecisionNode<N, C>> getDecisionNodes();

    @Override
    public abstract N clone();

    public abstract N cloneWith(Map<SignalProvider, SignalProvider> providerSubs,
                                Map<SignalConsumer, SignalConsumer> consumerSubs);

    public Set<SignalProvider> getNeurons() {
        return this.neuronsView;
    }

    public long getRound() {
        return this.round;
    }

    public void before() {
        for (SignalProvider neuron : this.neurons) {
            neuron.before();
        }
    }

    public void after() {
        for (SignalProvider neuron : this.neurons) {
            neuron.after();
        }
        this.round++;
    }

    public void reset() {
        this.round = 0;
        for (SignalProvider neuron : this.neurons) {
            neuron.reset();
        }
    }

    /**
     * IMPORTANT: MUST BE RUN AFTER MUTATING A NETWORK, BEFORE THE NETWORK IS USED --
     * Clears the set of neurons and repopulates it with all SignalProviders which are reachable from
     * the sensors and decision nodes, by tracing inputs and consumers recursively.
     *
     * @return this, for chaining
     */
    public N traceNeuronsSet() {
        this.neurons.clear();

        Set<SignalProvider> knownProviders = new HashSet<>(this.getSensors());
        this.getDecisionNodes().forEach(node -> knownProviders.addAll(node.getInputs()));

        Set<SignalProvider> providersInvoked = new HashSet<>();
        Set<SignalConsumer> consumersInvoked = new HashSet<>();

        // use on second and subsequent iterations, if there was any failure the first time around

        //this.neurons functions as providersFinished
        Set<SignalConsumer> consumersFinished = new HashSet<>();

        while (runTracingLoop(knownProviders, providersInvoked, consumersInvoked, consumersFinished)) {
            knownProviders.addAll(providersInvoked);

            providersInvoked.retainAll(this.neurons);
            consumersInvoked.retainAll(consumersFinished);
        }

        return (N)this;
    }

    private boolean runTracingLoop(Set<SignalProvider> knownProviders,
                                   Set<SignalProvider> providersInvoked,
                                   Set<SignalConsumer> consumersInvoked,
                                   Set<SignalConsumer> consumersFinished) {

        boolean failed = false;

        for (SignalProvider provider : knownProviders) {
            if (provider instanceof SensorNode && ((SensorNode) provider).getDecisionProvider() != this) {
                throw new IllegalStateException();
            }

            if (provider instanceof SignalConsumer consumer) try {
                this.traceConsumersInputs(consumer, providersInvoked, consumersInvoked, consumersFinished);

            } catch (StackOverflowError e) {
                failed = true;

                System.err.println("\tRecoverable error:"); // ...see explanation in cloneNeurons()
                System.err.println(e);
            }

            try {
                this.traceProvidersConsumers(provider, providersInvoked, consumersInvoked, consumersFinished);

            } catch (StackOverflowError e) {
                failed = true;

                System.err.println("\tRecoverable error:"); // ...see explanation in cloneNeurons()
                System.err.println(e);
            }
        }

        return failed;
    }

    private void traceConsumersInputs(SignalConsumer neuron,
                                      Set<SignalProvider> providersInvoked,
                                      Set<SignalConsumer> consumersInvoked,
                                      Set<SignalConsumer> consumersFinished)
            throws StackOverflowError {

        if (consumersInvoked.contains(neuron)) return;

        if (neuron instanceof ComplexNeuronMember complex) consumersInvoked.addAll(complex.getMembers());
        else consumersInvoked.add(neuron);

        for (SignalProvider provider : neuron.getInputs()) {
            if (provider instanceof SensorNode sensor && sensor.getDecisionProvider() != this)
                throw new IllegalStateException();

            if (provider instanceof SignalConsumer consumer) {
                this.traceConsumersInputs(consumer, providersInvoked, consumersInvoked, consumersFinished);
            }

            this.traceProvidersConsumers(provider, providersInvoked, consumersInvoked, consumersFinished);
        }

        if (neuron instanceof ComplexNeuronMember complex) {
            for (ComplexNeuronMember member : complex.getMembers()) {
                traceProvidersConsumers(member, providersInvoked, consumersInvoked, consumersFinished);
            }
            consumersFinished.addAll(complex.getMembers());

        } else {
            consumersFinished.add(neuron);
        }
    }

    private void traceProvidersConsumers(SignalProvider neuron,
                                         Set<SignalProvider> providersInvoked,
                                         Set<SignalConsumer> consumersInvoked,
                                         Set<SignalConsumer> consumersFinished)
            throws StackOverflowError {

        if (providersInvoked.contains(neuron)) return;
        providersInvoked.add(neuron);

        if (neuron instanceof ComplexNeuronMember complex) {
            this.traceProvidersConsumers(complex, providersInvoked, consumersInvoked, consumersFinished);
        }

        for (SignalConsumer consumer : neuron.getConsumers()) {
            if (consumer instanceof DecisionNode decision && decision.getDecisionProvider() != this)
                throw new IllegalStateException();

            this.traceConsumersInputs(consumer, providersInvoked, consumersInvoked, consumersFinished);

            if (consumer instanceof SignalProvider provider) {
                this.traceProvidersConsumers(provider, providersInvoked, consumersInvoked, consumersFinished);
            }
        }

        this.neurons.add(neuron);
    }


    /**
     * Default implementation assumes the Lists of sensor nodes are identical size, and maps
     * the two lists directly to each other by index.  If the list from the cloned NeuralNet is smaller
     * than the list from this, the method will throw a NullPointerException UNLESS the providersMap was
     * pre-populated with substitutions for the additional clonedFrom sensors.
     *
     * Custom implementations can override this.
     *
     * @param clonedFrom NeuralNet which this is cloned from
     * @param usingMap The pre-existing map to populate with the sensor mappings, or null for a new HashMap
     * @return Same map passed in, modified with the old sensors (from clonedFrom) added as keys,
     *          and the new sensors (from this) as their values
     *
     * @throws NoSuchElementException if the SensorNode list from the cloned NeuralNet is smaller than
     * the list from this
     */
    private Map<SignalProvider, SignalProvider> populateSensorCloneMap(NeuralNet clonedFrom,
                                                                    Map<SignalProvider, SignalProvider> usingMap) {

        if (usingMap == null) usingMap = new HashMap<>(((NeuralNet<S, N, C>)clonedFrom).neurons.size());

        for (Iterator<? extends SensorNode<S, N>>
                origIt = clonedFrom.getSensors().listIterator(),
                myIt = this.getSensors().listIterator();
                origIt.hasNext();) {

            SensorNode orig = origIt.next(),
                       mine = myIt.hasNext() ? myIt.next() : null;

            usingMap.computeIfAbsent(orig, o -> mine).addConsumers(orig.getConsumers());
        }

        return usingMap;
    }


    /**
     * Default implementation assumes the Lists of decision nodes are identical size, and maps
     * the two lists directly to each other by index.  If the list from the cloned NeuralNet is smaller
     * than the list from this, the method will throw a NullPointerException UNLESS the consumersMap was
     * pre-populated with substitutions for the additional clonedFrom decisionNodes.
     *
     * Custom implementations can override this.
     *
     * @param clonedFrom NeuralNet which this is cloned from
     * @return Map with the old decision node (from clonedFrom) as key, and the new decision node (from this) as value
     * @throws NoSuchElementException if the DecisionNode list from the cloned NeuralNet is smaller than
     * the list from this
     */
    private Map<SignalConsumer, SignalConsumer> populateDecisionCloneMap(NeuralNet clonedFrom,
                                                                     Map<SignalConsumer, SignalConsumer> usingMap) {
        if (usingMap == null) usingMap = new HashMap<>();

        for (ListIterator<? extends DecisionNode>
                origIt = clonedFrom.getDecisionNodes().listIterator(),
                myIt = this.getDecisionNodes().listIterator();
                origIt.hasNext();) {

            DecisionNode orig = origIt.next(),
                         mine = myIt.hasNext() ? myIt.next() : null;

            //if this net has fewer sensor

            usingMap.computeIfAbsent(orig, o -> mine).setInputs(orig.getInputs());
        }

        return usingMap;
    }

    public void setSensedObject(S sensedObject) {
        this.sensedObjected = sensedObject;
    }

    public S getSensedObject() {
        return this.sensedObjected;
    }

    public abstract class Sensor extends CachingProvider implements SensorNode<S, N> {
        protected Sensor() { }

        @Override
        public S getSensedObject() {
            return NeuralNet.this.getSensedObject();
        }

        @Override
        public N getDecisionProvider() {
            return (N)NeuralNet.this;
        }

        @Override
        public abstract short sense();

        @Override
        protected short calcOutput() {
            return this.sense();
        }

        @Override
        public CachingProvider clone() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }
    }

    public abstract class Decision implements DecisionNode<N, C> {
        private List<SignalProvider> inputs = new ArrayList<>(1);
        private List<SignalProvider> inputsView = Collections.unmodifiableList(inputs);

        protected Decision() { }

        @Override
        public int getMinInputs() {
            return 1;
        }

        @Override
        public int getMaxInputs() {
            return 1;
        }

        @Override
        public N getDecisionProvider() {
            return (N)NeuralNet.this;
        }

        @Override
        public abstract int getDecisionId();

        @Override
        public List<SignalProvider> getInputs() {
            return this.inputsView;
        }

        @Override
        public int inputsSize() {
            return this.inputs.size();
        }

        @Override
        public boolean containsInput(SignalProvider provider) {
            return this.inputs.contains(provider);
        }

        @Override
        public void setInputs(List<SignalProvider> inputs) {
            switch (inputs.size()) {
                case 0: this.inputs.clear(); return;
                case 1: break;
                default: throw new IllegalArgumentException();
            }

            SignalProvider newProvider = inputs.get(0);
            if (newProvider == null) throw new NullPointerException();

            if (this.inputs.size() == 0) this.inputs.add(newProvider);
            else this.inputs.set(0, newProvider);
        }

        @Override
        public void addInput(SignalProvider newProvider) {
            if (this.inputs.size() != 0) throw new IndexOutOfBoundsException();
            if (newProvider == null) throw new NullPointerException();
            this.inputs.add(newProvider);
        }

        @Override
        public void addInput(int index, SignalProvider newProvider) {
            this.inputs.add(index, newProvider);
        }

        @Override
        public boolean replaceInput(SignalProvider oldProvider, SignalProvider newProvider) {
            if (this.inputs.size() == 0 || inputs.get(0) != oldProvider) return false;
            if (newProvider == null) throw new NullPointerException();
            inputs.set(0, newProvider);
            return true;
        }

        @Override
        public SignalProvider replaceInput(int index, SignalProvider newProvider) {
            if (index != 0) throw new IndexOutOfBoundsException();
            if (newProvider == null) throw new NullPointerException();

            if (this.inputs.size() == 0) {
                this.inputs.add(newProvider);
                return null;
            }

            return this.inputs.set(0, newProvider);
        }

        @Override
        public void replaceInputs(Map<SignalProvider, SignalProvider> replacements) throws IllegalStateException {
            if (this.inputs.size() == 0) return;
            SignalProvider orig = this.inputs.get(0);
            SignalProvider replacement = replacements.get(orig);

            if (replacement == null) throw new IllegalStateException();

            replacements.put(orig, replacement);
            this.inputs.set(0, replacement);
        }

        @Override
        public SignalProvider removeInput(int index) {
            return this.inputs.remove(index);
        }

        @Override
        public boolean removeInput(SignalProvider removeAll) {
            return this.inputs.remove(removeAll);
        }

        @Override
        public void clearInputs() {
            this.inputs.clear();
        }

        @Override
        public SignalConsumer clone() {
            throw new UnsupportedOperationException();
        }
    }

    protected NoOp getNoOp() {
        NoOp noOp;
        if (this.noOp != null) noOp = this.noOp.get();
        else noOp = null;

        if (noOp == null) {
            noOp = new NoOp();
            this.noOp = new SerializableWeakRef<>(noOp);
        }

        return noOp;
    }

    protected class NoOp extends Decision {
        private NoOp() {
            this.replaceInput(0, FixedValueProvider.makeMin());
        }

        @Override
        public int getDecisionId() {
            return -1;
        }

        @Override
        public SignalConsumer clone() {
            throw new UnsupportedOperationException();
        }
    }
}