package neuralNet.function;

import neuralNet.neuron.*;

import java.util.*;

import static neuralNet.util.Util.*;

/**
 * Negates the input value, and clips input -32,768 into output 32,767.  Zero is mapped to itself.
 * Note that there is NOT a perfect 1-to-1 mapping of negative values to positive values because of
 * this artifacts.  For a true 1-to-1 mapping, use NegateBalanced
 *
 * This function is the inverse of itself for all inputs EXCEPT -32,768
 */
public class NegateClipped implements FunctionWithInputs {
    public static final NegateClipped instance = new NegateClipped();

    public static CachingNeuronUsingFunction makeNeuron() {
        return new CachingNeuronUsingFunction(instance);
    }

    public static CachingNeuronUsingFunction makeNeuron(List<SignalProvider> inputs) {
        return new CachingNeuronUsingFunction(instance, inputs);
    }

    public static CachingNeuronUsingFunction makeNeuron(SignalProvider input) {
        return new CachingNeuronUsingFunction(instance, List.of(input));
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
