package neuralNet.test;

import neuralNet.function.*;
import neuralNet.neuron.*;

public class TestNarrow {
    public static void main (String[] args) {
        SwitchIterable si = new SwitchIterable(new CachingNeuronUsingFunction(Narrow.INSTANCE), 0);

        for (short output : si) {
            System.out.println(si.getControlInput() + "\t" + output);
        }
    }
}
