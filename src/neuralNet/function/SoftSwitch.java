package neuralNet.function;

import neuralNet.neuron.*;

import java.io.*;
import java.util.*;

import static neuralNet.util.Util.*;

public class SoftSwitch implements FunctionWithInputs {
    public static final SoftSwitch instance = new SoftSwitch();

    public static CachingNeuronUsingFunction makeNeuron() {
        return new CachingNeuronUsingFunction(instance);
    }

    public static CachingNeuronUsingFunction makeNeuron(List<SignalProvider> inputs) {
        return new CachingNeuronUsingFunction(instance, inputs);
    }

    public static CachingNeuronUsingFunction makeNeuron(SignalProvider ... inputs) {
        return new CachingNeuronUsingFunction(instance, inputs);
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
    public boolean inputOrderMatters() {
        return true;
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

    private Object readResolve() throws ObjectStreamException {
        return instance;
    }
}
