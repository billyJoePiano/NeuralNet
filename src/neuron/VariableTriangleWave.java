package neuron;

import java.util.*;

import static neuron.StatelessFunction.mod;

/**
 * Forms a wave with a variable period (required) and variable phase-shift (optional, assumed 0 if not present).
 * While the phase-shift input impacts only the output of the current round, the period input determines the
 * output of the NEXT round, and therefore functions like a memory-neuron input.
 *
 * (OLD -- for commented out code) For a breakdown of the logic behind the calculations used in this implementation,
 * see the Excel spreadsheet at the root of the repo: "VariableTriangleWave (simplified example).xlsx"
 */
public class VariableTriangleWave extends VariableWave<VariableTriangleWave> {
    public static final double MAX_W_FLOOR_ALLOWANCE = (double)Short.MAX_VALUE + 1;
    public static final double TWICE_MAX_W_FLOOR_ALLOWANCE = (double)Short.MAX_VALUE * 2 + 1;
    public static final double TWICE_MIN_W_FLOOR_ALLOWANCE = (double)Short.MIN_VALUE * 2 + 1;

    public static final double TWICE_MAX = (double)Short.MAX_VALUE * 2;
    public static final double TWICE_MIN = (double)Short.MIN_VALUE * 2;
    public static final double TWICE_RANGE = TWICE_MAX - TWICE_MIN; //actually 2 less than RANGE * 2, see spreadsheet explanation for details

    @Override
    public int getMinInputs() {
        return 1;
    }

    @Override
    public int getMaxInputs() {
        return 2;
    }

    public VariableTriangleWave(SignalProvider period, SignalProvider phase, double periodMin, double periodMax) {
        this(List.of(period, phase), periodMin, periodMax);
    }

    public VariableTriangleWave(VariableTriangleWave cloneFrom) {
        super(cloneFrom);
    }

    public VariableTriangleWave(VariableTriangleWave cloneFrom, double periodMin, double periodMax) {
        super(cloneFrom, periodMin, periodMax);
    }

    public VariableTriangleWave(List<SignalProvider> inputs, double periodMin, double periodMax) {
        super(inputs, periodMin, periodMax);
    }

    protected short waveFunctionActual(double currentPosition) {
        if (currentPosition == 1.0) return -1;
        else return super.waveFunctionActual(currentPosition);
    }

    protected double waveFunctionNormalized(double currentPosition) {
        // https://handwiki.org/wiki/Triangle_wave
        return 2.0 * Math.abs(mod(currentPosition - 0.5, 2) - 1.0) - 1.0;
    }

    /*
    @Override
    protected final short calcOutput(List<SignalProvider> inputs) {
        double position;
        if (inputs.size() != 1) {
            position = this.nextPosition;

        } else {
            position = this.nextPosition + inputs.get(1).getOutput() * 2;

            while (position < TWICE_MIN_W_FLOOR_ALLOWANCE) position += TWICE_RANGE;
            while (position >= TWICE_MAX_W_FLOOR_ALLOWANCE) position -= TWICE_RANGE;
        }

        if (position >= MAX_W_FLOOR_ALLOWANCE) {
            return (short)(TWICE_MAX - Math.floor(position));

        } else if (position < Short.MIN_VALUE) {
            return (short)(TWICE_MIN - Math.floor(position));

        } else {
            return (short)Math.floor(position);
        }
    }

    @Override protected void assignNextPosition(double period) {
        this.nextPosition += TWICE_RANGE / period;

        while (this.nextPosition < TWICE_MIN_W_FLOOR_ALLOWANCE) this.nextPosition += TWICE_RANGE;
        while (this.nextPosition >= TWICE_MAX_W_FLOOR_ALLOWANCE) this.nextPosition -= TWICE_RANGE;
    }

    @Override
    protected double waveFunctionNormalized(double currentPosition) {
        return 0;
    }
     */

    @Override
    public VariableTriangleWave clone() {
        return new VariableTriangleWave(this);
    }

    @Override
    protected VariableTriangleWave cloneWith(double periodMin, double periodMax) {
        return new VariableTriangleWave(this, periodMin, periodMax);
    }
}
