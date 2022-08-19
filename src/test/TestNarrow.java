package test;

import neuron.*;

public class TestNarrow {
    public static void main (String[] args) {
        SwitchIterable si = new SwitchIterable(new StatelessCachingNeuron(Narrow.instance), 0);

        for (short output : si) {
            System.out.println(si.getControlInput() + "\t" + output);
        }
    }
}
