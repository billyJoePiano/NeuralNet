package neuralNet.function;

import neuralNet.neuron.*;

import java.io.*;
import java.util.*;

public class Closeness implements FunctionWithInputs {
    public static Closeness instance = new Closeness();

    public static CachingNeuronUsingFunction makeNeuron() {
        return new CachingNeuronUsingFunction(instance);
    }

    public static CachingNeuronUsingFunction makeNeuron(List<SignalProvider> inputs) {
        return new CachingNeuronUsingFunction(instance, inputs);
    }

    public static CachingNeuronUsingFunction makeNeuron(SignalProvider ... inputs) {
        return new CachingNeuronUsingFunction(instance, inputs);
    }

    private Closeness() { }

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
        int max = inputs.get(0).getOutput();
        int min = max;

        for (SignalProvider neuron : inputs) {
            short output = neuron.getOutput();

            if (output > max) {
                max = output;

            } else if (output < min) {
                min = output;
            }
        }

        return (short)(Short.MAX_VALUE - max + min);
    }

    private Object readResolve() throws ObjectStreamException {
        return instance;
    }
}
