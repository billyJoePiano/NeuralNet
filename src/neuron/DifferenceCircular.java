package neuron;

import java.util.*;

public class DifferenceCircular implements StatelessFunction {
    public static final DifferenceCircular instance = new DifferenceCircular();

    public static StatelessCachingNeuron makeNeuron() {
        return new StatelessCachingNeuron(instance);
    }

    public static StatelessCachingNeuron makeNeuron(List<SignalProvider> inputs) {
        return new StatelessCachingNeuron(inputs, instance);
    }

    public static StatelessCachingNeuron makeNeuron(SignalProvider value, SignalProvider comparedTo) {
        return new StatelessCachingNeuron(Arrays.asList(value, comparedTo), instance);
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
