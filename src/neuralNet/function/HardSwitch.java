package neuralNet.function;

import neuralNet.neuron.*;

import java.io.*;
import java.util.*;

import static neuralNet.util.Util.*;

public class HardSwitch implements FunctionWithInputs {
    public static final HardSwitch instance = new HardSwitch();

    public static CachingNeuronUsingFunction makeNeuron() {
        return new CachingNeuronUsingFunction(instance);
    }

    public static CachingNeuronUsingFunction makeNeuron(List<SignalProvider> inputs) {
        return new CachingNeuronUsingFunction(instance, inputs);
    }

    public static CachingNeuronUsingFunction makeNeuron(SignalProvider ... inputs) {
        return new CachingNeuronUsingFunction(instance, inputs);
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
    public boolean inputOrderMatters() {
        return true;
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

    private Object readResolve() throws ObjectStreamException {
        return instance;
    }
}
