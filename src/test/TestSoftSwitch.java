package test;

import neuron.CachingNeuron;
import neuron.*;

import java.util.ArrayList;
import java.util.List;

// Output can be copied into Excel spreadsheet for graphing
public class TestSoftSwitch {
    public static void main(String args[]) {
        short[] params = new short[] { Short.MIN_VALUE, -24576, -16384, -8192, 0, 8191, 16383, 24575, Short.MAX_VALUE };
        //short[] params = new short[] { Short.MIN_VALUE, Short.MAX_VALUE, Short.MIN_VALUE / 2, Short.MAX_VALUE / 2};

        iteration(params);
    }

    public static void iteration(short[] params) {
        List<CachingNeuron> input = new ArrayList(params.length + 2);

        Neuron neuron = SoftSwitch.makeNeuron();

        SwitchIterable si = new SwitchIterable(neuron, params);

        for (Short output : si) {
            System.out.println(si.getControlInput() + "\t" + output);
        }
    }
}
