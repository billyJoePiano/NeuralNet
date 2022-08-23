package neuralNet.test;

import neuralNet.neuron.*;

import java.util.*;

public class SwitchIterable implements Iterable<Short>, Iterator<Short>, SignalProvider {
    private Neuron neuron;
    private short i = Short.MIN_VALUE;
    private Map<Integer, Integer> iterationCounts = new HashMap<>();
    private int iterationGroup = 0;
    private int counter = 0;
    private boolean inUse = false;
    private boolean firstIteration = true;

    public SwitchIterable(Neuron switchNeuron, int numInputs) {
        List<SignalProvider> inputs = new ArrayList(numInputs + 1);
        inputs.add(0, this);

        for (short i = 1; i <= numInputs; i++) {
            inputs.add(new FixedValueProvider(i));
        }

        this.neuron = switchNeuron;
        switchNeuron.setInputs(inputs);
    }

    public SwitchIterable(Neuron switchNeuron, short[] inputVals) {
        List<SignalProvider> inputs = new ArrayList(inputVals.length + 1);
        inputs.add(0, new CachingProviderUsingFunction(this::controlInput));

        for (short val : inputVals) {
            inputs.add(new FixedValueProvider(val));
        }

        this.neuron = switchNeuron;
        switchNeuron.setInputs(inputs);
    }

    public SwitchIterable(Neuron switchNeuron, List<SignalProvider> inputs) {
        inputs.add(0, new CachingProviderUsingFunction(this::controlInput));
        this.neuron = switchNeuron;
        switchNeuron.setInputs(inputs);;
    }

    public short getControlInput() {
        return this.i;
    }

    private short controlInput() {
        return this.i;
    }


    @Override
    public SwitchIterable iterator() throws IllegalStateException {
        if (this.inUse) {
            throw new IllegalStateException();
        }

        inUse = true;
        return this;
    }

    public String toString() {
        boolean first = true;
        String output = "[";
        for (SignalProvider neuron : this.neuron.getInputs()) {
            if (first) {
                first = false;
                output += neuron.getOutput();

            } else {
                output += ", " + neuron.getOutput();
            }
        }

        return output + "]";
    }

    @Override
    public boolean hasNext() {
        if (i < Short.MAX_VALUE) return true;
        iterationCounts.put(iterationGroup, counter);
        return false;
    }

    @Override
    public Short next() {
        if (this.i == Short.MAX_VALUE) {
            throw new NoSuchElementException();

        } else if (this.firstIteration) {
            this.firstIteration = false;

        } else {
            this.i++;
        }

        this.counter++;

        this.neuron.before();
        short output = this.neuron.getOutput();
        this.neuron.after();
        return output;
    }

    public void incrementIterationGroup() {
        if (this.counter > 0) {
            this.iterationCounts.put(this.iterationGroup++, this.counter - 1);
            this.counter = 1;

        } else {
            this.iterationCounts.put(iterationGroup++, 0);
        }
    }

    public int getIterationGroup() {
        return this.iterationGroup;
    }

    public Map<Integer, Integer> getIterationCounts() {
        return this.iterationCounts;
    }

    @Override
    public short getOutput() {
        return this.i;
    }

    @Override
    public Set<SignalConsumer> getConsumers() {
        return null;
    }

    @Override
    public boolean addConsumer(SignalConsumer consumer) {
        return false;
    }

    @Override
    public boolean removeConsumer(SignalConsumer consumer) {
        return false;
    }

    @Override
    public boolean replaceConsumer(SignalConsumer oldConsumer, SignalConsumer newConsumer) {
        return false;
    }

    @Override
    public void replaceConsumers(Map<SignalConsumer, SignalConsumer> neuronMap) {

    }

    @Override
    public void reset() {

    }

    /*
    @Override
    public void clearConsumers() {

    }
     */

    @Override
    public SignalProvider clone() {
        return null;
    }
}
