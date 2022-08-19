package neuron;

import java.util.*;

import static neuron.StatelessFunction.*;

public class Uniformity implements StatelessFunction {
    public static final Uniformity instance = new Uniformity();

    public static StatelessCachingNeuron makeNeuron() {
        return new StatelessCachingNeuron(instance);
    }

    public static StatelessCachingNeuron makeNeuron(List<SignalProvider> inputs) {
        return new StatelessCachingNeuron(inputs, instance);
    }

    public static StatelessCachingNeuron makeNeuron(SignalProvider ... inputs) {
        return new StatelessCachingNeuron(Arrays.asList(inputs), instance);
    }

    private Uniformity() { }

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

        short[] vals = new short[inputs.size()];

        for (SignalProvider neuron : inputs) {
            short output = neuron.getOutput();
            sum += output;
            vals[count++] = output;
        }

        if (count != vals.length) throw new IllegalStateException();

        double mean = (double)sum / count;

        double sumSq = 0;

        for (short val : vals) {
            double diff = mean - val;
            sumSq += diff * diff;
        }

        return roundClip(Short.MAX_VALUE - Math.sqrt(sumSq / count) * 2);
    }
}