package neuralNet.neuron;

import neuralNet.function.*;
import neuralNet.network.*;

import java.io.*;
import java.lang.invoke.*;
import java.util.*;

import static neuralNet.evolve.Tweakable.*;
import static neuralNet.neuron.StaticWaveProvider.*;
import static neuralNet.util.Util.*;

/**
 *
 * Forms a wave with a variable period (required) and variable phase-shift (optional, assumed 0 if not present).
 *
 * Unlike StaticWaveNeuron, the position in the phase is calculated during the calcOutput(inputs) method
 * rather than in after(), because the neuron
 */
public class VariableWaveNeuron extends CachingNeuron implements SignalProvider.Tweakable<VariableWaveNeuron> {
    public static final long serialVersionUID = -3247512747152016341L;

    public static final List<List<Param>> MUTATION_PARAMS_BOTH_POS = makeParams(true, true);
    public static final List<List<Param>> MUTATION_PARAMS_MIN_NEG = makeParams(false, true);
    public static final List<List<Param>> MUTATION_PARAMS_MAX_NEG = makeParams(true, false);
    public static final List<List<Param>> MUTATION_PARAMS_BOTH_NEG = makeParams(false, false);

    private static List<List<Param>> makeParams(boolean minSign, boolean maxSign) {
        Param min = minSign ? Param.BOOLEAN_NEG : Param.BOOLEAN;
        Param max = maxSign ? Param.BOOLEAN_NEG : Param.BOOLEAN;

        List params = new ArrayList(WAVE_FUNCTIONS.size());
        for (WaveFunction func : WAVE_FUNCTIONS) {
            params.add(List.of(Param.DEFAULT, min, Param.DEFAULT, max, func.getMutationParam()));
        }
        return (List<List<Param>>)Collections.unmodifiableList(params);
    }

    public final long lastTweaked;

    public final WaveFunction waveFunction;

    public final double periodMin;
    public final double periodMax;
    private transient double periodRange;

    private transient double nextPosition = 0;
    private transient double lastPeriod;
    private transient double currentPhaseShift;

    @Override
    protected Object readResolve() throws ObjectStreamException {
        this.periodRange = this.periodMax - this.periodMin;
        return super.readResolve();
    }

    /*
    private Object readResolve() throws ObjectStreamException {
        return new VariableWaveNeuron(this, null);
    }

    public VariableWaveNeuron(VariableWaveNeuron deserializedFrom, Void v) {
        super(deserializedFrom, null);
        this.waveFunction = deserializedFrom.waveFunction;
        this.lastTweaked = deserializedFrom.lastTweaked;
        this.periodMin = deserializedFrom.periodMin;
        this.periodMax = deserializedFrom.periodMax;
        this.periodRange = periodMax - periodMin;
    }
     */

    public VariableWaveNeuron(SignalProvider period, WaveFunction waveFunction,
                              double periodMin, double periodMax)
            throws IllegalArgumentException {

        this(List.of(period), waveFunction, periodMin, periodMax);
    }

    public VariableWaveNeuron(SignalProvider period, SignalProvider phase,
                              WaveFunction waveFunction, double periodMin, double periodMax)
            throws IllegalArgumentException {

        this(List.of(period, phase), waveFunction, periodMin, periodMax);
    }

    public VariableWaveNeuron(List<SignalProvider> inputs, WaveFunction waveFunction, double periodMin, double periodMax)
            throws IllegalArgumentException {

        super(inputs);

        if (waveFunction == null || periodMin == 0 || periodMax == 0
                || !(Double.isFinite(periodMin) && Double.isFinite(periodMax))) {

            throw new IllegalArgumentException();
        }

        this.lastTweaked = -1;
        this.waveFunction = waveFunction;
        this.periodMin = periodMin;
        this.periodMax = periodMax;
        this.periodRange = periodMax - periodMin;
    }

    public VariableWaveNeuron(WaveFunction waveFunction, double periodMin, double periodMax)
            throws IllegalArgumentException {
    
        super();
        
        if (waveFunction == null || periodMin == 0 || periodMax == 0
                || !(Double.isFinite(periodMin) && Double.isFinite(periodMax))) {

            throw new IllegalArgumentException();
        }

        this.lastTweaked = -1;
        this.waveFunction = waveFunction;
        this.periodMin = periodMin;
        this.periodMax = periodMax;
        this.periodRange = periodMax - periodMin;
    }
    
    public VariableWaveNeuron(VariableWaveNeuron cloneFrom, WaveFunction waveFunction,
                              double periodMin, double periodMax, boolean forTrial) {
        
        super(cloneFrom);

        if (waveFunction == null || periodMin == 0 || periodMax == 0
                || !(Double.isFinite(periodMin) && Double.isFinite(periodMax))) {

            throw new IllegalArgumentException();
        }

        this.lastTweaked = forTrial ? NeuralNet.getCurrentGeneration() : cloneFrom.lastTweaked;
        this.waveFunction = waveFunction;
        this.periodMin = periodMin;
        this.periodMax = periodMax;
        this.periodRange = periodMax - periodMin;
    }

    public VariableWaveNeuron(VariableWaveNeuron cloneFrom) {
        super(cloneFrom);
        this.lastTweaked = cloneFrom.lastTweaked;
        this.waveFunction = cloneFrom.waveFunction;
        this.periodMin = cloneFrom.periodMin;
        this.periodMax = cloneFrom.periodMax;
        this.periodRange = cloneFrom.periodRange;
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
    public boolean inputOrderMatters() {
        return true;
    }

    public double getLastPeriod() {
        return this.lastPeriod;
    }

    public double getCurrentPhaseShift() {
        this.getOutput(); //ensure this.currentPhaseShift is updated
        return this.currentPhaseShift;
    }

    public double getCurrentPhasePosition() {
        this.getOutput(); //ensure this.currentPhaseShift is updated
        double position = this.nextPosition + this.currentPhaseShift;

        if (position >= 2.0) return position - 2.0;
        else if (position < 0.0) return position + 2.0;
        else return position;
    }

    public double getUnshiftedCurrentPhasePosition() {
        return this.nextPosition;
    }

    @Override
    protected short calcOutput(List<SignalProvider> inputs) {
        double periodInput = this.getInputs().get(0).getOutput();

        this.lastPeriod = this.periodMin + this.periodRange * (periodInput + ZEROIZE) / RANGE;
        if (this.lastPeriod == 0) this.lastPeriod = findNearestNonZeroPeriod(periodInput);


        this.nextPosition += 2 / this.lastPeriod;

        if (this.nextPosition < 0) {
            if (this.nextPosition < -2.0) {
                this.nextPosition %= 2;
            }

            while (this.nextPosition < 0) this.nextPosition += 2;
        }

        if (this.nextPosition >= 2.0) {
            this.nextPosition %= 2;
        }


        if (inputs.size() == 1) {
            return roundClip(this.waveFunction.calc(this.nextPosition) * MAX_PLUS_ONE - 0.5);
        }

        this.currentPhaseShift = (inputs.get(1).getOutput() / MAX_PLUS_ONE);
        double position = this.nextPosition + this.currentPhaseShift;

        if (position >= 2) position -= 2;
        else if (position < 0) position += 2;

        return roundClip(this.waveFunction.calc(position) * MAX_PLUS_ONE - 0.5);
    }

    @Override
    public boolean sameBehavior(SignalProvider other) {
        if (other == this) return true;
        if (!(other instanceof VariableWaveNeuron o)) return false;
        return this.waveFunction == o.waveFunction && this.periodMin == o.periodMin && this.periodMax == o.periodMax;
    }

    public void after() {
        this.getOutput(); // populates cache if it hasn't been already, to ensure the round is processed
    }
    

    private double findNearestNonZeroPeriod(double periodInput) {
        //rare cases where min/max cross zero, and period ends up at zero...
        // find the closest non-zero value
        double period;
        int diff = 1;
        do {
            // try adding 'diff' to the periodInput first
            period = this.periodMin + this.periodRange * (periodInput + diff + ZEROIZE) / RANGE;
            if (period != 0) return period;

            // if that doesn't work, try subtracting 'diff' instead
            period = this.periodMin + this.periodRange * (periodInput - diff + ZEROIZE) / RANGE;
            if (period != 0) return period;

            // if that doesn't work, increment 'diff' and repeat in reverse order (subtract first, then add)
            diff++;

            period = this.periodMin + this.periodRange * (periodInput - diff + ZEROIZE) / RANGE;
            if (period != 0) return period;

            period = this.periodMin + this.periodRange * (periodInput + diff + ZEROIZE) / RANGE;
            if (period != 0) return period;

            diff++;

        } while (true); //should be extremely rare to need this, but just in case...
    }

    @Override
    public void reset() {
        this.nextPosition = 0;
        this.lastPeriod = 0;
        super.reset();
    }


    @Override
    public List<Param> getTweakingParams() {
        if (this.periodMin > 0)
            if (this.periodMax > 0) return MUTATION_PARAMS_BOTH_POS.get(this.waveFunction.getIndex());
            else                    return MUTATION_PARAMS_MAX_NEG.get(this.waveFunction.getIndex());

        else if (this.periodMax > 0) return MUTATION_PARAMS_MIN_NEG.get(this.waveFunction.getIndex());
        else                         return MUTATION_PARAMS_BOTH_NEG.get(this.waveFunction.getIndex());
    }

    @Override
    public Long getLastTweakedGeneration() {
        return this.lastTweaked == -1 ? null : this.lastTweaked;
    }

    @Override
    public VariableWaveNeuron tweak(short[] params, boolean forTrial) {
        double periodMin = transformByMagnitudeAndSign(this.periodMin, params[0], params[1]);
        double periodMax = transformByMagnitudeAndSign(this.periodMax, params[2], params[3]);

        return new VariableWaveNeuron(this, this.waveFunction.mutate(params[4]), periodMin, periodMax, forTrial);
    }

    @Override
    public short[] getTweakingParams(VariableWaveNeuron toAchieve) {
        short[] params = toAchieveByMagnitudeAndSign(new short[5], this.periodMin, toAchieve.periodMin, this.periodMax, toAchieve.periodMax);
        params[4] = this.waveFunction.getMutationParam(toAchieve.waveFunction);

        return params;
    }

    @Override
    public VariableWaveNeuron clone() {
        return new VariableWaveNeuron(this);
    }

    public String toString() {
        return "Variable" + this.waveFunction.getClass().getSimpleName();
    }

    public static final long HASH_HEADER = NeuralHash.HEADERS.get(MethodHandles.lookup().lookupClass());

    @Override
    public long calcNeuralHashFor(LoopingNeuron looper) {
        long hash = HASH_HEADER ^ Long.rotateRight(this.inputs.get(0).getNeuralHashFor(looper), 17)
                ^ Long.rotateLeft(Double.doubleToLongBits(this.periodMin), 13)
                ^ Long.rotateLeft(Double.doubleToLongBits(this.periodMax), 17);

        if (this.inputs.size() == 1) return hash;
        return hash ^ Long.rotateRight(this.inputs.get(1).getNeuralHashFor(looper), 34); //17 * 2
    }
}