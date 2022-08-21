package neuralNet.function;

import neuralNet.neuron.*;

import java.util.*;

import static neuralNet.function.StatelessFunction.*;

/**
 * AKA AdditionNormalized
 */
public class Average implements StatelessFunction {
    public static final Average instance = new Average();

    public static CachingNeuronUsingStatelessFunction makeNeuron() {
        return new CachingNeuronUsingStatelessFunction(instance);
    }

    public static CachingNeuronUsingStatelessFunction makeNeuron(List<SignalProvider> inputs) {
        return new CachingNeuronUsingStatelessFunction(inputs, instance);
    }

    public static CachingNeuronUsingStatelessFunction makeNeuron(SignalProvider ... inputs) {
        return new CachingNeuronUsingStatelessFunction(Arrays.asList(inputs), instance);
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
