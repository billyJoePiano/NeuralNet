package neuralNet.function;

import neuralNet.neuron.*;

import java.lang.invoke.*;
import java.util.*;

import static neuralNet.util.Util.*;

public enum MultiplyClipped implements NeuralFunction {
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
        double product = 1;

        for (SignalProvider neuron : inputs) {
            double val = (neuron.getOutput() + ZEROIZE) / MAX_PLUS_ONE; //normalizes to 0.0 - 2.0 (inclusive-exclusive)
            if (val == 0) return Short.MIN_VALUE; //no point in calculating when the product will remain zero
            product *= val;
        }

        return clip(product * MAX_PLUS_ONE - ZEROIZE);
    }
}
