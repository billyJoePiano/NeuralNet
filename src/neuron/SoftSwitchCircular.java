package neuron;

import java.util.*;

public class SoftSwitchCircular implements StatelessFunction {
    public static final SoftSwitchCircular instance = new SoftSwitchCircular();

    public static StatelessCachingNeuron makeNeuron() {
        return new StatelessCachingNeuron(instance);
    }

    public static StatelessCachingNeuron makeNeuron(List<SignalProvider> inputs) {
        return new StatelessCachingNeuron(inputs, instance);
    }

    public static StatelessCachingNeuron makeNeuron(SignalProvider ... inputs) {
        return new StatelessCachingNeuron(Arrays.asList(inputs), instance);
    }

    private SoftSwitchCircular() { }

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
        
        double index = (controlInput + ZEROIZE_INT) * (len - 1)
                        * RANGE_INV + 0.5;

        int primary = (int)Math.round(index);
        if (primary >= len) primary = len - 1;
        else if (primary < 1) primary = 1;

        double secondaryWeight = index - primary;
        int secondary;
        if (secondaryWeight >= 0) {
            secondary = primary + 1;
            if (secondary >= len) secondary = 1;

        } else {
            secondaryWeight = -secondaryWeight;

            secondary = primary - 1;
            if (secondary < 1) secondary = len - 1;
        }

        double primaryWeight = 1 - secondaryWeight;

        return (short)Math.round(
                  primaryWeight * inputs.get(primary).getOutput()
              + secondaryWeight * inputs.get(secondary).getOutput());
    }
}
