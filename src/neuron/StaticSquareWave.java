package neuron;

import java.util.*;

/**
 * Forms a sine-wave form
 */
public class StaticSquareWave extends StaticWave<StaticSquareWave> {
    public final double periodHalfway = this.period / 2;

    public StaticSquareWave(StaticWave cloneFrom) {
        super(cloneFrom);
    }

    public StaticSquareWave(StaticWave cloneFrom, double period, double phase) {
        super(cloneFrom, period, phase);
    }

    public StaticSquareWave(double period, double phase) {
        super(period, phase);
    }

    @Override
    protected StaticSquareWave cloneWith(double period, double phase) {
        return null;
    }


    @Override
    protected short calcOutput(List<SignalProvider> inputs) {
        if (this.sign) {
            return this.rounds < this.periodHalfway ? Short.MAX_VALUE : Short.MIN_VALUE;

        } else {
            if (this.rounds == 0) return Short.MAX_VALUE; //would be this.period (which is negative) but this.rounds is exclusive of that value
            return this.rounds < this.periodHalfway ? Short.MAX_VALUE : Short.MIN_VALUE;
        }
    }

    @Override
    public StaticSquareWave clone() {
        return new StaticSquareWave(this);
    }
}
