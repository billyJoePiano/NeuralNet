package neuron;

import java.util.*;

public class HardSwitch implements StatelessFunction {
    public static final HardSwitch instance = new HardSwitch();

    public static StatelessCachingNeuron makeNeuron() {
        return new StatelessCachingNeuron(instance);
    }

    public static StatelessCachingNeuron makeNeuron(List<SignalProvider> inputs) {
        return new StatelessCachingNeuron(inputs, instance);
    }

    public static StatelessCachingNeuron makeNeuron(SignalProvider ... inputs) {
        return new StatelessCachingNeuron(Arrays.asList(inputs), instance);
    }

    private HardSwitch() { }

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
        int len = inputs.size() - 1; //length minus the control input
        
        int index = (int)((controlInput + ZEROIZE_INT) * len
                * RANGE_INV) + 1;

        if (index >= len) index = len;
        else if (index < 1) index = 1;

        return inputs.get(index).getOutput();
    }
}
