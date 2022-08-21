package neuralNet.function;

import neuralNet.neuron.*;

import java.util.*;

public class SoftSwitch implements StatelessFunction {
    public static final SoftSwitch instance = new SoftSwitch();

    public static CachingNeuronUsingStatelessFunction makeNeuron() {
        return new CachingNeuronUsingStatelessFunction(instance);
    }

    public static CachingNeuronUsingStatelessFunction makeNeuron(List<SignalProvider> inputs) {
        return new CachingNeuronUsingStatelessFunction(inputs, instance);
    }

    public static CachingNeuronUsingStatelessFunction makeNeuron(SignalProvider ... inputs) {
        return new CachingNeuronUsingStatelessFunction(Arrays.asList(inputs), instance);
    }

    private SoftSwitch() { }

    @Override
    public int getMinInputs() {
        return 3;
    }

    @Override
    public int getMaxInputs() {
        return 65;
    }

    @Override
    public short calcOutput(List<SignalProvider> inputs) {
        int controlInput = inputs.get(0).getOutput();
        int len = inputs.size();
        
        double index = (controlInput + ZEROIZE_INT) * (len - 2)
                * RANGE_INV + 1;

        int primary = (int)Math.round(index);
        if (primary >= len) {
            primary = len - 1;
        }
        else if (primary < 1) primary = 1;

        double secondaryWeight = index - primary;
        int secondary;
        if (secondaryWeight >= 0) {
            secondary = primary + 1;
            if (secondary >= len) {
                return inputs.get(primary).getOutput();
            }

        } else {
            secondaryWeight = -secondaryWeight;

            secondary = primary - 1;
            if (secondary < 1) {
                throw new IllegalStateException();
            }
        }

        double primaryWeight = 1 - secondaryWeight;

        return (short)Math.round(
                primaryWeight * inputs.get(primary).getOutput()
                        + secondaryWeight * inputs.get(secondary).getOutput());
    }
}
