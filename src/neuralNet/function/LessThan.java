package neuralNet.function;

import neuralNet.neuron.*;

import java.io.*;
import java.util.*;

/**
 * AKA AdditionNormalized
 */
public class LessThan implements FunctionWithInputs {
    public static final LessThan instance = new LessThan();

    public static CachingNeuronUsingFunction makeNeuron() {
        return new CachingNeuronUsingFunction(instance);
    }

    public static CachingNeuronUsingFunction makeNeuron(List<SignalProvider> inputs) {
        return new CachingNeuronUsingFunction(instance, inputs);
    }

    public static CachingNeuronUsingFunction makeNeuron(SignalProvider value, SignalProvider comparedTo) {
        return new CachingNeuronUsingFunction(instance, Arrays.asList(value, comparedTo));
    }

    private LessThan() { }

    @Override
    public int getMinInputs() {
        return 2;
    }

    @Override
    public int getMaxInputs() {
        return 2;
    }

    @Override
    public boolean inputOrderMatters() {
        return true;
    }

    @Override
    public short calcOutput(List<SignalProvider> inputs) {
        return inputs.get(0).getOutput() < inputs.get(1).getOutput() ? Short.MAX_VALUE : Short.MIN_VALUE;
    }

    private Object readResolve() throws ObjectStreamException {
        return instance;
    }
}
