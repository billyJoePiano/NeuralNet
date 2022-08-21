package neuralNet.test;

import neuralNet.function.*;


public class TestHardSwitch {
    private static final short[][] hardParams = new short[][] {
            {0},
            {-10923, 10922},
            {-16384, 0, 16384},
            {-19661, -6554, 6553, 19660},
            {-21845, -10923, 0, 10922, 21845}
    };

    public static void main(String args[]) {
        for (short[] params : hardParams) {
            iteration(params);
        }
    }

    public static void iteration(short[] params) {
        SwitchIterable si = new SwitchIterable(HardSwitch.makeNeuron(), params.length + 1);

        int iterationGroup = si.getIterationGroup();

        for (Short output : si) {
            if (iterationGroup < params.length
                    && si.getControlInput() >= params[iterationGroup]) {

                si.incrementIterationGroup();
                iterationGroup++;
                assert iterationGroup == si.getIterationGroup();
            }

            if (iterationGroup + 1 != output) {
                System.err.println(si + " -> " + output);
            }
        }

        System.out.println(si.getIterationCounts());
    }

}
