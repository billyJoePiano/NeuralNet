package neuron;

import java.util.*;

import static neuron.StatelessMutatableFunction.*;

/**
 * Forms a sine-wave with given period expressed as a coefficient of pi
 * (aka, where 2.0 represents a full sine-wave cycle) and a phase-shift
 * also expressed as coefficient of pi
 */
public abstract class StaticWave<N extends StaticWave<N>> extends CachingNeuron implements Mutatable<N> {
    public static final List<Param> MUTATION_PARAMS_POS = List.of(Param.DEFAULT, Param.BOOLEAN_NEG, Param.DEFAULT);
    public static final List<Param> MUTATION_PARAMS_NEG = List.of(Param.DEFAULT, Param.BOOLEAN, Param.DEFAULT);

    public final boolean sign; //period sign, true = positive, false = negative
    public final double phase;
    public final double period;
    public final double periodDecrement;  //normally the same as period.  When period < 1, periodDecrement = largest multiple of period that is <= 1
    protected double rounds;

    public StaticWave(StaticWave cloneFrom) {
        super();
        this.sign = cloneFrom.sign;
        this.period = cloneFrom.period;
        this.phase = cloneFrom.phase;
        this.periodDecrement = cloneFrom.periodDecrement;

        this.rounds = this.phase * Math.abs(this.period) / 2;
    }

    public StaticWave(StaticWave cloneFrom, double period, double phase) {
        super(cloneFrom);

        if (period == 0 || !(Double.isFinite(period) && Double.isFinite(phase))) {
            throw new IllegalArgumentException();
        }

        this.period = period;

        if (!(this.sign = period > 0)) {
            period = -period; // make positive for determining fractional periodDecrements
            phase = -phase; //negate, normalize, then negate again
        }

        while (phase < 0.0) phase += 2.0;
        while (phase >= 2.0) phase -= 2.0;

        this.phase = this.sign ? phase : -phase;

        if (period > 0.5) { //anything between 0.5 and 1.0 would still have a periodDecrement == period * 1
            this.periodDecrement = this.period;

        } else {
            int i = 3;
            while (period * i < 1) i++;
            this.periodDecrement = this.period * (i - 1);
        }

        this.rounds = this.phase * period / 2; //method-local 'period' variable will always be positive, but phase will have same sign as this.period
    }


    /**
     * When period is positive, this.phase will always be between 0 (inclusive) and 2 (exclusive).
     * When period is negative, this.phase will always be between 0 (inclusive) and -2 (exclusive).
     *
     * @param period
     * @param phase
     */
    public StaticWave(double period, double phase) {
        super();

        if (period == 0 || !(Double.isFinite(period) && Double.isFinite(phase))) {
            throw new IllegalArgumentException();
        }

        this.period = period;

        if (!(this.sign = period > 0)) {
            period = -period; // make positive for determining fractional periodDecrements
            phase = -phase; //negate, normalize, then negate again
        }

        while (phase < 0.0) phase += 2.0;
        while (phase >= 2.0) phase -= 2.0;

        this.phase = this.sign ? phase : -phase;

        if (period > 0.5) { //anything between 0.5 and 1.0 would still have a periodDecrement == period * 1
            this.periodDecrement = this.period;

        } else {
            int i = 3;
            while (period * i < 1) i++;
            this.periodDecrement = this.period * (i - 1);
        }

        this.rounds = this.phase * period / 2; //method-local 'period' variable will always be positive, but this.phase will have same sign as this.period
    }


    public void after() {
        this.getOutput(); // populates cache if it hasn't been already, before this.rounds is mutated
        if (this.sign) {
            this.rounds += 1;
            if (this.rounds >= this.period) {
                this.rounds -= this.periodDecrement;
                while (this.rounds >= this.period) this.rounds -= period;
            }

        } else {
            this.rounds -= 1;
            if (this.rounds <= this.period) {
                this.rounds -= this.periodDecrement; //periodDecrement will be negative in this scenario
                while (this.rounds <= this.period) this.rounds -= period;
            }
        }
    }

    @Override
    public void reset() {
        this.rounds = this.phase * Math.abs(this.period) / 2;
        super.reset();
    }

    @Override
    public List<Param> getMutationParams() {
        if (this.sign) return MUTATION_PARAMS_POS;
        else return MUTATION_PARAMS_NEG;
    }

    @Override
    public N mutate(short[] params) {
        double period = transformByMagnitudeAndSign(this.period, params[0], params[1]);
        double phase = this.phase + (double)params[2] / MAX_PLUS_ONE;
        return this.cloneWith(period, phase);
    }

    @Override
    public short[] getMutationParams(N toAchieve) {
        short[] period = toAchieveByMagnitudeAndSign(this.period, toAchieve.period);
        double phase = Math.round((toAchieve.phase - this.phase) * MAX_PLUS_ONE);
        while (phase > Short.MAX_VALUE) phase -= RANGE;
        while (phase < Short.MIN_VALUE) phase += RANGE;

        return new short[] { period[0], period[1], (short)phase };
    }

    protected abstract N cloneWith(double period, double phase);
}
