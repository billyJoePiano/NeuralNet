package neuralNet.function;

import neuralNet.neuron.*;

import java.io.*;
import java.util.*;

import static neuralNet.util.Util.*;

public class Equals implements FunctionWithInputs {
    public static final Equals instance = new Equals();

    public static CachingNeuronUsingFunction makeNeuron() {
        return new CachingNeuronUsingFunction(instance);
    }

    public static CachingNeuronUsingFunction makeNeuron(List<SignalProvider> inputs) {
        return new CachingNeuronUsingFunction(instance, inputs);
    }

    public static CachingNeuronUsingFunction makeNeuron(SignalProvider ... inputs) {
        return new CachingNeuronUsingFunction(instance, inputs);
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

    private Object readResolve() throws ObjectStreamException {
        return instance;
    }
}
