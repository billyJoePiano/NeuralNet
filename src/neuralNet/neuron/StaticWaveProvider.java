package neuralNet.neuron;

import neuralNet.function.*;
import neuralNet.network.*;

import java.util.*;

import static neuralNet.evolve.Tweakable.*;
import static neuralNet.util.Util.*;

/**
 * Forms a sine-wave with given period expressed as a coefficient of pi
 * (aka, where 2.0 represents a full sine-wave cycle) and a phase-shift
 * also expressed as coefficient of pi
 */
public class StaticWaveProvider extends CachingProvider implements SignalProvider.Tweakable<StaticWaveProvider> {
    public static final List<WaveFunction> WAVE_FUNCTIONS =
            List.of(SineWave.INSTANCE, TriangleWave.INSTANCE, SawWave.INSTANCE, SquareWave.INSTANCE);

    public static final List<Param> WAVE_FUNCTION_PARAMS =
            List.of(new Param(0, 3), new Param(-1, 2), new Param(-2, 1), new Param(-3, 0));

    public static final List<List<Param>> MUTATION_PARAMS_POS = List.of(
            List.of(Param.DEFAULT, Param.BOOLEAN_NEG, Param.CIRCULAR, SineWave.INSTANCE.getMutationParam()),
            List.of(Param.DEFAULT, Param.BOOLEAN_NEG, Param.CIRCULAR, TriangleWave.INSTANCE.getMutationParam()),
            List.of(Param.DEFAULT, Param.BOOLEAN_NEG, Param.CIRCULAR, SawWave.INSTANCE.getMutationParam()),
            List.of(Param.DEFAULT, Param.BOOLEAN_NEG, Param.CIRCULAR, SquareWave.INSTANCE.getMutationParam()));

    public static final List<List<Param>> MUTATION_PARAMS_NEG = List.of(
            List.of(Param.DEFAULT, Param.BOOLEAN, Param.CIRCULAR, SineWave.INSTANCE.getMutationParam()),
            List.of(Param.DEFAULT, Param.BOOLEAN, Param.CIRCULAR, TriangleWave.INSTANCE.getMutationParam()),
            List.of(Param.DEFAULT, Param.BOOLEAN, Param.CIRCULAR, SawWave.INSTANCE.getMutationParam()),
            List.of(Param.DEFAULT, Param.BOOLEAN, Param.CIRCULAR, SquareWave.INSTANCE.getMutationParam()));


    public final long lastTweaked;

    public final WaveFunction waveFunction;

    public final boolean sign;
    public final double phase;
    public final double period;

    public final double incrementPerRound;
    public final boolean increment2orMore; // when the magnitude (abs) of the increment is >= 2.0

    private transient double currentPhase;

    public StaticWaveProvider(WaveFunction waveFunction, double period, double phase) {
        super();

        if (waveFunction == null || period == 0 || !(Double.isFinite(period) && Double.isFinite(phase))) {
            throw new IllegalArgumentException();
        }

        this.lastTweaked = -1; //marker for 'null'
        this.waveFunction = waveFunction;
        this.period = period;
        this.sign = period > 0;
        this.incrementPerRound = 2 / period;
        this.increment2orMore = this.sign ? this.incrementPerRound >= 2.0 : this.incrementPerRound <= -2.0;

        phase %= 2.0;
        if (phase >= 0.0) this.phase = phase;
        else this.phase = phase + 2.0;

        this.currentPhase = this.phase;
    }

    public StaticWaveProvider(StaticWaveProvider cloneFrom, WaveFunction waveFunction, double period, double phase, boolean forTrial) {
        super(cloneFrom);

        if (waveFunction == null || period == 0 || !(Double.isFinite(period) && Double.isFinite(phase))) {
            throw new IllegalArgumentException();
        }

        if (forTrial) this.lastTweaked = NeuralNet.getCurrentGeneration();
        else this.lastTweaked = cloneFrom.lastTweaked;

        this.waveFunction = waveFunction;
        this.period = period;
        this.sign = period > 0;
        this.incrementPerRound = 2 / period;
        this.increment2orMore = this.sign ? this.incrementPerRound >= 2.0 : this.incrementPerRound <= -2.0;

        phase %= 2.0;
        if (phase >= 0.0) this.phase = phase;
        else this.phase = phase + 2.0;

        this.currentPhase = this.phase;
    }

    public StaticWaveProvider(StaticWaveProvider cloneFrom) {
        super();
        this.lastTweaked = cloneFrom.lastTweaked;
        this.waveFunction = cloneFrom.waveFunction;
        this.sign = cloneFrom.sign;
        this.period = cloneFrom.period;
        this.phase = cloneFrom.phase;
        this.incrementPerRound = cloneFrom.incrementPerRound;
        this.increment2orMore = cloneFrom.increment2orMore;
        this.currentPhase = this.phase;
    }

    public StaticWaveProvider(StaticWaveProvider cloneFrom, WaveFunction waveFunction) {
        super(cloneFrom);
        if (waveFunction == null) throw new IllegalArgumentException();

        this.lastTweaked = NeuralNet.getCurrentGeneration();

        this.waveFunction = waveFunction;
        this.sign = cloneFrom.sign;
        this.period = cloneFrom.period;
        this.phase = cloneFrom.phase;
        this.incrementPerRound = cloneFrom.incrementPerRound;
        this.increment2orMore = cloneFrom.increment2orMore;
        this.currentPhase = this.phase;
    }

    public StaticWaveProvider(StaticWaveProvider cloneFrom, double period, double phase) {
        this(cloneFrom, cloneFrom.waveFunction, period, phase, false);
    }


    public void after() {
        this.getOutput(); // populates cache if it hasn't been already, before this.rounds is mutated

        this.currentPhase += this.incrementPerRound;

        if (this.sign) {
            if (this.increment2orMore) {
                this.currentPhase %= 2.0;

            } else if (this.currentPhase >= 2.0) {
                this.currentPhase -= 2.0;
            }

        } else {
            if (this.increment2orMore) {
                this.currentPhase %= 2.0;
                if (this.currentPhase < 0.0) this.currentPhase += 2.0;

            } else if (this.currentPhase <= 2.0) {
                this.currentPhase += 2.0;
            }
        }
    }

    @Override
    protected short calcOutput() {
        return roundClip(this.waveFunction.calc(this.currentPhase) * MAX_PLUS_ONE - 0.5);
    }

    @Override
    public void reset() {
        this.currentPhase = this.phase;
        super.reset();
    }

    @Override
    public StaticWaveProvider clone() {
        return new StaticWaveProvider(this);
    }

    @Override
    public List<Param> getTweakingParams() {
        if (this.sign) return MUTATION_PARAMS_POS.get(this.waveFunction.getIndex());
        else return MUTATION_PARAMS_NEG.get(this.waveFunction.getIndex());
    }

    @Override
    public Long getLastTweakedGeneration() {
        return this.lastTweaked == -1 ? null : this.lastTweaked;
    }

    @Override
    public StaticWaveProvider tweak(short[] params, boolean forTrial) {
        double period = transformByMagnitudeAndSign(this.period, params[0], params[1]);
        double phase = this.phase + (double)params[2] / MAX_PLUS_ONE;

        return new StaticWaveProvider(this, this.waveFunction.mutate(params[3]), period, phase, forTrial);
    }

    @Override
    public short[] getTweakingParams(StaticWaveProvider toAchieve) {
        short[] params = toAchieveByMagnitudeAndSign(new short[4], this.period, toAchieve.period);

        params[2] = (short)Math.round((toAchieve.phase - this.phase) * MAX_PLUS_ONE);
        params[3] = this.waveFunction.getMutationParam(toAchieve.waveFunction);

        return params;
    }

    public String toString() {
        return "Static" + this.waveFunction.getClass().getSimpleName();
    }
}
