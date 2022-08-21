package neuralNet.function;

import neuralNet.neuron.*;

import java.util.*;

import static neuralNet.function.StatelessFunction.*;

public class DifferenceNormalized implements StatelessFunction {
    public static final DifferenceNormalized instance = new DifferenceNormalized();

    public static CachingNeuronUsingStatelessFunction makeNeuron() {
        return new CachingNeuronUsingStatelessFunction(instance);
    }

    public static CachingNeuronUsingStatelessFunction makeNeuron(List<SignalProvider> inputs) {
        return new CachingNeuronUsingStatelessFunction(inputs, instance);
    }

    public static CachingNeuronUsingStatelessFunction makeNeuron(SignalProvider value, SignalProvider comparedTo) {
        return new CachingNeuronUsingStatelessFunction(Arrays.asList(value, comparedTo), instance);
    }

    private DifferenceNormalized() { }

    @Override
    public int getMinInputs() {
        return 2;
    }

    @Override
    public int getMaxInputs() {
        return 2;
    }

    @Override
    public short calcOutput(List<SignalProvider> inputs) {
        return clip(Math.floor(((double)((int)inputs.get(0).getOutput() - (int)inputs.get(1).getOutput()) / 2.0)));
    }
}
