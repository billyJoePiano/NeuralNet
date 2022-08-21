package neuralNet.function;

import neuralNet.neuron.*;

import java.util.*;

public class Min implements StatelessFunction {
    public static final Min instance = new Min();

    public static CachingNeuronUsingStatelessFunction makeNeuron() {
        return new CachingNeuronUsingStatelessFunction(instance);
    }

    public static CachingNeuronUsingStatelessFunction makeNeuron(List<SignalProvider> inputs) {
        return new CachingNeuronUsingStatelessFunction(inputs, instance);
    }

    public static CachingNeuronUsingStatelessFunction makeNeuron(SignalProvider ... inputs) {
        return new CachingNeuronUsingStatelessFunction(Arrays.asList(inputs), instance);
    }

    private Min() { }

    @Override
    public int getMinInputs() {
        return 2;
    }

    @Override
    public int getMaxInputs() {
        return 256;
    }

    @Override
    public short calcOutput(List<SignalProvider> inputs) {
        short min = inputs.get(0).getOutput();

        for (SignalProvider neuron : inputs) {
            short output = neuron.getOutput();
            if (output < min) {
                min = output;
            }
        }

        return min;
    }
}

