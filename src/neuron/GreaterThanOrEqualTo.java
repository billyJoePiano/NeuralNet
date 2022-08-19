package neuron;

import java.util.*;

/**
 * AKA AdditionNormalized
 */
public class GreaterThanOrEqualTo implements StatelessFunction {
    public static final GreaterThanOrEqualTo instance = new GreaterThanOrEqualTo();

    public static StatelessCachingNeuron makeNeuron() {
        return new StatelessCachingNeuron(instance);
    }

    public static StatelessCachingNeuron makeNeuron(List<SignalProvider> inputs) {
        return new StatelessCachingNeuron(inputs, instance);
    }

    public static StatelessCachingNeuron makeNeuron(SignalProvider value, SignalProvider comparedTo) {
        return new StatelessCachingNeuron(Arrays.asList(value, comparedTo), instance);
    }

    private GreaterThanOrEqualTo() { }

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
        return inputs.get(0).getOutput() >= inputs.get(1).getOutput() ? Short.MAX_VALUE : Short.MIN_VALUE;
    }
}
