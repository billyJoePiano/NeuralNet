package neuron;

import java.util.*;

import static neuron.StatelessFunction.*;

/**
 * Forms a sine-wave with given period expressed as a coefficient of pi
 * (aka, where 2.0 represents a full sine-wave cycle) and a phase-shift
 * also expressed as coefficient of pi
 */
public class StaticSineWave extends StaticWave<StaticSineWave> {
    public final double halfPeriod = Math.abs(this.period / 2);

    public StaticSineWave(double period, double phase) {
        super(period, phase);
    }

    public StaticSineWave(StaticWave cloneFrom) {
        super(cloneFrom);
    }


    @Override
    protected short calcOutput(List<SignalProvider> inputs) {
        double currentPosition = this.rounds / this.halfPeriod;

        if (this.sign ? currentPosition == 1.0 : currentPosition == -1.0) {
            return -1;
        }

        /*
        if (this.sign) {
            if (currentPosition == 0.0 || currentPosition == 2.0) return 0;
            else if (currentPosition == 1.0) return -1;
            else if (currentPosition == 0.5) return Short.MAX_VALUE;
            else if (currentPosition == 1.5) return Short.MIN_VALUE;

        } else {
            if (currentPosition == 0.0 || currentPosition == -2.0) return 0;
            else if (currentPosition == -1.0) return -1;
            else if (currentPosition == -0.5) return Short.MIN_VALUE;
            else if (currentPosition == -1.5) return Short.MAX_VALUE;
        }
         */

        short result = roundClip(Math.sin(currentPosition * PI) * MAX_PLUS_ONE - 0.5);
        return result;
    }

    @Override
    public StaticSineWave clone() {
        return new StaticSineWave(this);
    }

    @Override
    protected StaticSineWave cloneWith(double period, double phase) {
        return new StaticSineWave(period, phase);
    }
}
