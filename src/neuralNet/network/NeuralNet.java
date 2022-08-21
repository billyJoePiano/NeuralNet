package neuralNet.network;

import neuralNet.neuron.*;

import java.util.*;

public abstract class NeuralNet<O extends Sensable<O>,
                            C extends DecisionConsumer<C>,
                            N extends NeuralNet<O, C, N>> {

    private final Set<Neuron> neurons = Collections.newSetFromMap(new WeakHashMap<>());
    private final Set<Neuron> neuronsView = Collections.unmodifiableSet(neurons);
    private O sensedObjected;

    public abstract List<SensorNode<O, ?, N>> getSensors();
    public abstract List<DecisionNode<C, ?, N>> getDecisionNodes();



    protected NeuralNet(Set<Neuron> neurons) throws IllegalStateException {
        for (Neuron neuron : neurons) {
            validateNeuron(neuron);
        }
    }

    protected NeuralNet(N cloneFrom) {
        Map<SignalProvider, SignalProvider> providersMap = this.getSensorCloneMap(cloneFrom);
        Map<SignalConsumer, SignalConsumer> consumersMap = this.getDecisionCloneMap(cloneFrom);

        for (Neuron neuron : (Set<Neuron>)((NeuralNet)cloneFrom).neurons) {
            Neuron mine = neuron.clone();
            providersMap.put(neuron, mine);
            consumersMap.put(neuron, mine);

            this.neurons.add(mine);
        }

        for (Neuron neuron : this.neurons) {
            neuron.replaceInputs(providersMap);
        }

        for (Map.Entry<SignalProvider, SignalProvider> entry : providersMap.entrySet()) {
            SignalProvider newProvider = entry.getValue();
            if (this.neurons.contains(newProvider)) continue;

            SignalProvider orig = entry.getKey();

            if (newProvider instanceof Neuron) {
                if (!(orig instanceof Neuron)) throw new IllegalStateException();
                // TODO PROBLEM : calling neuron.replaceProviders(providersMap)
                // will throw ConcurrentModificationException on next iteration!!!


            } else if (orig instanceof SignalConsumer) throw new IllegalStateException();
        }

        for (Neuron neuron : this.neurons) {

        }
    }

    //private void cloneNeuron(Neuron neuron, Map)

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