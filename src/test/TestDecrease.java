package test;

import neuron.*;

public class TestDecrease {
    public static void main (String[] args) {
        SwitchIterable si = new SwitchIterable(Decrease.makeNeuron(), 0);

        for (short output : si) {
            System.out.println(si.getControlInput() + "\t" + output);
        }
    }
}
