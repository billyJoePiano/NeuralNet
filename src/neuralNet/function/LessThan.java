package neuralNet.function;

import neuralNet.neuron.*;

import java.util.*;

/**
 * AKA AdditionNormalized
 */
public class LessThan implements FunctionWithInputs {
    public static final LessThan instance = new LessThan();

    public static CachingNeuronUsingFunction makeNeuron() {
        return new CachingNeuronUsingFunction(instance);
    }

    public static CachingNeuronUsingFunction makeNeuron(List<SignalProvider> inputs) {
        return new CachingNeuronUsingFunction(inputs, instance);
    }

    public static CachingNeuronUsingFunction makeNeuron(SignalProvider value, SignalProvider comparedTo) {
        return new CachingNeuronUsingFunction(Arrays.asList(value, comparedTo), instance);
    }

    private LessThan() { }

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
        return inputs.get(0).getOutput() < inputs.get(1).getOutput() ? Short.MAX_VALUE : Short.MIN_VALUE;
    }
}
