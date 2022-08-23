package neuralNet.network;

import neuralNet.neuron.*;

import java.util.*;

public abstract class NeuralNet<S extends Sensable<S>,
                            N extends NeuralNet<S, N, C>,
                            C extends DecisionConsumer<S, C, ?>>
        implements DecisionProvider<S, N, C> {

    private final Set<SignalProvider> neurons = Collections.newSetFromMap(new WeakHashMap<>());
    private final Set<SignalProvider> neuronsView = Collections.unmodifiableSet(neurons);
    private S sensedObjected;
    private long round = 0;

    protected NeuralNet() { }

    protected NeuralNet(Set<Neuron> neurons) throws IllegalStateException {

        this.neurons.addAll(this.getSensors());

        for (Neuron neuron : neurons) {
            validateNeuron(neuron);
        }
    }

    protected NeuralNet(N cloneFrom) {
        Map<SignalProvider, SignalProvider> providersMap = this.getSensorCloneMap(cloneFrom);
        Map<SignalConsumer, SignalConsumer> consumersMap = this.getDecisionCloneMap(cloneFrom);

        // clone neurons from the old instance, and put into the providersMap
        for (SignalProvider old : (Set<SignalProvider>)((NeuralNet)cloneFrom).neurons) {
            if (providersMap.containsKey(old)) continue; //SensorNodes

            SignalProvider mine = old.clone();
            providersMap.put(old, mine);

            if (old instanceof SignalConsumer) {
                if (!(mine instanceof SignalConsumer)) throw new IllegalStateException();
                consumersMap.put((SignalConsumer)old, (SignalConsumer)mine);
            }

            this.neurons.add(mine);
        }

        // call replaceInputs using map
        for (SignalProvider neuron : this.neurons) {
            if (neuron instanceof SignalConsumer) {
                ((SignalConsumer)neuron).replaceInputs(providersMap);
            }
        }

        int lastSize = 0;
        Set<Map.Entry<SignalProvider, SignalProvider>> entrySetCopy;

        // check for Neurons/SignalProviders missing from the cloneFrom net
        while (lastSize != (lastSize =
                                (entrySetCopy = new HashSet<>(providersMap.entrySet()))
                                        .size())) {
            // need to make copy because providersMap will be mutated, don't want ConcurrentModificationException

            for (Map.Entry<SignalProvider, SignalProvider> entry : entrySetCopy) {
                SignalProvider newProvider = entry.getValue();
                if (this.neurons.contains(newProvider)) continue;
                else if (newProvider instanceof SensorNode && ((SensorNode)newProvider).getDecisionProvider() != this)
                    throw new IllegalStateException();

                this.neurons.add(newProvider);

                SignalProvider orig = entry.getKey();

                if (!(newProvider instanceof SignalConsumer)) {
                    if (orig instanceof SignalConsumer) throw new IllegalStateException();
                    else continue;

                } else if (!(orig instanceof SignalConsumer)) throw new IllegalStateException();

                SignalConsumer newConsumer = (SignalConsumer)newProvider;
                SignalConsumer origConsumer = (SignalConsumer)orig;

                consumersMap.put(origConsumer, newConsumer);
                newConsumer.replaceInputs(providersMap);
            }
        }

        for (SignalProvider neuron : this.neurons) {
            neuron.replaceConsumers(consumersMap);
        }
    }

    @Override
    public abstract List<? extends SensorNode<S, N>> getSensors();

    @Override
    public abstract List<? extends DecisionNode<N, C>> getDecisionNodes();

    @Override
    public abstract N clone();

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
        checkForUnaccountedNeurons();
        for (SignalProvider neuron : this.neurons) {
            neuron.reset();
        }
    }

    public void checkForUnaccountedNeurons() {
        Set<SignalProvider> copy = new HashSet<>(this.neurons);
        for (SignalProvider neuron : copy) {
            if (neuron instanceof SensorNode && ((SensorNode)neuron).getDecisionProvider() != this) {
                throw new IllegalStateException();
            }

            if (neuron instanceof SignalConsumer) {
                checkForUnaccountedNeurons((SignalConsumer)neuron);
            }
        }
    }

    protected void checkForUnaccountedNeurons(SignalConsumer neuron) {
        for (SignalProvider provider : neuron.getInputs()) {
            if (this.neurons.contains(provider)) continue;
            if (provider instanceof SensorNode && ((SensorNode)provider).getDecisionProvider() != this)
                throw new IllegalStateException();

            this.neurons.add(provider);

            if (provider instanceof SignalConsumer) {
                checkForUnaccountedNeurons((SignalConsumer)provider);
            }
        }
    }

    private void validateNeuron(Neuron neuron) throws IllegalStateException {
        if (this.neurons.contains(neuron)) return;
        this.neurons.add(neuron);

        for (SignalConsumer consumer : neuron.getConsumers()) {
            if (consumer instanceof NeuralNet.Decision) {
                DecisionProvider net = ((Decision)consumer).getDecisionProvider();
                if (net != this && net != null) {
                    this.neurons.remove(neuron);
                    throw new IllegalStateException();
                }

            } else if (consumer instanceof Neuron) {
                validateNeuron((Neuron)consumer);
            }
        }

        for (SignalProvider provider: neuron.getInputs()) {
            if (provider instanceof SensorNode) {
                DecisionProvider net = ((SensorNode)provider).getDecisionProvider();
                if (net != this && net != null) {
                    this.neurons.remove(neuron);
                    throw new IllegalStateException();
                }

            } else if (provider instanceof Neuron) {
                validateNeuron((Neuron)provider);
            }
        }
    }


    /**
     * Default implementation assumes the Lists of sensor nodes are identical size, and maps
     * the two lists directly to each other by index.  Iterator will throw a NoSuchElementException
     * if the list from the cloned NeuralNet is smaller than the list from this
     *
     * Custom implementations can override this.
     *
     * @param clonedFrom NeuralNet which this is cloned from
     * @return Map with the old sensor (from clonedFrom) as key, and the new sensor (from this) as value
     * @throws NoSuchElementException if the SensorNode list from the cloned NeuralNet is smaller than
     * the list from this
     */
    public Map<SignalProvider, SignalProvider> getSensorCloneMap(N clonedFrom) {
        Map<SignalProvider, SignalProvider> map = new HashMap<>();

        for (Iterator<? extends SensorNode<S, N>> orig = clonedFrom.getSensors().listIterator(),
             mine = this.getSensors().listIterator();
             orig.hasNext();) {

            map.put(orig.next(), mine.next());
        }

        return map;
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

    protected abstract class Decision implements DecisionNode<N, C> {
        private List<SignalProvider> inputs = new ArrayList<>(1);
        private List<SignalProvider> inputsView = Collections.unmodifiableList(inputs);

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
        public void setInputs(List<SignalProvider> inputs) {
            if (inputs.size() != 1) throw new IllegalArgumentException();
            if (this.inputs.size() == 0) this.inputs.add(inputs.get(0));
            else this.inputs.set(0, inputs.get(0));
        }

        @Override
        public boolean replaceInput(SignalProvider oldProvider, SignalProvider newProvider) {
            if (inputs.get(0) != oldProvider) return false;
            inputs.set(0, newProvider);
            return true;
        }

        @Override
        public SignalProvider replaceInput(int index, SignalProvider newProvider) {
            if (index != 0) throw new IndexOutOfBoundsException();

            if (this.inputs.size() == 0) {
                this.inputs.add(newProvider);
                return null;
            }

            SignalProvider old = this.inputs.get(0);
            this.inputs.set(0, newProvider);
            return old;
        }

        @Override
        public void replaceInputs(Map<SignalProvider, SignalProvider> replacements) throws IllegalStateException {
            if (this.inputs.size() == 0) return;
            SignalProvider orig = this.inputs.get(0);
            SignalProvider replacement = replacements.get(orig);
            if (replacement == null) {
                replacement = orig.clone();
                if (replacement == null) throw new IllegalStateException();
                replacements.put(orig, replacement);
            }
            this.inputs.set(0, replacement);
        }
    }

    private NoOp noOp;
    protected NoOp getNoOp() {
        if (this.noOp == null) this.noOp = new NoOp();
        return this.noOp;
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