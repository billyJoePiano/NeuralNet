package neuralNet.function;

import neuralNet.neuron.*;

import static neuralNet.util.Util.*;

public class SquareWave implements WaveFunction {
    public static final double PI = Math.PI;
    public static final double NEGATIVE_ONE = (-0.5 / MAX_PLUS_ONE); // -0.5 is used since the Wave neuralNet.neuron will shift another -0.5 before normalizing to the short range

    public static final SquareWave instance = new SquareWave();

    public static StaticWaveProvider makeNeuron(double period, double phase) {
        return new StaticWaveProvider(instance, period, phase);
    }

    public static VariableWaveNeuron makeNeuron(SignalProvider period, SignalProvider phase,
                                                double minPeriod, double maxPeriod) {

        return new VariableWaveNeuron(period, phase, instance, minPeriod, maxPeriod);
    }

    public static VariableWaveNeuron makeNeuron(SignalProvider period, double minPeriod, double maxPeriod) {
        return new VariableWaveNeuron(period, instance, minPeriod, maxPeriod);
    }

    private SquareWave() { }

    @Override
    public double calc(double phasePosition) {
        return phasePosition < 1.0 ? Short.MAX_VALUE : Short.MIN_VALUE;
    }
}
