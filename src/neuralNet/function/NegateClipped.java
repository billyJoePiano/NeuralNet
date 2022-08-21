package neuralNet.function;

import neuralNet.neuron.*;

import java.util.*;

import static neuralNet.function.StatelessFunction.*;

/**
 * Negates the input value, and clips input -32,768 into output 32,767.  Zero is mapped to itself.
 * Note that there is NOT a perfect 1-to-1 mapping of negative values to positive values because of
 * this artifacts.  For a true 1-to-1 mapping, use NegateBalanced
 *
 * This function is the inverse of itself for all inputs EXCEPT -32,768
 */
public class NegateClipped implements StatelessFunction {
    public static final NegateClipped instance = new NegateClipped();

    public static CachingNeuronUsingStatelessFunction makeNeuron() {
        return new CachingNeuronUsingStatelessFunction(instance);
    }

    public static CachingNeuronUsingStatelessFunction makeNeuron(List<SignalProvider> inputs) {
        return new CachingNeuronUsingStatelessFunction(inputs, instance);
    }

    public static CachingNeuronUsingStatelessFunction makeNeuron(SignalProvider input) {
        return new CachingNeuronUsingStatelessFunction(List.of(input), instance);
    }

    private NegateClipped() { }

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
        return clip(-(int)inputs.get(0).getOutput());
    }
}
