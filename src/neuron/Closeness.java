package neuron;

import java.util.*;

public class Closeness implements StatelessFunction {
    public static Closeness instance = new Closeness();

    public static StatelessCachingNeuron makeNeuron() {
        return new StatelessCachingNeuron(instance);
    }

    public static StatelessCachingNeuron makeNeuron(List<SignalProvider> inputs) {
        return new StatelessCachingNeuron(inputs, instance);
    }

    public static StatelessCachingNeuron makeNeuron(SignalProvider ... inputs) {
        return new StatelessCachingNeuron(Arrays.asList(inputs), instance);
    }

    private Closeness() { }

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
        int max = inputs.get(0).getOutput();
        int min = max;

        for (SignalProvider neuron : inputs) {
            short output = neuron.getOutput();

            if (output > max) {
                max = output;

            } else if (output < min) {
                min = output;
            }
        }

        return (short)(Short.MAX_VALUE - max + min);
    }
}
