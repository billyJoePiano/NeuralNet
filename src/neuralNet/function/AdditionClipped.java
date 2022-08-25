package neuralNet.function;

import neuralNet.neuron.*;

import java.io.*;
import java.util.*;

import static neuralNet.util.Util.*;

public class AdditionClipped implements FunctionWithInputs {
    public static final AdditionClipped instance = new AdditionClipped();

    public static CachingNeuronUsingFunction makeNeuron() {
        return new CachingNeuronUsingFunction(instance);
    }

    public static CachingNeuronUsingFunction makeNeuron(List<SignalProvider> inputs) {
        return new CachingNeuronUsingFunction(instance, inputs);
    }

    public static CachingNeuronUsingFunction makeNeuron(SignalProvider ... inputs) {
        return new CachingNeuronUsingFunction(instance, inputs);
    }

    private AdditionClipped() { }

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
        int sum = 0;

        for (SignalProvider neuron : inputs) {
            sum += neuron.getOutput();
        }

        return clip(sum);
    }

    private Object readResolve() throws ObjectStreamException {
        return instance;
    }
}
