package neuralNet.neuron;

import neuralNet.function.*;
import neuralNet.network.*;

import java.util.*;

import static neuralNet.function.Tweakable.*;
import static neuralNet.neuron.StaticWaveProvider.*;
import static neuralNet.util.Util.*;

/**
 * TODO: Write a neuralNet.test which verifies that all subclass implementations, when given the same period input,
 * remain completely in-phase with each other over a very large number of periods.  Tests should include
 * both fixed and varying period input.
 *
 *
 * Forms a wave with a variable period (required) and variable phase-shift (optional, assumed 0 if not present).
 * While the phase-shift input impacts only the output of the current round, the period input determines the
 * output of the NEXT round, and therefore functions like a memory-neuralNet.neuron input.
 */
public class VariableWaveNeuron extends CachingNeuron implements SignalProvider.Tweakable<VariableWaveNeuron> {

    public static final List<List<Param>> MUTATION_PARAMS_BOTH_POS = makeParams(true, true);
    public static final List<List<Param>> MUTATION_PARAMS_MIN_NEG = makeParams(false, true);
    public static final List<List<Param>> MUTATION_PARAMS_MAX_NEG = makeParams(true, false);
    public static final List<List<Param>> MUTATION_PARAMS_BOTH_NEG = makeParams(false, false);

    private static List<List<Param>> makeParams(boolean minSign, boolean maxSign) {
        Param min = minSign ? Param.BOOLEAN_NEG : Param.BOOLEAN;
        Param max = maxSign ? Param.BOOLEAN_NEG : Param.BOOLEAN;

        List params = new ArrayList(WAVE_FUNCTIONS.size());
        for (WaveFunction func : WAVE_FUNCTIONS) {
            params.add(List.of(Param.DEFAULT, minSign, Param.DEFAULT, maxSign, func.getMutationParam()));
        }
        return (List<List<Param>>)Collections.unmodifiableList(params);
    }

    public final long lastTweaked;

    public final WaveFunction waveFunction;

    public final double periodMin;
    public final double periodMax;
    public final double periodRange;

    private double nextPosition = 0;
    private double lastPeriod;
    private double currentPhaseShift;

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
        if (inputs.size() == 1) {
            return roundClip(this.waveFunction.calc(this.nextPosition) * MAX_PLUS_ONE - 0.5);
        }

        this.currentPhaseShift = (inputs.get(1).getOutput() / MAX_PLUS_ONE);
        double position = this.nextPosition + this.currentPhaseShift;

        if (position >= 2) position -= 2;
        else if (position < 0) position += 2;

        return roundClip(this.waveFunction.calc(position) * MAX_PLUS_ONE - 0.5);
    }

    public void after() {
        this.getOutput(); // populates cache if it hasn't been already, before nextPosition is mutated

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

    /**
     * Behavior depends upon whether there is a phase neuralNet.neuron input.  Without a phase input,
     * this behaves like a memory neuralNet.neuron which allows circular references.  With a phase input,
     * it behaves as a normal neuralNet.neuron only with respect to the phase neuralNet.neuron.
     *
     * @return
     */
    public Set<SignalConsumer> traceConsumers() {
        if (this.getInputs().size() <= 1) return null;

        Set<SignalConsumer> consumers = new HashSet<>();
        super.traceConsumers(consumers);
        return consumers;
    }

    /**
     * Behavior depends upon whether there is a phase neuralNet.neuron input.  Without a phase input,
     * this behaves like a memory neuralNet.neuron which allows circular references.  With a phase input,
     * it behaves as a normal neuralNet.neuron ONLY with respect to the phase neuralNet.neuron.
     *
     * @return
     */
    public void traceConsumers(Set<SignalConsumer> addToExistingSet) {
        List<SignalProvider> inputs = this.getInputs();
        if (inputs.size() <= 1) return;

        SignalProvider phaseNeuron = inputs.get(1);

        if (addToExistingSet.contains(phaseNeuron)) {
            super.traceConsumers(addToExistingSet);
        }
    }


    /**
     * Behavior depends upon whether there is a phase neuralNet.neuron input.  Without a phase input,
     * this behaves like a memory neuralNet.neuron which allows circular references.  With a phase input,
     * it behaves as a normal neuralNet.neuron only with respect to the phase neuralNet.neuron.
     *
     * @return
     */
    public Set<SignalProvider> traceProviders() {
        List<SignalProvider> inputs = this.getInputs();
        if (inputs.size() <= 1) return null;

        SignalProvider phaseNeuron = inputs.get(1);

        if (phaseNeuron instanceof Neuron) {
            Set<SignalProvider> providers = new HashSet<>();
            providers.add(this);
            ((Neuron)phaseNeuron).traceProviders(providers);
            return providers;

        } else {
            return null;
        }
    }

    /**
     * Behavior depends upon whether there is a phase neuralNet.neuron input.  Without a phase input,
     * this behaves like a memory neuralNet.neuron which allows circular references.  With a phase input,
     * it behaves as a normal neuralNet.neuron only with respect to the phase neuralNet.neuron.
     *
     * @return
     */
    public void traceProviders(Set<SignalProvider> addToExistingSet) {
        List<SignalProvider> inputs = this.getInputs();
        if (inputs.size() <= 1) return;

        if (addToExistingSet.contains(this)) return;
        addToExistingSet.add(this);

        SignalProvider phaseNeuron = inputs.get(1);
        if (phaseNeuron instanceof Neuron) {
            ((Neuron)phaseNeuron).traceProviders(addToExistingSet);
        }
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
}