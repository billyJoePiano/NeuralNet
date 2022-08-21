package neuralNet.function;

import neuralNet.neuron.*;

import java.util.*;

/**
 * AKA AdditionNormalized
 */
public class LessThan implements StatelessFunction {
    public static final LessThan instance = new LessThan();

    public static CachingNeuronUsingStatelessFunction makeNeuron() {
        return new CachingNeuronUsingStatelessFunction(instance);
    }

    public static CachingNeuronUsingStatelessFunction makeNeuron(List<SignalProvider> inputs) {
        return new CachingNeuronUsingStatelessFunction(inputs, instance);
    }

    public static CachingNeuronUsingStatelessFunction makeNeuron(SignalProvider value, SignalProvider comparedTo) {
        return new CachingNeuronUsingStatelessFunction(Arrays.asList(value, comparedTo), instance);
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
