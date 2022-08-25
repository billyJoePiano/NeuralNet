package neuralNet.function;

import neuralNet.neuron.*;

import java.io.*;
import java.util.*;

public class Max implements FunctionWithInputs {
    public static final Max instance = new Max();

    public static CachingNeuronUsingFunction makeNeuron() {
        return new CachingNeuronUsingFunction(instance);
    }

    public static CachingNeuronUsingFunction makeNeuron(List<SignalProvider> inputs) {
        return new CachingNeuronUsingFunction(instance, inputs);
    }

    public static CachingNeuronUsingFunction makeNeuron(SignalProvider ... inputs) {
        return new CachingNeuronUsingFunction(instance, inputs);
    }

    private Max() { }

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
        short max = inputs.get(0).getOutput();

        for (SignalProvider neuron : inputs) {
            short output = neuron.getOutput();
            if (output > max) {
                max = output;
            }
        }

        return max;
    }

    private Object readResolve() throws ObjectStreamException {
        return instance;
    }
}

