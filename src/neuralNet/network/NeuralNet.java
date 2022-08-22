package neuralNet.network;

import neuralNet.neuron.*;

import java.util.*;

public abstract class NeuralNet<O extends Sensable<O>,
                            C extends DecisionConsumer<C>,
                            N extends NeuralNet<O, C, N>> {

    private final Set<SignalProvider> neurons = Collections.newSetFromMap(new WeakHashMap<>());
    private final Set<SignalProvider> neuronsView = Collections.unmodifiableSet(neurons);
    private O sensedObjected;
    private long round = 0;

    public abstract List<SensorNode<O, ?, N>> getSensors();
    public abstract List<DecisionNode<C, ?, N>> getDecisionNodes();

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
                else if (newProvider instanceof SensorNode && ((SensorNode)newProvider).getNeuralNet() != this)
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
            if (neuron instanceof SensorNode && ((SensorNode)neuron).getNeuralNet() != this) {
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
            if (provider instanceof SensorNode && ((SensorNode)provider).getNeuralNet() != this)
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
            if (consumer instanceof DecisionNode) {
                NeuralNet net = ((DecisionNode)consumer).getNeuralNet();
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
                NeuralNet net = ((SensorNode)provider).getNeuralNet();
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

        for (Iterator<SensorNode<O, ?, N>> orig = clonedFrom.getSensors().listIterator(),
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

        for (Iterator<DecisionNode<C, ?, N>> orig = clonedFrom.getDecisionNodes().listIterator(),
                                             mine = this.getDecisionNodes().listIterator();
                orig.hasNext();) {

            map.put(orig.next(), mine.next());
        }

        return map;
    }

    public abstract N deepClone();

    public void setSensedObject(O sensedObject) {
        this.sensedObjected = sensedObject;
    }

    public O getSensedObject() {
        return this.sensedObjected;
    }

}