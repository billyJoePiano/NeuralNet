package neuron;

import java.util.*;

public class Widen implements StatelessFunction {
    public static final double MULTIPLIER = 181.01933598375616624661615669884; // 128 * sqrt(2) aka sqrt(2^15 or 32768)

    public static final Widen instance = new Widen();

    public static StatelessCachingNeuron makeNeuron() {
        return new StatelessCachingNeuron(instance);
    }

    public static StatelessCachingNeuron makeNeuron(List<SignalProvider> inputs) {
        return new StatelessCachingNeuron(inputs, instance);
    }

    public static StatelessCachingNeuron makeNeuron(SignalProvider input) {
        return new StatelessCachingNeuron(Arrays.asList(input), instance);
    }

    private Widen() { }

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
        //return (short)(Math.round(Math.sqrt(inputs.get(0).getOutput() + SHORT_ZEROIZE) * 256) + Short.MIN_VALUE);

        int output = inputs.get(0).getOutput();
        double result;

        if (output >= 0) {
            result = Math.round(Math.sqrt((output + 1) * 32768));
            if (result >= Short.MAX_VALUE) return Short.MAX_VALUE;
            else return (short) result;

        } else {
            result = -Math.round(Math.sqrt(-output * 32768));
            if (result <= Short.MIN_VALUE) return Short.MIN_VALUE;
            else return (short)result;
        }
    }
}