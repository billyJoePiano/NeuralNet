package neuralNet.function;

import neuralNet.neuron.*;

import java.lang.invoke.*;
import java.util.*;

public enum Narrow implements NeuralFunction {
    INSTANCE;

    public static final long HASH_HEADER = NeuralHash.HEADERS.get(MethodHandles.lookup().lookupClass());
    public long hashHeader() {
        return HASH_HEADER;
    }

    public static final double INV_MULTIPLIER = 1 / (double)32768;

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
            result = Math.round(output * output * INV_MULTIPLIER);
            if (result >= Short.MAX_VALUE) return Short.MAX_VALUE;
            else return (short) result;

        } else {
            output += 1;
            result = -Math.round(output * output * INV_MULTIPLIER);
            if (result <= Short.MIN_VALUE) return Short.MIN_VALUE;
            else return (short)result;
        }
    }
}
