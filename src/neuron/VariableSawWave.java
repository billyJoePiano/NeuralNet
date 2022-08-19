package neuron;

import java.util.*;

/**
 * Forms a wave with a variable period (required) and variable phase-shift (optional, assumed 0 if not present).
 * While the phase-shift input impacts only the output of the current round, the period input determines the
 * output of the NEXT round, and therefore functions like a memory-neuron input.
 */
public class VariableSawWave extends VariableWave<VariableSawWave> {
    public static final double MAX_W_FLOOR_ALLOWANCE = (double)Short.MAX_VALUE + 1;

    public VariableSawWave(SignalProvider period, SignalProvider phase, double periodMin, double periodMax) {
        this(List.of(period, phase), periodMin, periodMax);
    }

    public VariableSawWave(VariableSawWave cloneFrom) {
        super(cloneFrom);
    }

    public VariableSawWave(VariableSawWave cloneFrom, double periodMin, double periodMax) {
        super(cloneFrom, periodMin, periodMax);
    }

    public VariableSawWave(List<SignalProvider> inputs, double periodMin, double periodMax) {
        super(inputs, periodMin, periodMax);
    }

    @Override
    public int getMinInputs() {
        return 1;
    }

    @Override
    public int getMaxInputs() {
        return 2;
    }

    @Override
    protected final short calcOutput(List<SignalProvider> inputs) {
        if (inputs.size() == 1) {
            return (short)Math.round(this.nextPosition);
        }

        double position = this.nextPosition + inputs.get(1).getOutput();

        while (this.nextPosition < Short.MIN_VALUE) this.nextPosition += NORMALIZE;
        while (this.nextPosition >= MAX_W_FLOOR_ALLOWANCE) this.nextPosition -= NORMALIZE;

        return (short)Math.floor(position);
    }

    @Override
    public void assignNextPosition(double period) {
        this.nextPosition += NORMALIZE / period;

        while (this.nextPosition < Short.MIN_VALUE) this.nextPosition += NORMALIZE;
        while (this.nextPosition >= MAX_W_FLOOR_ALLOWANCE) this.nextPosition -= NORMALIZE;
    }

    @Override
    protected double waveFunctionNormalized(double currentPosition) {
        return 0;
    }

    @Override
    public VariableSawWave clone() {
        return new VariableSawWave(this);
    }

    @Override
    protected VariableSawWave cloneWith(double periodMin, double periodMax) {
        return new VariableSawWave(this, periodMin, periodMax);
    }
}
