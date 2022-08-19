package neuron;

import java.util.*;

import static neuron.StatelessFunction.*;

/**
 * Forms a sine-wave with given period expressed as a coefficient of pi
 * (aka, where 2.0 represents a full sine-wave cycle) and a phase-shift
 * also expressed as coefficient of pi
 */
public class StaticTriangleWave extends StaticWave<StaticTriangleWave> {
    public final double periodInv = Math.abs(1 / this.period); // ??? Is this assigned after the super constructor has returned?
    public final double halfPeriodSigned = this.period / 2;
    public final double halfPeriod = Math.abs(this.halfPeriodSigned);
    public final double quarterPeriod = Math.abs(this.period / 4);


    public StaticTriangleWave(double period, double phase) {
        super(period, phase);
    }

    public StaticTriangleWave(StaticWave cloneFrom) {
        super(cloneFrom);
    }


    @Override
    protected short calcOutput(List<SignalProvider> inputs) {
        // https://handwiki.org/wiki/Triangle_wave
        if (this.rounds == this.halfPeriodSigned) {
            return -1;
        }

        return roundClip((Math.abs(mod(this.rounds - this.quarterPeriod, this.period) - this.halfPeriod) * this.periodInv - 0.25) * TWICE_NORMALIZE - 0.5);
        /*
        double mod = mod(this.rounds - this.quarterPeriod, this.period);
        double absArg = mod - this.halfPeriod;
        double abs = Math.abs(absArg);
        double fraction = abs * this.periodInv;
        double shifted = fraction - 0.25;
        double expanded = shifted * TWICE_NORMALIZE;
        double offset = expanded - 0.5;
        short roundClip = roundClip(offset);

        return roundClip;
         */
    }

    @Override
    public StaticTriangleWave clone() {
        return new StaticTriangleWave(this);
    }

    @Override
    protected StaticTriangleWave cloneWith(double period, double phase) {
        return new StaticTriangleWave(period, phase);
    }
}
