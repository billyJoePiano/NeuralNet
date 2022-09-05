package neuralNet.function;

import neuralNet.neuron.*;

import java.lang.invoke.*;
import java.util.*;

public enum Widen implements NeuralFunction {
    INSTANCE;

    public static final long HASH_HEADER = NeuralHash.HEADERS.get(MethodHandles.lookup().lookupClass());
    public long hashHeader() {
        return HASH_HEADER;
    }

    public static final double MULTIPLIER = 181.01933598375616624661615669884; // 128 * sqrt(2) aka sqrt(2^15 or 32768)

    public static CachingNeuronUsingFunction makeNeuron() {
        return new CachingNeuronUsingFunction(INSTANCE);
    }

    public static CachingNeuronUsingFunction makeNeuron(List<SignalProvider> inputs) {
        return new CachingNeuronUsingFunction(INSTANCE, inputs);
    }

    public static CachingNeuronUsingFunction makeNeuron(SignalProvider input) {
        return new CachingNeuronUsingFunction(INSTANCE, Arrays.asList(input));
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
