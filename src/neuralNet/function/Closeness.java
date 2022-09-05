package neuralNet.function;

import neuralNet.neuron.*;

import java.lang.invoke.*;
import java.util.*;

public enum Closeness implements NeuralFunction {
    INSTANCE;

    public static final long HASH_HEADER = NeuralHash.HEADERS.get(MethodHandles.lookup().lookupClass());
    public long hashHeader() {
        return HASH_HEADER;
    }

    public static CachingNeuronUsingFunction makeNeuron() {
        return new CachingNeuronUsingFunction(INSTANCE);
    }

    public static CachingNeuronUsingFunction makeNeuron(List<SignalProvider> inputs) {
        return new CachingNeuronUsingFunction(INSTANCE, inputs);
    }

    public static CachingNeuronUsingFunction makeNeuron(SignalProvider ... inputs) {
        return new CachingNeuronUsingFunction(INSTANCE, inputs);
    }

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
