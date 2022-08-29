package neuralNet.network;

import neuralNet.neuron.*;
import neuralNet.util.*;

import java.util.*;

public abstract class NeuralNet<S extends Sensable<S>,
                            N extends NeuralNet<S, N, C>,
                            C extends DecisionConsumer<S, C, ?>>
        implements DecisionProvider<S, N, C> {

    //TODO figure out how to serialize/deserialize the current generation value
    private static long generation = 0;
    public static long getCurrentGeneration() { return generation; }
    public static long nextGeneration() { return ++generation; }

    private Set<SignalProvider> neurons = new SerializableWeakHashSet<>();
    private SerializableWeakRef<NoOp> noOp;

    private transient Set<SignalProvider> neuronsView = ((SerializableWeakHashSet<SignalProvider>)this.neurons).getView();
    private transient S sensedObjected;
    private transient long round = 0;

    protected NeuralNet() { }

    /**
     * Makes clones of all the neurons from the cloned NeuralNet, mapping the old neurons to the new ones,
     * and then replaces the inputs and consumers of each copied neuron using the map of old to new.
     *
     * Note that this operation can't be done immediately in a NeuralNet clone constructor, because the
     * subclass' constructor may need to perform some initialization tasks before the full cloning operation
     * is ready to commence (e.g. populate sensor and decision nodes). This method should be called from the
     * subclass's constructor after it has finished those initializations.
     *
     * @param cloneFrom the neural net we are cloning
     * @param substitutions any special neuron substitutions to use.  NOTE THAT ANY PASSED MAP WILL BE MUTATED.
     *                      If null, an empty HashMap will be constructed and used to map neurons from the old
     *                      net to cloned copies for the new one
     *
     */
    protected void cloneNeurons(N cloneFrom, Map<SignalProvider, SignalProvider> substitutions) {
        cloneFrom.checkForUnaccountedNeurons();

        Map<SignalProvider, SignalProvider> providersMap = this.populateSensorCloneMap(cloneFrom, substitutions);
        Map<SignalConsumer, SignalConsumer> consumersMap = this.getDecisionCloneMap(cloneFrom);

        // clone allProviders from the old instance, and put into the providersMap
        for (SignalProvider old : ((NeuralNet<S, N, C>)cloneFrom).neurons) {
            SignalProvider mine = providersMap.get(old);
            if (mine == null) {
                mine = old.clone();
                providersMap.put(old, mine);
            }

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
        // providersFinished is this.allProviders
        Set<SignalConsumer> consumersFinished = new LinkedHashSet<>();

        Set<SignalProvider> providersInvoked = new LinkedHashSet<>();
        Set<SignalConsumer> consumersInvoked = new LinkedHashSet<>();

        Set<SignalProvider> allProviders = new HashSet<>(providersMap.values()); //make a copy, because the map will be mutated

        do {

            repeatIteration = false;

            for (SignalProvider neuron : allProviders) try {
                this.checkProvidersConsumers(neuron, providersMap, consumersMap, providersInvoked, consumersInvoked, consumersFinished);

                if (neuron instanceof SignalConsumer consumer) {
                    this.checkConsumersInputs(consumer, providersMap, consumersMap, providersInvoked, consumersInvoked, consumersFinished);
                }

            } catch (StackOverflowError e) {
                System.err.println("Recoverable error: " + e);
                repeatIteration = true;
                // if this happens, just continue iterating on allProviders, and then repeat the entire
                // process again.  As allProviders becomes populated, the two functions will not need to recurse
                // as deeply the next time around
            }


            for (DecisionNode<N, C> node : this.getDecisionNodes()) try {
                this.checkConsumersInputs(node, providersMap, consumersMap, providersInvoked, consumersInvoked, consumersFinished);

            } catch (StackOverflowError e) {
                System.err.println("Recoverable error: " + e);
                repeatIteration = true;
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
    private void checkConsumersInputs(SignalConsumer neuron,
                                      Map<SignalProvider, SignalProvider> providersMap,
                                      Map<SignalConsumer, SignalConsumer> consumersMap,
                                      Set<SignalProvider> providersInvoked,
                                      Set<SignalConsumer> consumersInvoked,
                                      Set<SignalConsumer> consumersFinished)
            throws StackOverflowError {

        if (consumersInvoked.contains(neuron)) return;
        consumersInvoked.add(neuron);

        for (SignalProvider old : neuron.getInputs()) {
            SignalProvider newProvider = providersMap.get(old);
            boolean isNew = newProvider == null;
            if (isNew) newProvider = old.clone();

            if (newProvider instanceof SensorNode provider && provider.getDecisionProvider() != this)
                throw new IllegalStateException();

            if (!(newProvider instanceof SignalConsumer newConsumer)) {
                if (old instanceof SignalConsumer) throw new IllegalStateException();

                if (isNew) providersMap.put(old, newProvider);
                checkProvidersConsumers(newProvider, providersMap, consumersMap, providersInvoked, consumersInvoked, consumersFinished);
                continue;

            }
            if (!(old instanceof SignalConsumer oldConsumer)) throw new IllegalStateException();

            if (isNew) {
                providersMap.put(old, newProvider);
                consumersMap.put(oldConsumer, newConsumer);
            }

            checkProvidersConsumers(newProvider, providersMap, consumersMap, providersInvoked, consumersInvoked, consumersFinished);
            checkConsumersInputs(newConsumer, providersMap, consumersMap, providersInvoked, consumersInvoked, consumersFinished);
        }

        neuron.replaceInputs(providersMap);
        consumersFinished.add(neuron);
    }

    private void checkProvidersConsumers(SignalProvider neuron,
                                         Map<SignalProvider, SignalProvider> providersMap,
                                         Map<SignalConsumer, SignalConsumer> consumersMap,
                                         Set<SignalProvider> providersInvoked,
                                         Set<SignalConsumer> consumersInvoked,
                                         Set<SignalConsumer> consumersFinished)
            throws StackOverflowError {

        if (providersInvoked.contains(neuron)) return;
        providersInvoked.add(neuron);

        for (SignalConsumer old : neuron.getConsumers()) {
            SignalConsumer newConsumer = consumersMap.get(old);
            boolean isNew = newConsumer == null;
            if (isNew) newConsumer = old.clone();

            if (newConsumer instanceof DecisionNode decision && decision.getDecisionProvider() != this)
                throw new IllegalStateException();

            if (!(newConsumer instanceof SignalProvider newProvider)) {
                if (old instanceof SignalProvider) throw new IllegalStateException();
                if (isNew) consumersMap.put(old, newConsumer);
                checkConsumersInputs(newConsumer, providersMap, consumersMap, providersInvoked, consumersInvoked, consumersFinished);
                continue;

            }
            if (!(old instanceof SignalProvider oldProvider)) throw new IllegalStateException();

            if (isNew) {
                consumersMap.put(old, newConsumer);
                providersMap.put(oldProvider, newProvider);
            }

            checkProvidersConsumers(newProvider, providersMap, consumersMap, providersInvoked, consumersInvoked, consumersFinished);
            checkConsumersInputs(newConsumer, providersMap, consumersMap, providersInvoked, consumersInvoked, consumersFinished);
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

    public abstract N cloneWith(Map<SignalProvider, SignalProvider> substitutions);

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
        //checkForUnaccountedNeurons();
        for (SignalProvider neuron : this.neurons) {
            neuron.reset();
        }
    }

    public N checkForUnaccountedNeurons() {
        this.getDecisionNodes().forEach(node -> this.neurons.addAll(node.getInputs()));
        boolean repeatIteration;
        do {
            repeatIteration = false;
            for (SignalProvider neuron : new HashSet<>(this.neurons)) {
                if (neuron instanceof SensorNode && ((SensorNode) neuron).getDecisionProvider() != this) {
                    throw new IllegalStateException();
                }

                if (neuron instanceof SignalConsumer consumer) try {
                    checkForUnaccountedNeurons(consumer);

                } catch (StackOverflowError e) {
                    repeatIteration = true;
                    System.err.println("Recoverable error: " + e); // ...see explanation in cloning constructor
                }
            }

        } while (repeatIteration);

        return (N)this;
    }

    protected void checkForUnaccountedNeurons(SignalConsumer neuron) throws StackOverflowError {
        for (SignalProvider provider : neuron.getInputs()) {
            if (this.neurons.contains(provider)) continue;
            if (provider instanceof SensorNode && ((SensorNode)provider).getDecisionProvider() != this)
                throw new IllegalStateException();

            this.neurons.add(provider);

            if (provider instanceof SignalConsumer consumer) {
                checkForUnaccountedNeurons(consumer);
            }
        }
    }

    private void validateNeuron(Neuron neuron) throws IllegalStateException {
        if (this.neurons.contains(neuron)) return;
        this.neurons.add(neuron);

        Set<SignalConsumer> consumers = neuron.getConsumers();
        if (consumers != null) {
            for (SignalConsumer consumer : neuron.getConsumers()) {
                if (consumer instanceof NeuralNet.Decision) {
                    DecisionProvider net = ((Decision) consumer).getDecisionProvider();
                    if (net != this && net != null) {
                        this.neurons.remove(neuron);
                        throw new IllegalStateException();
                    }

                } else if (consumer instanceof Neuron) {
                    validateNeuron((Neuron) consumer);
                }
            }
        }

        List<SignalProvider> inputs = neuron.getInputs();
        if (inputs != null) {
            for (SignalProvider provider : inputs) {
                if (provider instanceof SensorNode) {
                    DecisionProvider net = ((SensorNode) provider).getDecisionProvider();
                    if (net != this && net != null) {
                        this.neurons.remove(neuron);
                        throw new IllegalStateException();
                    }

                } else if (provider instanceof Neuron) {
                    validateNeuron((Neuron) provider);
                }
            }
        }
    }

    /**
     * Convenience method for populateSensorCloneMap() which uses the default empty HashMap
     *
     * @param clonedFrom NeuralNet which this is cloned from
     * @return a new HashMap populated with the old sensors
     */
    private Map<SignalProvider, SignalProvider> getSensorCloneMap(N clonedFrom) {
        return populateSensorCloneMap(clonedFrom, null);
    }

    /**
     * Default implementation assumes the Lists of sensor nodes are identical size, and maps
     * the two lists directly to each other by index.  Iterator will throw a NoSuchElementException
     * if the list from the cloned NeuralNet is smaller than the list from this
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
    private Map<SignalProvider, SignalProvider> populateSensorCloneMap(N clonedFrom,
                                                                    Map<SignalProvider, SignalProvider> usingMap) {

        if (usingMap == null) usingMap = new HashMap<>(((NeuralNet<S, N, C>)clonedFrom).neurons.size());

        for (Iterator<? extends SensorNode<S, N>>
                origIt = clonedFrom.getSensors().listIterator(),
                myIt = this.getSensors().listIterator();
                origIt.hasNext();) {

            SensorNode<S, N> orig = origIt.next(),
                             mine = myIt.next();

            mine.addConsumers(orig.getConsumers());
            usingMap.put(orig, mine);
        }

        return usingMap;
    }


    /**
     * Default implementation assumes the Lists of decision nodes are identical size, and maps
     * the two lists directly to each other by index.  Iterator will throw a NoSuchElementException
     * if the list from the cloned NeuralNet is smaller than the list from this
     *
     * Custom implementations can override this.
     *
     * @param clonedFrom NeuralNet which this is cloned from
     * @return Map with the old decision node (from clonedFrom) as key, and the new decision node (from this) as value
     * @throws NoSuchElementException if the DecisionNode list from the cloned NeuralNet is smaller than
     * the list from this
     */
    private Map<SignalConsumer, SignalConsumer> getDecisionCloneMap(N clonedFrom) {
        Map<SignalConsumer, SignalConsumer> map = new HashMap<>();

        for (ListIterator<? extends DecisionNode<N, C>>
                origIt = clonedFrom.getDecisionNodes().listIterator(),
                myIt = this.getDecisionNodes().listIterator();
                origIt.hasNext();) {

            DecisionNode<N, C> orig = origIt.next(),
                               mine = myIt.next();

            mine.setInputs(orig.getInputs());
            map.put(orig, mine);
        }

        return map;
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