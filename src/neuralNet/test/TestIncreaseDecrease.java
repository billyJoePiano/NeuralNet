package neuralNet.test;

import neuralNet.function.*;

// Output can be copied into Excel spreadsheet for graphing
public class TestIncreaseDecrease {
    public static void main (String[] args) {
        //CompoundNeuron cn = new CompoundNeuron(Increase.makeNeuron(), Decrease.makeNeuron());
        //CompoundNeuron cn = new CompoundNeuron(Decrease.makeNeuron(), Increase.makeNeuron());
        //CompoundNeuron cn = new CompoundNeuron(Increase.makeNeuron(), Negate.makeNeuron(), Increase.makeNeuron());
        CompoundNeuron cn = new CompoundNeuron(Decrease.makeNeuron(), NegateBalanced.makeNeuron(), Decrease.makeNeuron());

        SwitchIterable si = new SwitchIterable(cn, 0);

        for (short output : si) {
            System.out.println(si.getControlInput() + "\t" + output);
        }
    }
}
