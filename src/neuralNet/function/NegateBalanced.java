package neuralNet.function;

import neuralNet.neuron.*;

import java.lang.invoke.*;
import java.util.*;

/**
 * Negates the input plus one.  This maps every negative value to a positive value 1-to-1, where zero
 * is considered a positive values.  Therefore:
 *        0 maps to -1
 *      192 maps to -193
 *  -32,768 maps to 32,767    (min and max values, respectively)
 *          ETC
 *
 * This function is the perfect inverse of itself, for all input values
 */
public enum NegateBalanced implements NeuralFunction {
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

    public static CachingNeuronUsingFunction makeNeuron(SignalProvider input) {
        return new CachingNeuronUsingFunction(INSTANCE, List.of(input));
    }

    @Override
    public int getMinInputs() {
        return 1;
    }

    @Override
    public int getMaxInputs() {
        return 1;
    }

    @Override
    public short calcOutput(List<SignalProvider> inputs) {
        return (short) -(((int)inputs.get(0).getOutput()) + 1);
    }
}
