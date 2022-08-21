package neuralNet.function;

import neuralNet.neuron.*;

import java.util.*;

public class Equals implements StatelessFunction {
    public static final Equals instance = new Equals();

    public static CachingNeuronUsingStatelessFunction makeNeuron() {
        return new CachingNeuronUsingStatelessFunction(instance);
    }

    public static CachingNeuronUsingStatelessFunction makeNeuron(List<SignalProvider> inputs) {
        return new CachingNeuronUsingStatelessFunction(inputs, instance);
    }

    public static CachingNeuronUsingStatelessFunction makeNeuron(SignalProvider ... inputs) {
        return new CachingNeuronUsingStatelessFunction(Arrays.asList(inputs), instance);
    }

    private Equals() { }

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
        int size = inputs.size();
        if (size == 2) {
            return inputs.get(0).getOutput() == inputs.get(1).getOutput() ? Short.MAX_VALUE : Short.MIN_VALUE;
        }

        Map<Short, Integer> occurences = new HashMap<>(inputs.size());

        int maxCount = 1;

        for (SignalProvider neuron : inputs) {
            short val = neuron.getOutput();
            int count = occurences.getOrDefault(val, 0) + 1;
            occurences.put(val, count);

            if (count > maxCount) {
                maxCount = count;
            }
        }

        return (short)(((double)(maxCount - 1) / (double)(size - 1)) * RANGE_INT + Short.MIN_VALUE);
    }
}
