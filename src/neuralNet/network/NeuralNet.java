package neuralNet.network;

import neuralNet.neuron.*;

import java.io.*;
import java.lang.ref.*;
import java.util.*;

public abstract class NeuralNet<S extends Sensable<S>,
                            N extends NeuralNet<S, N, C>,
                            C extends DecisionConsumer<S, C, ?>>
        implements DecisionProvider<S, N, C> {

    private static long generation = 0;
    public static long getCurrentGeneration() { return generation; }
    public static long nextGeneration() { return ++generation; }

    private transient Set<SignalProvider> neurons = Collections.newSetFromMap(new WeakHashMap<>());
    private transient Set<SignalProvider> neuronsView = Collections.unmodifiableSet(this.neurons);
    private transient S sensedObjected;
    private transient long round = 0;

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.writeObject(new ArrayList(this.neurons));
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        this.neurons = Collections.newSetFromMap(new WeakHashMap<>());
        this.neurons.addAll((List<SignalProvider>)stream.readObject());
        this.neuronsView = Collections.unmodifiableSet(this.neurons);
    }

    protected NeuralNet() { }

    protected NeuralNet(Set<Neuron> neurons) throws IllegalStateException {

        this.neurons.addAll(this.getSensors());

        for (Neuron neuron : neurons) {
            validateNeuron(neuron);
        }
    }

    protected NeuralNet(N cloneFrom) {
        this(cloneFrom, null);
    }

    protected NeuralNet(N cloneFrom, Map<SignalProvider, SignalProvider> substitutions) {
        cloneFrom.checkForUnaccountedNeurons();

        if (substitutions != null) this.neurons.addAll(substitutions.values()); //do this before adding sensors to the map

        Map<SignalProvider, SignalProvider> providersMap = this.populateSensorCloneMap(cloneFrom, substitutions);
        Map<SignalConsumer, SignalConsumer> consumersMap = this.getDecisionCloneMap(cloneFrom);

        // clone neurons from the old instance, and put into the providersMap
        for (SignalProvider old : (Set<SignalProvider>)((NeuralNet)cloneFrom).neurons) {
            SignalProvider mine = providersMap.get(old);
            if (mine == null) {
                mine = old.clone();
                providersMap.put(old, mine);
            }

            this.neurons.add(mine);

            if (old instanceof SignalConsumer oldConsumer) {
                if (mine instanceof SignalConsumer myConsumer) {
                    consumersMap.put(oldConsumer, myConsumer);

                } else throw new IllegalStateException();
            } else if (mine instanceof SignalConsumer) throw new IllegalStateException();
        }


        // check for Neurons/SignalProvider dependencies missing from cloneFrom.neurons
        // We track neurons where replaceInputs() was already called, in case of re-iteration due to StackOverFlow...
        // If replaceInputs() is called on the same neuron twice, it will throw NoSuchElementException
        boolean repeatIteration;
        Set<SignalConsumer> inputsReplaced = new HashSet<>();

        do {
            repeatIteration = false;
            for (SignalProvider neuron : new HashSet<>(this.neurons)) {
                if (neuron instanceof SignalConsumer consumer) try {
                    this.checkForUnaccountedCloning(consumer, providersMap, consumersMap, inputsReplaced);

                } catch (StackOverflowError e) {
                    // if this happens, just continue iterating on the current copy of this.neurons, and then
                    // repeat the entire process again.  As this.neurons becomes populated, the
                    // checkForUnaccountedCloning function will not need to recurse as deeply the next time around
                    System.err.println("Recoverable error: " + e);
                    repeatIteration = true;
                }
            }
        } while (repeatIteration);


        for (SignalProvider neuron : this.neurons) {
            neuron.replaceConsumers(consumersMap);
        }
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
    private void checkForUnaccountedCloning(SignalConsumer neuron,
                                               Map<SignalProvider, SignalProvider> providersMap,
                                               Map<SignalConsumer, SignalConsumer> consumersMap,
                                               Set<SignalConsumer> inputsReplaced)
            throws StackOverflowError {

        if (inputsReplaced.contains(neuron)) return;

        for (SignalProvider old : neuron.getInputs()) {
            if (this.neurons.contains(old)) continue;
            SignalProvider newProvider = old.clone();

            if (newProvider instanceof SensorNode provider && provider.getDecisionProvider() != this)
                throw new IllegalStateException();

            providersMap.put(old, newProvider);
            this.neurons.add(newProvider);

            if (!(newProvider instanceof SignalConsumer newConsumer)) {
                if (old instanceof SignalConsumer) throw new IllegalStateException();
                else continue;
            }
            if (!(old instanceof SignalConsumer oldConsumer)) throw new IllegalStateException();

            consumersMap.put(oldConsumer, newConsumer);
            checkForUnaccountedCloning(newConsumer, providersMap, consumersMap, inputsReplaced);
        }

        neuron.replaceInputs(providersMap);
        inputsReplaced.add(neuron);
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
    public Map<SignalProvider, SignalProvider> getSensorCloneMap(N clonedFrom) {
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
    public Map<SignalProvider, SignalProvider> populateSensorCloneMap(N clonedFrom,
                                                                    Map<SignalProvider, SignalProvider> usingMap) {

        if (usingMap == null) usingMap = new HashMap<>(((NeuralNet)clonedFrom).neurons.size());

        for (Iterator<? extends SensorNode<S, N>> orig = clonedFrom.getSensors().listIterator(),
             mine = this.getSensors().listIterator();
             orig.hasNext();) {

             usingMap.put(orig.next(), mine.next());
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
    public Map<SignalConsumer, SignalConsumer> getDecisionCloneMap(N clonedFrom) {
        Map<SignalConsumer, SignalConsumer> map = new HashMap<>();

        for (ListIterator<? extends DecisionNode<N, C>> orig = clonedFrom.getDecisionNodes().listIterator(),
             mine = this.getDecisionNodes().listIterator();
             orig.hasNext();) {

            map.put(orig.next(), mine.next());
        }

        return map;
    }

    public void setSensedObject(S sensedObject) {
        this.sensedObjected = sensedObject;
    }

    public S getSensedObject() {
        return this.sensedObjected;
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
    }

    private WeakReference<NoOp> noOp;
    protected NoOp getNoOp() {
        NoOp noOp;
        if (this.noOp != null) noOp = this.noOp.get();
        else noOp = null;

        if (noOp == null) {
            noOp = new NoOp();
            this.noOp = new WeakReference<>(noOp);
        }

        return noOp;
    }

    protected class NoOp extends Decision {
        private NoOp() {
            this.replaceInput(0, FixedValueProvider.MIN);
        }

        @Override
        public int getDecisionId() {
            return -1;
        }
    }
}