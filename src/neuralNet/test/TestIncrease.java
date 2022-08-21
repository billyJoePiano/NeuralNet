package neuralNet.test;

import neuralNet.function.*;

public class TestIncrease {
    public static void main (String[] args) {
        SwitchIterable si = new SwitchIterable(Increase.makeNeuron(), 0);

        for (short output : si) {
            System.out.println(si.getControlInput() + "\t" + output);
        }
    }
}
