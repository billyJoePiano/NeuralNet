package neuralNet.function;

import neuralNet.neuron.*;

import java.util.*;

import static neuralNet.util.Util.*;

public class Deviation implements FunctionWithInputs {
    public static final Deviation instance = new Deviation();

    public static CachingNeuronUsingFunction makeNeuron() {
        return new CachingNeuronUsingFunction(instance);
    }

    public static CachingNeuronUsingFunction makeNeuron(List<SignalProvider> inputs) {
        return new CachingNeuronUsingFunction(instance, inputs);
    }

    public static CachingNeuronUsingFunction makeNeuron(SignalProvider ... inputs) {
        return new CachingNeuronUsingFunction(instance, inputs);
    }

    private Deviation() { }

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

        for (SignalProvider node : inputs) {
            short output = node.getOutput();
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

        return roundClip(Math.sqrt(sumSq / count) * 2 + Short.MIN_VALUE);
    }

}
