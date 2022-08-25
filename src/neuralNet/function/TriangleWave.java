package neuralNet.function;

import neuralNet.neuron.*;

import java.io.*;

import static neuralNet.util.Util.*;

public class TriangleWave implements WaveFunction {
    public static final double PI = Math.PI;
    public static final double NEGATIVE_ONE = (-0.5 / MAX_PLUS_ONE); // -0.5 is used since the Wave neuralNet.neuron will shift another -0.5 before normalizing to the short range

    public static final TriangleWave instance = new TriangleWave();

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

    private TriangleWave() { }

    @Override
    public double calc(double phasePosition) {
        if (phasePosition == 1.0) return NEGATIVE_ONE;

        // https://handwiki.org/wiki/Triangle_wave
        return 2.0 * Math.abs(mod(phasePosition - 0.5, 2) - 1.0) - 1.0;
    }

    private Object readResolve() throws ObjectStreamException {
        return instance;
    }
}
