package neuron;

import java.util.*;

public class AdditionCircular implements StatelessFunction {
    public static final AdditionCircular instance = new AdditionCircular();

    public static StatelessCachingNeuron makeNeuron() {
        return new StatelessCachingNeuron(instance);
    }

    public static StatelessCachingNeuron makeNeuron(List<SignalProvider> inputs) {
        return new StatelessCachingNeuron(inputs, instance);
    }

    public static StatelessCachingNeuron makeNeuron(SignalProvider ... inputs) {
        return new StatelessCachingNeuron(Arrays.asList(inputs), instance);
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
