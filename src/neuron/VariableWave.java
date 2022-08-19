package neuron;

import java.util.*;

import static neuron.StatelessFunction.roundClip;
import static neuron.StatelessMutatableFunction.*;

/**
 * TODO: Write a test which verifies that all subclass implementations, when given the same period input,
 * remain completely in-phase with each other over a very large number of periods.  Tests should include
 * both fixed and varying period input.
 *
 *
 * Forms a wave with a variable period (required) and variable phase-shift (optional, assumed 0 if not present).
 * While the phase-shift input impacts only the output of the current round, the period input determines the
 * output of the NEXT round, and therefore functions like a memory-neuron input.
 */
public abstract class VariableWave<N extends VariableWave<N>>
        extends CachingNeuron implements Mutatable<N> {

    public static final List<Param> MUTATION_PARAMS_BOTH_POS = List.of(Param.DEFAULT, Param.BOOLEAN_NEG, Param.DEFAULT, Param.BOOLEAN_NEG);
    public static final List<Param> MUTATION_PARAMS_MIN_NEG = List.of(Param.DEFAULT, Param.BOOLEAN, Param.DEFAULT, Param.BOOLEAN_NEG);
    public static final List<Param> MUTATION_PARAMS_MAX_NEG = List.of(Param.DEFAULT, Param.BOOLEAN_NEG, Param.DEFAULT, Param.BOOLEAN);
    public static final List<Param> MUTATION_PARAMS_BOTH_NEG = List.of(Param.DEFAULT, Param.BOOLEAN, Param.DEFAULT, Param.BOOLEAN);

    public final double periodMin;
    public final double periodMax;
    public final double periodRange;

    protected double nextPosition = 0;
    protected double lastPeriod;
    protected double currentPhaseShift;

    protected VariableWave(VariableWave cloneFrom) {
        super(cloneFrom);
        this.periodMin = cloneFrom.periodMin;
        this.periodMax = cloneFrom.periodMax;
        this.periodRange = cloneFrom.periodRange;
    }

    protected VariableWave(VariableWave cloneFrom, double periodMin, double periodMax)
            throws IllegalArgumentException {

        super(cloneFrom);

        if (!(Double.isFinite(periodMin) && Double.isFinite(periodMax))) throw new IllegalArgumentException();
        if (periodMin == 0 || periodMax == 0) throw new IllegalArgumentException();

        this.periodMin = periodMin;
        this.periodMax = periodMax;
        this.periodRange = periodMax - periodMin;
    }

    protected VariableWave(SignalProvider period, SignalProvider phase, double periodMin, double periodMax)
            throws IllegalArgumentException {

        this(List.of(period, phase), periodMin, periodMax);
    }

    protected VariableWave(List<SignalProvider> inputs, double periodMin, double periodMax)
            throws IllegalArgumentException {

        super(inputs);

        if (!(Double.isFinite(periodMin) && Double.isFinite(periodMax))) throw new IllegalArgumentException();
        if (periodMin == 0 || periodMax == 0) throw new IllegalArgumentException();

        this.periodMin = periodMin;
        this.periodMax = periodMax;
        this.periodRange = periodMax - periodMin;
    }

    @Override
    public int getMinInputs() {
        return 1;
    }

    @Override
    public int getMaxInputs() {
        return 2;
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
            return this.waveFunctionActual(this.nextPosition);
        }

        this.currentPhaseShift = (inputs.get(1).getOutput() / MAX_PLUS_ONE);
        double position = this.nextPosition + this.currentPhaseShift;

        if (position >= 2) position -= 2;
        else if (position < 0) position += 2;

        return this.waveFunctionActual(position);
    }

   /**
    * Default implementation takes the normalized double floating-point output from waveFunctionNormalized()
    * and transforms it across the range of the short integer for output.
    *
    * If a subclass prefers, it can override this implementation as an alternative to implementing the function
    * within waveFunctionNormalized, and implement waveFunctionNormalized as simply 'return 0;' since it
    * would never be invoked
    *
    * @param currentPosition Normalized phase position (including phase offset from phase neuron, if present).
    *                        currentPosition will always be >= 0.0 and < 2.0
    *
    * @return the actual neuron output as a short
    */
    protected short waveFunctionActual(double currentPosition) {
        return roundClip(this.waveFunctionNormalized(currentPosition) * MAX_PLUS_ONE - 0.5);
    }

    /**
     * 'Optional' abstract function, which can be implemented as just "return 0" if not being used.
     * The alternative, if not using this normalized function, is to override the default implementation of
     * waveFunctionActual to obtain a normalized currentPosition and return the actual output,
     * or override calcOutput entirely and use the actual this.nextPosition to return the actual output
     *
     * @param currentPosition Normalized phase position (including phase offset from phase neuron, if present).
     *                        currentPosition will always be >= 0.0 and < 2.0
     *
     * @return the normalized wave function output, >= -1.0 and <= 1.0
     */
    protected abstract double waveFunctionNormalized(double currentPosition);

    public void after() {
        this.getOutput(); // populates cache if it hasn't been already, before nextPosition is mutated

        double periodInput = this.getInputs().get(0).getOutput();

        this.lastPeriod = this.periodMin + this.periodRange * (periodInput + ZEROIZE) / RANGE;
        if (this.lastPeriod == 0) this.lastPeriod = findNearestNonZeroPeriod(periodInput);

        this.assignNextPosition(this.lastPeriod);
    }

    /**
     * Invoked during the after() function, to determine the next position.  By default
     * the nextPosition is normalized between 0.0 (inclusive) and 2.0 (exclusive), but
     * implementations may override this behavior.  It is upto the implementation to assign
     * its desired value to nextPosition
     *
     * @param period the period as calculated using the period neural input applied to the range
     *               of periods allowed for this VariableWaveNeuron
     */
    protected void assignNextPosition(double period) {
        this.nextPosition += 2 / period;

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

    protected double findNearestNonZeroPeriod(double periodInput) {
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
     * Behavior depends upon whether there is a phase neuron input.  Without a phase input,
     * this behaves like a memory neuron which allows circular references.  With a phase input,
     * it behaves as a normal neuron only with respect to the phase neuron.
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
     * Behavior depends upon whether there is a phase neuron input.  Without a phase input,
     * this behaves like a memory neuron which allows circular references.  With a phase input,
     * it behaves as a normal neuron ONLY with respect to the phase neuron.
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
     * Behavior depends upon whether there is a phase neuron input.  Without a phase input,
     * this behaves like a memory neuron which allows circular references.  With a phase input,
     * it behaves as a normal neuron only with respect to the phase neuron.
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
     * Behavior depends upon whether there is a phase neuron input.  Without a phase input,
     * this behaves like a memory neuron which allows circular references.  With a phase input,
     * it behaves as a normal neuron only with respect to the phase neuron.
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
    public List<Param> getMutationParams() {
        if (this.periodMin > 0)
            if (this.periodMax > 0) return MUTATION_PARAMS_BOTH_POS;
            else                    return MUTATION_PARAMS_MAX_NEG;

        else if (this.periodMax > 0) return MUTATION_PARAMS_MIN_NEG;
        else                         return MUTATION_PARAMS_BOTH_NEG;
    }

    @Override
    public N mutate(short[] params) {
        return this.cloneWith(
                        transformByMagnitudeAndSign(this.periodMin, params[0], params[1]),
                        transformByMagnitudeAndSign(this.periodMax, params[2], params[3]));
    }

    protected abstract N cloneWith(double periodMin, double periodMax);

    @Override
    public short[] getMutationParams(N toAchieve) {
        return toAchieveByMagnitudeAndSign(this.periodMin, toAchieve.periodMin, this.periodMax, toAchieve.periodMax);
    }
}
