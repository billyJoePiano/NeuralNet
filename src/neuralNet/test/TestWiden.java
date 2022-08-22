package neuralNet.test;

import neuralNet.function.*;

public class TestWiden {
    public static void main (String[] args) {
        SwitchIterable si = new SwitchIterable(Widen.makeNeuron(), 0);

        for (short output : si) {
            System.out.println(si.getControlInput() + "\t" + output);
        }
    }
}
