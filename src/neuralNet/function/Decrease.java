package neuralNet.function;

import neuralNet.neuron.*;

import java.lang.invoke.*;
import java.util.*;

import static neuralNet.util.Util.*;

/**
 * A hyperbolic curve function which causes most outputs to be considerably smaller than the input.
 * The input and output are equal at Short.MAX_VALUE and Short.MIN_VALUE.
 *
 * Decrease and Increase are inverse functions of each other, in theory.  However, in practice,
 * the rounding required to fit the outputs into discrete integers causes some loss of precision
 * when used to invert one another.
 */
public enum Decrease implements NeuralFunction {
    INSTANCE;

    public static final long HASH_HEADER = NeuralHash.HEADERS.get(MethodHandles.lookup().lookupClass());
    public long hashHeader() {
        return HASH_HEADER;
    }

    public static final double NUMERATOR = 536870912; // 2 to the 29th power
    public static final double ADDEND = 40131.439945759589960864302279981; // sqrt(2^29 + 2^30) - 1

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
        //double orig = inputs.get(0).getOutput() + SHORT_ZEROIZE;
        //return (short)(Math.round((orig * orig * SHORT_RANGE_INV) + Short.MIN_VALUE));

        /*
        double result = Math.round(-ADDEND - NUMERATOR / ((double)inputs.get(0).getOutput() - ADDEND));
        if (result >= Short.MAX_VALUE) return Short.MAX_VALUE;
        else if (result <= Short.MIN_VALUE) return Short.MIN_VALUE;
        else return (short)result;
         */

        return roundClip(-ADDEND - NUMERATOR / ((double)inputs.get(0).getOutput() - ADDEND));
    }
}
