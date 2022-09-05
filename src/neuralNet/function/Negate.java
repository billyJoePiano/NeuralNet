package neuralNet.function;

import neuralNet.neuron.*;

import java.lang.invoke.*;
import java.util.*;

/**
 * Negates the input value, except maps -32,768 to 32,767, and vice-versa.  Zero is mapped to itself.
 * Note that there is NOT a perfect 1-to-1 mapping of negative values to positive values because of
 * this.  For a true 1-to-1 mapping, use NegateBalanced
 *
 * This function is the inverse of itself for all inputs EXCEPT -32,767 (one more than Short.MIN_VALUE)
 * which first maps to 32,767, then back to -32,768 (Short.MIN_VALUE)
 */
public enum Negate implements NeuralFunction {
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
        short input = inputs.get(0).getOutput();
        if (input == Short.MAX_VALUE) return Short.MIN_VALUE;
        if (input == Short.MIN_VALUE) return Short.MAX_VALUE;
        else return (short)-input;
    }
}
