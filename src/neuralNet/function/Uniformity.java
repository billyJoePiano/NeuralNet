package neuralNet.function;

import neuralNet.neuron.*;

import java.util.*;

import static neuralNet.function.FunctionWithInputs.*;

public class Uniformity implements FunctionWithInputs {
    public static final Uniformity instance = new Uniformity();

    public static CachingNeuronUsingFunction makeNeuron() {
        return new CachingNeuronUsingFunction(instance);
    }

    public static CachingNeuronUsingFunction makeNeuron(List<SignalProvider> inputs) {
        return new CachingNeuronUsingFunction(inputs, instance);
    }

    public static CachingNeuronUsingFunction makeNeuron(SignalProvider ... inputs) {
        return new CachingNeuronUsingFunction(Arrays.asList(inputs), instance);
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
