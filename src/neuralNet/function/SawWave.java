package neuralNet.function;

import neuralNet.neuron.*;

public enum SawWave implements WaveFunction {
    INSTANCE;

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
        if (phasePosition < 1.0) {
            return phasePosition;

        } else {
            return phasePosition - 2.0;
        }
    }
}
