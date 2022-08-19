package neuron;

import java.util.*;

import static neuron.StatelessFunction.roundClip;

public class VariableSineWave extends VariableWave<VariableSineWave> {

    public VariableSineWave(SignalProvider period, SignalProvider phase, double periodMin, double periodMax) {
        super(List.of(period, phase), periodMin, periodMax);
    }

    public VariableSineWave(VariableSineWave cloneFrom) {
        super(cloneFrom);
    }

    public VariableSineWave(VariableSineWave cloneFrom, double minPeriod, double maxPeriod) {
        super(cloneFrom, minPeriod, maxPeriod);
    }

    public VariableSineWave(List<SignalProvider> inputs, double periodMin, double periodMax) {
        super(inputs, periodMin, periodMax);
    }

    @Override
    protected short waveFunctionActual(double currentPosition) {
        if (currentPosition == 1.0) return -1;

        short result = roundClip(Math.sin(currentPosition * PI) * MAX_PLUS_ONE - 0.5);
        return result;
    }

    @Override
    protected double waveFunctionNormalized(double currentPosition) {
        return Math.sin(currentPosition * PI);
    }

    @Override
    public VariableSineWave clone() {
        return new VariableSineWave(this);
    }

    @Override
    protected VariableSineWave cloneWith(double periodMin, double periodMax) {
        return new VariableSineWave(this, periodMin, periodMax);
    }
}
