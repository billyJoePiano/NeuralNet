package neuralNet.function;

import neuralNet.neuron.*;

import java.util.*;

public class Narrow implements StatelessFunction {
    public static final double INV_MULTIPLIER = 1 / (double)32768;

    public static final Narrow instance = new Narrow();

    public static CachingNeuronUsingStatelessFunction makeNeuron() {
        return new CachingNeuronUsingStatelessFunction(instance);
    }

    public static CachingNeuronUsingStatelessFunction makeNeuron(List<SignalProvider> inputs) {
        return new CachingNeuronUsingStatelessFunction(inputs, instance);
    }

    public static CachingNeuronUsingStatelessFunction makeNeuron(SignalProvider input) {
        return new CachingNeuronUsingStatelessFunction(Arrays.asList(input), instance);
    }

    private Narrow() { }

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
