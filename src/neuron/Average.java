package neuron;

import java.util.*;

import static neuron.StatelessFunction.*;

/**
 * AKA AdditionNormalized
 */
public class Average implements StatelessFunction {
    public static final Average instance = new Average();

    public static StatelessCachingNeuron makeNeuron() {
        return new StatelessCachingNeuron(instance);
    }

    public static StatelessCachingNeuron makeNeuron(List<SignalProvider> inputs) {
        return new StatelessCachingNeuron(inputs, instance);
    }

    public static StatelessCachingNeuron makeNeuron(SignalProvider ... inputs) {
        return new StatelessCachingNeuron(Arrays.asList(inputs), instance);
    }

    private Average() { }

    @Override
    public int getMinInputs() {
        return 2;
    }

    @Override
    public int getMaxInputs() {
        return 256;
    }

    @Override
    public short calcOutput(List<SignalProvider> inputs) {
        int sum = 0;
        int count = 0;

        for (SignalProvider neuron : inputs) {
            sum += neuron.getOutput();
            count++;
        }

        return roundClip((double)sum / count);
    }
}
