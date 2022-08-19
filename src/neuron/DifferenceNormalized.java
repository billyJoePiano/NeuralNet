package neuron;

import java.util.*;

import static neuron.StatelessFunction.*;

public class DifferenceNormalized implements StatelessFunction {
    public static final DifferenceNormalized instance = new DifferenceNormalized();

    public static StatelessCachingNeuron makeNeuron() {
        return new StatelessCachingNeuron(instance);
    }

    public static StatelessCachingNeuron makeNeuron(List<SignalProvider> inputs) {
        return new StatelessCachingNeuron(inputs, instance);
    }

    public static StatelessCachingNeuron makeNeuron(SignalProvider value, SignalProvider comparedTo) {
        return new StatelessCachingNeuron(Arrays.asList(value, comparedTo), instance);
    }

    private DifferenceNormalized() { }

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
        return clip(Math.floor(((double)((int)inputs.get(0).getOutput() - (int)inputs.get(1).getOutput()) / 2.0)));
    }
}
