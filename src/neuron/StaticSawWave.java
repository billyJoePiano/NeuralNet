package neuron;

import java.util.*;

/**
 * Forms a sawtooth wave, where the start of a period produces an output of 0.  In other words, a period begins
 * half-way through the ramp-up, and the sudden
 */
public class StaticSawWave extends StaticWave<StaticSawWave> {
    public final double periodInv2 = Math.abs(2 / this.period); // ??? Is this assigned after the super constructor has returned?

    public StaticSawWave(StaticWave cloneFrom) {
        super(cloneFrom);
    }

    public StaticSawWave(double period, double phase) {
        super(period, phase);
    }

    @Override
    protected StaticSawWave cloneWith(double period, double phase) {
        return new StaticSawWave(period, phase);
    }


    @Override
    protected short calcOutput(List<SignalProvider> inputs) {
        return (short)Math.round(this.rounds * this.periodInv2 * MAX_PLUS_ONE - 0.5);
    }

    @Override
    public StaticSawWave clone() {
        return new StaticSawWave(this);
    }
}
