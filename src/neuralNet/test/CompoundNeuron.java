package neuralNet.test;

import neuralNet.neuron.*;

import java.util.*;

public class CompoundNeuron extends CachingNeuron {
    private final Set<SignalConsumer> consumers = new HashSet<>();
    private final Set<SignalConsumer> consumersView = Collections.unmodifiableSet(this.consumers);

    private final List<Neuron> neurons;

    public CompoundNeuron(CompoundNeuron cloneFrom) {
        super(cloneFrom);
        List<Neuron> neurons = new ArrayList<>(cloneFrom.neurons.size());
        for (Neuron neuron : cloneFrom.neurons) {
            neurons.add(neuron.clone());
        }
        this.neurons = this.checkNeurons(neurons);
    }

    public CompoundNeuron(Neuron n1, Neuron n2) {
        this.neurons = checkNeurons(List.of(n1, n2));
    }

    public CompoundNeuron(Neuron n1, Neuron n2, Neuron n3) {
        this.neurons = checkNeurons(List.of(n1, n2, n3));
    }

    public CompoundNeuron(Neuron n1, Neuron n2, Neuron n3, Neuron n4) {
        this.neurons = checkNeurons(List.of(n1, n2, n3, n4));
    }

    public CompoundNeuron(List<Neuron> neurons) {
        this.neurons = checkNeurons(new ArrayList(neurons));
    }

    private List<Neuron> checkNeurons(List<Neuron> neurons) {
        if (neurons == null) throw new IllegalArgumentException();

        boolean first = true;

        List<SignalProvider> inputs = this.getInputs();

        for (Neuron neuron : neurons) {
            if (first) {
                first = false;
                if (inputs != null && (neuron.getMinInputs() > inputs.size() || neuron.getMaxInputs() < inputs.size())) {
                    throw new IllegalArgumentException();
                }

            } else if (neuron.getMaxInputs() < 1 || neuron.getMinInputs() > 1) {
                throw new IllegalArgumentException();
            }
        }

        if (first) { //means there were no neurons
            throw new IllegalArgumentException();
        }

        Neuron previous = null;
        first = true;
        for (Neuron neuron : neurons) {
            if (first) {
                first = false;
                if (inputs != null) neuron.setInputs(this.getInputs());

            } else {
                neuron.setInputs(List.of(previous));
            }
            previous = neuron;
        }

        return neurons;
    }


    @Override
    public int getMinInputs() {
        return neurons.get(0).getMinInputs();
    }

    @Override
    public int getMaxInputs() {
        return neurons.get(0).getMaxInputs();
    }

    @Override
    public void before() {
        super.before();
        for (Neuron neuron : this.neurons) {
            neuron.before();
        }
    }

    @Override
    public void after() {
        super.after();
        for (Neuron neuron : this.neurons) {
            neuron.after();
        }
    }

    @Override
    public void setInputs(List<SignalProvider> inputs) {
        super.setInputs(inputs);
        this.neurons.get(0).setInputs(inputs);
    }

    @Override
    public SignalProvider replaceInput(int index, SignalProvider newProvider) throws NullPointerException {
        this.neurons.get(0).replaceInput(index, newProvider);
        return super.replaceInput(index, newProvider);
    }

    @Override
    public boolean replaceInput(SignalProvider oldProvider, SignalProvider newProvider) throws IllegalArgumentException, NullPointerException {
        this.neurons.get(0).replaceInput(oldProvider, newProvider);
        return super.replaceInput(oldProvider, newProvider);
    }

    @Override
    protected short calcOutput(List<SignalProvider> inputs) {
        return this.neurons.get(this.neurons.size() - 1).getOutput();
    }

    @Override
    protected long calcNeuralHashFor(LoopingNeuron looper) {
        return this.neurons.get(this.neurons.size() - 1).getNeuralHashFor(looper);
    }

    @Override
    public CompoundNeuron clone() {
        return new CompoundNeuron(this);
    }
}
