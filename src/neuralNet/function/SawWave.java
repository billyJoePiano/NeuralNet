package neuralNet.function;

import neuralNet.network.*;
import neuralNet.neuron.*;

public class SawWave implements WaveFunction {
    public static final SawWave instance = new SawWave();

    public static StaticWaveNeuron makeNeuron(double period, double phase) {
        return new StaticWaveNeuron(instance, period, phase);
    }

    public static VariableWaveNeuron makeNeuron(SignalProvider period, SignalProvider phase,
                                                double minPeriod, double maxPeriod) {

        return new VariableWaveNeuron(period, phase, instance, minPeriod, maxPeriod);
    }

    public static VariableWaveNeuron makeNeuron(SignalProvider period, double minPeriod, double maxPeriod) {
        return new VariableWaveNeuron(period, instance, minPeriod, maxPeriod);
    }

    @Override
    public double calc(double phasePosition) {
        if (phasePosition < 1.0) {
            return phasePosition;

        } else {
            return phasePosition - 2.0;
        }
    }
}
