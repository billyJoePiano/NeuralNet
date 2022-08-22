package neuralNet.function;

import neuralNet.neuron.*;

import java.util.*;

public class DifferenceCircular implements FunctionWithInputs {
    public static final DifferenceCircular instance = new DifferenceCircular();

    public static CachingNeuronUsingFunction makeNeuron() {
        return new CachingNeuronUsingFunction(instance);
    }

    public static CachingNeuronUsingFunction makeNeuron(List<SignalProvider> inputs) {
        return new CachingNeuronUsingFunction(inputs, instance);
    }

    public static CachingNeuronUsingFunction makeNeuron(SignalProvider value, SignalProvider comparedTo) {
        return new CachingNeuronUsingFunction(Arrays.asList(value, comparedTo), instance);
    }

    private DifferenceCircular() { }

    @Override
    public int getMinInputs() {
        return 2;
    }

    @Override
    public int getMaxInputs() {
        return 2;
    }

    @Override
    public short calcOutput(List<SignalProvider> inputs) {
        return (short)((int)inputs.get(0).getOutput() - (int)inputs.get(1).getOutput());
    }
}
