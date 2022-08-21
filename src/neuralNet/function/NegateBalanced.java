package neuralNet.function;

import neuralNet.neuron.*;

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
public class NegateBalanced implements StatelessFunction {
    public static final NegateBalanced instance = new NegateBalanced();

    public static CachingNeuronUsingStatelessFunction makeNeuron() {
        return new CachingNeuronUsingStatelessFunction(instance);
    }

    public static CachingNeuronUsingStatelessFunction makeNeuron(List<SignalProvider> inputs) {
        return new CachingNeuronUsingStatelessFunction(inputs, instance);
    }

    public static CachingNeuronUsingStatelessFunction makeNeuron(SignalProvider input) {
        return new CachingNeuronUsingStatelessFunction(List.of(input), instance);
    }

    private NegateBalanced() { }

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
