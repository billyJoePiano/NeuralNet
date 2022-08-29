package neuralNet.function;

import neuralNet.neuron.*;

import java.util.*;

public enum Min implements FunctionWithInputs {
    INSTANCE;

    public static CachingNeuronUsingFunction makeNeuron() {
        return new CachingNeuronUsingFunction(INSTANCE);
    }

    public static CachingNeuronUsingFunction makeNeuron(List<SignalProvider> inputs) {
        return new CachingNeuronUsingFunction(INSTANCE, inputs);
    }

    public static CachingNeuronUsingFunction makeNeuron(SignalProvider ... inputs) {
        return new CachingNeuronUsingFunction(INSTANCE, inputs);
    }

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
