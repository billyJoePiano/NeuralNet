package neuron;

import java.util.*;

import static neuron.StatelessFunction.roundClip;

/**
 * A hyperbolic curve function which causes most outputs to be considerably smaller than the input.
 * The input and output are equal at Short.MAX_VALUE and Short.MIN_VALUE.
 *
 * Decrease and Increase are inverse functions of each other, in theory.  However, in practice,
 * the rounding required to fit the outputs into discrete integers causes some loss of precision
 * when used to invert one another.
 */
public class Decrease implements StatelessFunction {
    public static final double NUMERATOR = 536870912; // 2 to the 29th power
    public static final double ADDEND = 40131.439945759589960864302279981; // sqrt(2^29 + 2^30) - 1

    public static final Decrease instance = new Decrease();

    public static StatelessCachingNeuron makeNeuron() {
        return new StatelessCachingNeuron(instance);
    }

    public static StatelessCachingNeuron makeNeuron(List<SignalProvider> inputs) {
        return new StatelessCachingNeuron(inputs, instance);
    }

    public static StatelessCachingNeuron makeNeuron(SignalProvider input) {
        return new StatelessCachingNeuron(List.of(input), instance);
    }

    private Decrease() { }

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