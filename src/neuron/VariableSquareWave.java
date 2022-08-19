package neuron;

import java.util.*;

public class VariableSquareWave extends VariableWave<VariableSquareWave> {

    public VariableSquareWave(SignalProvider period, SignalProvider phase, double periodMin, double periodMax) {
        super(List.of(period, phase), periodMin, periodMax);
    }

    public VariableSquareWave(VariableSquareWave cloneFrom) {
        super(cloneFrom);
    }

    public VariableSquareWave(VariableSquareWave cloneFrom, double periodMin, double periodMax) {
        super(cloneFrom, periodMin, periodMax);
    }

    public VariableSquareWave(List<SignalProvider> inputs, double periodMin, double periodMax) {
        super(inputs, periodMin, periodMax);
    }

    @Override
    protected short waveFunctionActual(double currentPosition) {
        return currentPosition < 1 ? Short.MAX_VALUE : Short.MIN_VALUE;
    }

    @Override
    protected double waveFunctionNormalized(double currentPosition) {
        return 0; //not used.  waveFunctionActual is overridden instead
    }

    @Override
    public VariableSquareWave clone() {
        return new VariableSquareWave(this);
    }

    @Override
    protected VariableSquareWave cloneWith(double periodMin, double periodMax) {
        return new VariableSquareWave(this, periodMin, periodMax);
    }
}
