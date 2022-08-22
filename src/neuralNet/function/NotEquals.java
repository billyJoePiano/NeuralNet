package neuralNet.function;

import neuralNet.neuron.*;

import java.util.*;

public class NotEquals implements FunctionWithInputs {
    public static final NotEquals instance = new NotEquals();

    public static CachingNeuronUsingFunction makeNeuron() {
        return new CachingNeuronUsingFunction(instance);
    }

    public static CachingNeuronUsingFunction makeNeuron(List<SignalProvider> inputs) {
        return new CachingNeuronUsingFunction(inputs, instance);
    }

    public static CachingNeuronUsingFunction makeNeuron(SignalProvider ... inputs) {
        return new CachingNeuronUsingFunction(Arrays.asList(inputs), instance);
    }

    private NotEquals() { }

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
        int size = inputs.size();
        if (size == 2) {
            return inputs.get(0).getOutput() != inputs.get(1).getOutput() ? Short.MAX_VALUE : Short.MIN_VALUE;
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

        return (short)(Short.MAX_VALUE - ((double)(maxCount - 1) / (size - 1)) * RANGE_INT);
    }
}

