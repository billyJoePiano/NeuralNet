package neuralNet.function;

import neuralNet.neuron.*;

import static neuralNet.util.Util.*;

public enum SineWave implements WaveFunction {
    INSTANCE;

    public static final double PI = Math.PI;
    public static final double NEGATIVE_ONE = (-0.5 / MAX_PLUS_ONE); // -0.5 is used since the Wave neuralNet.neuron will shift another -0.5 before normalizing to the short range

    public static StaticWaveProvider makeNeuron(double period, double phase) {
        return new StaticWaveProvider(INSTANCE, period, phase);
    }

    public static VariableWaveNeuron makeNeuron(SignalProvider period, SignalProvider phase,
                                                double minPeriod, double maxPeriod) {

        return new VariableWaveNeuron(period, phase, INSTANCE, minPeriod, maxPeriod);
    }

    public static VariableWaveNeuron makeNeuron(SignalProvider period, double minPeriod, double maxPeriod) {
        return new VariableWaveNeuron(period, INSTANCE, minPeriod, maxPeriod);
    }

    @Override
    public double calc(double phasePosition) {
        if (phasePosition == 1.0) return NEGATIVE_ONE;
        else return Math.sin(phasePosition * PI);
    }
}
