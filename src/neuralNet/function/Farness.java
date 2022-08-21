package neuralNet.function;

import neuralNet.neuron.*;

import java.util.*;

public class Farness implements StatelessFunction {
    public static final Farness instance = new Farness();

    public static CachingNeuronUsingStatelessFunction makeNeuron() {
        return new CachingNeuronUsingStatelessFunction(instance);
    }

    public static CachingNeuronUsingStatelessFunction makeNeuron(List<SignalProvider> inputs) {
        return new CachingNeuronUsingStatelessFunction(inputs, instance);
    }

    public static CachingNeuronUsingStatelessFunction makeNeuron(SignalProvider ... inputs) {
        return new CachingNeuronUsingStatelessFunction(Arrays.asList(inputs), instance);
    }

    private Farness() { }

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

        return (short)(max - min + Short.MIN_VALUE);
    }
}

