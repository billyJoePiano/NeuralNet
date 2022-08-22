package neuralNet.function;

import neuralNet.neuron.*;

import java.util.*;

import static neuralNet.function.FunctionWithInputs.*;

/**
 * AKA AdditionNormalized
 */
public class Average implements FunctionWithInputs {
    public static final Average instance = new Average();

    public static CachingNeuronUsingFunction makeNeuron() {
        return new CachingNeuronUsingFunction(instance);
    }

    public static CachingNeuronUsingFunction makeNeuron(List<SignalProvider> inputs) {
        return new CachingNeuronUsingFunction(inputs, instance);
    }

    public static CachingNeuronUsingFunction makeNeuron(SignalProvider ... inputs) {
        return new CachingNeuronUsingFunction(Arrays.asList(inputs), instance);
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
