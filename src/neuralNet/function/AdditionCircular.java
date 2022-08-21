package neuralNet.function;

import neuralNet.neuron.*;

import java.util.*;

public class AdditionCircular implements StatelessFunction {
    public static final AdditionCircular instance = new AdditionCircular();

    public static CachingNeuronUsingStatelessFunction makeNeuron() {
        return new CachingNeuronUsingStatelessFunction(instance);
    }

    public static CachingNeuronUsingStatelessFunction makeNeuron(List<SignalProvider> inputs) {
        return new CachingNeuronUsingStatelessFunction(inputs, instance);
    }

    public static CachingNeuronUsingStatelessFunction makeNeuron(SignalProvider ... inputs) {
        return new CachingNeuronUsingStatelessFunction(Arrays.asList(inputs), instance);
    }

    private AdditionCircular() { }

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

        return (short)sum;
    }
}
