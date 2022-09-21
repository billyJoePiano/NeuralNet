package neuralNet.neuron;

import neuralNet.network.*;
import neuralNet.test.*;
import neuralNet.util.*;

import java.io.*;
import java.lang.invoke.*;
import java.util.*;

import static neuralNet.evolve.Tweakable.*;
import static neuralNet.util.Util.*;

/**
 * Memories may have a delay period with no signal strength, and then may fade into full strength.
 * Once at full strength, the memory is retained forever (until reset is called) in an accumulated
 * average
 *
 */
public class LongTermMemoryNeuron extends MemoryNeuron<LongTermMemoryNeuron> {
    public static final long serialVersionUID = -5198894392784820039L;

    public final long lastTweaked;

    /**
     * Signal level to provide before enough memories have accumulated to produce a calculated signal
     */
    public final short defaultVal;

    /**
     * number of rounds (including the most recent round) before the current signal input
     * begins fading in.  It will not be included in any weighted calculations until then
     */
    public final int delay;
    private transient double delayDbl;

    /**
     * Number of rounds over which a memory starts to fade in to long-term memory.
     * Weight of signal is round / (fadeIn + 1)
     */
    public final int fadeIn;

    /**
     * the relative weight of ALL fadeIn values (after their weighted avg is calculated)
     * as compared to ONE full-strength memory
     */
    private transient double fadeInWeight;
    private transient double fadeInPlusOne;

    private transient AccumulatedAverage accumulated = new AccumulatedAverage();
    private transient double[] recent;
    private transient int index = 0;
    private transient int size = 0;
    private transient short nextOutput;
    private transient List<Param> tweakingParams;

    protected Object readResolve() throws ObjectStreamException {
        this.delayDbl = this.delay;
        this.fadeInWeight = (double) fadeIn / 2.0;
        this.fadeInPlusOne = this.fadeIn + 1;
        this.accumulated = new AccumulatedAverage();

        if (this.delay == 0 && this.fadeIn == 0) this.recent = null;
        else this.recent = new double[this.delay + this.fadeIn];

        return super.readResolve();
    }

    /*
    private Object readResolve() throws ObjectStreamException {
        return new LongTermMemoryNeuron(this, null);
    }

    private LongTermMemoryNeuron(LongTermMemoryNeuron deserializedFrom, Void v) {
        super(deserializedFrom, null);
        this.lastTweaked = deserializedFrom.lastTweaked;
        this.defaultVal = deserializedFrom.defaultVal;
        this.delay = deserializedFrom.delay;
        this.delayDbl = deserializedFrom.delayDbl;
        this.fadeIn = deserializedFrom.fadeIn;
        this.fadeInPlusOne = this.fadeIn + 1;
        this.fadeInWeight = (double)this.fadeIn / 2.0;

        if (delay == 0 && fadeIn == 0) this.recent = null;
        else this.recent = new double[delay + fadeIn];
    }
     */

    public LongTermMemoryNeuron(LongTermMemoryNeuron cloneFrom) {
        super(cloneFrom);
        this.lastTweaked = cloneFrom.lastTweaked;
        this.defaultVal = cloneFrom.defaultVal;
        this.delay = cloneFrom.delay;
        this.delayDbl = cloneFrom.delayDbl;
        this.fadeIn = cloneFrom.fadeIn;
        this.fadeInPlusOne = cloneFrom.fadeInPlusOne;
        this.fadeInWeight = cloneFrom.fadeInWeight;
        this.tweakingParams = cloneFrom.tweakingParams;

        if (delay == 0 && fadeIn == 0) this.recent = null;
        else this.recent = new double[delay + fadeIn];
    }

    public LongTermMemoryNeuron(LongTermMemoryNeuron cloneFrom, short defaultVal, int delay, int fadeIn, boolean forTrial)
            throws IllegalArgumentException {

        super(cloneFrom);

        if (delay < 0) throw new IllegalArgumentException("LongTermMemory delay must be 0 or greater");
        if (fadeIn < 0) throw new IllegalArgumentException("LongTermMemory fadeIn must be 0 or greater");

        this.lastTweaked = forTrial ? NeuralNet.getCurrentGeneration() : cloneFrom.lastTweaked;

        this.defaultVal = defaultVal;
        this.nextOutput = defaultVal;
        this.delay = delay;
        this.delayDbl = delay;
        this.fadeIn = fadeIn;
        this.fadeInPlusOne = fadeIn + 1;
        this.fadeInWeight = (double)fadeIn / 2.0;
        // fadeInWeight is the relative weight of ALL fadeIn values (after their weighted avg is calculated)
        // in comparison to ONE full-strength memory
        // NOTE: x * (x + 1) / 2 is the formula for the sum of all integers between 1 and x (inclusive)
        // To arrive at the fadeInWeight, divide this by (x + 1), which algebraically simplifies to x / 2

        if (delay == 0 && fadeIn == 0) this.recent = null;
        else this.recent = new double[delay + fadeIn];
    }

    public LongTermMemoryNeuron(List<SignalProvider> input, int delayAndFadeIn)
            throws IllegalArgumentException {

        this(input, (short)0, delayAndFadeIn, delayAndFadeIn);
    }

    public LongTermMemoryNeuron(short defaultVal, int delay, int fadeIn)
            throws IllegalArgumentException {

        super();

        if (delay < 0) throw new IllegalArgumentException("LongTermMemory delay must be 0 or greater");
        if (fadeIn < 0) throw new IllegalArgumentException("LongTermMemory fadeIn must be 0 or greater");

        this.lastTweaked = -1;
        this.defaultVal = defaultVal;
        this.nextOutput = defaultVal;
        this.delay = delay;
        this.delayDbl = delay;
        this.fadeIn = fadeIn;
        this.fadeInPlusOne = fadeIn + 1;
        this.fadeInWeight = (double)fadeIn / 2.0;

        if (delay == 0 && fadeIn == 0) this.recent = null;
        else this.recent = new double[delay + fadeIn];
    }

    public LongTermMemoryNeuron(List<SignalProvider> inputs, short defaultVal, int delay, int fadeIn)
            throws IllegalArgumentException {

        super(inputs);

        if (delay < 0) throw new IllegalArgumentException("LongTermMemory delay must be 0 or greater");
        if (fadeIn < 0) throw new IllegalArgumentException("LongTermMemory fadeIn must be 0 or greater");

        this.lastTweaked = -1;
        this.defaultVal = defaultVal;
        this.nextOutput = defaultVal;
        this.delay = delay;
        this.delayDbl = delay;
        this.fadeIn = fadeIn;
        this.fadeInPlusOne = fadeIn + 1;
        this.fadeInWeight = (double)fadeIn / 2.0;

        if (delay == 0 && fadeIn == 0) this.recent = null;
        else this.recent = new double[delay + fadeIn];
    }


    @Override
    public int getMinInputs() {
        return 1;
    }

    @Override
    public int getMaxInputs() {
        return 1;
    }

    protected short calcOutput(List<SignalProvider> inputs) {
        return this.nextOutput;
    }

    @Override
    public boolean sameBehavior(SignalProvider other) {
        if (other == this) return true;
        if (other == null) return false;
        if (!(other instanceof LongTermMemoryNeuron o)) return false;
        return this.defaultVal == o.defaultVal && this.delay == o.delay && this.fadeIn == o.fadeIn;
    }

    public void after() {
        this.getOutput();
        // ensures that the output cache in CachingNeuron is populated with the CURRENT value from this.nextOutput
        // BEFORE this.nextOutput is re-assigned below.  This is done in case there is another memory neuralNet.neuron which
        // relies upon this one and hasn't had its after() method invoked yet, or in case of an un-invoked
        // circular loop where this neuralNet.neuron's input chain relies upon its own output

        List<SignalProvider> inputs = this.getInputs();

        if (this.recent == null) {
            // no delay and no fade in... skip right to adding to the accumulated store
            this.nextOutput = roundClip(this.accumulated.addAndGetAverage(inputs.get(0).getOutput()));

        } else if (this.size == this.recent.length) {
            double avg = this.accumulated.addAndGetAverage(this.recent[this.index]);
            // move the oldest value into the accumulated array...

            this.recent[this.index] = inputs.get(0).getOutput(); // ...replace with the newest value

            if (this.fadeIn == 0) {
                if (++this.index == this.recent.length) this.index = 0;
                this.nextOutput = roundClip(avg);

            } else {
                //the main execution path once long-term memory is established (except when this.recent == null, aka no delay or fadeIn)
                long size = this.accumulated.size();
                this.nextOutput = roundClip((processRecent() + avg * size) / (this.fadeInWeight + size));
            }

        } else {
            // nothing to add to accumulated yet
            this.size++;
            try {
                this.recent[this.index] = inputs.get(0).getOutput();

            } catch (ArrayIndexOutOfBoundsException e) {
                TestUtil.compareObjects(this, this);
                throw e;
            }

            if (this.index < this.delay || this.fadeIn == 0) {
                // nothing to calculate through fade in.  Increment index and return defaultVal
                if (++this.index == this.recent.length) this.index = 0;
                this.nextOutput = this.defaultVal;

            } else {
                this.nextOutput = processRecentOnly();
            }
        }
    }

    /**
     * Should only be invoked when it is known that the recent memories array is still being populated
     * AND there are values within the fadeIn range to be processed.
     *
     * The recent memories array will not be full except in the case of the last index.
     *
     * @return the signal strength to return to the getOutput function via calcOutput
     */
    private short processRecentOnly() {
        double sum = 0;

        int i;

        if (this.delay == 0) {
            i = this.index;

        } else {
            i = this.index - this.delay;
        }

        double weightSum = ((i + 1) * (i + 2)) / 2;
        // i is zero-indexed, therefore we must use i + 1 as "i" and i + 2 as "i + 1"

        while (i != -1) {
            sum += this.recent[i] * i;
            i--;
        }

        if (++this.index == this.recent.length) this.index = 0;

        return roundClip(sum / weightSum);
    }


    /**
     * Should only be invoked when it is known that the recent memories array is full,
     * and fadeIn > 0
     * @return the sum of all weighted recent memories within the fadeIn range.  Division
     * by the weightSum has NOT been done, because this will be done by calcOutput after adding
     * the addToStore value, when it submits the fadeInAvg argument for processAccumulated()
     */
    private double processRecent() {
        double sum = 0;

        int i;
        int fadeIn = 0;

        if (this.delay == 0) {
            i = this.index;

        } else {
            i = this.index - this.delay;
            if (i < 0) i += this.recent.length;
        }

        while (fadeIn != this.fadeIn) {
            fadeIn++;
            //double weight = fadeIn / this.fadeInPlusOneDbl;

            sum += this.recent[i] * fadeIn; // * weight;

            if (i != 0) i--;
            else i = this.recent.length - 1;
        }

        if (++this.index == this.recent.length) this.index = 0;

        return sum / (double)(this.fadeIn + 1);
        // rather than divide by fadeInWeight only to re-multiply by it again, we just return this...
        // algebraically simplified from:
        // ( sum  /  ((fadeIn + 1) * fadeIn / 2)  )   *     fadeIn / 2
        //           ^^ formula for weightSum ^^                  ^^^ fadeInWeight

        //  ...simplifies to just sum / (fadeIn + 1)
    }

    @Override
    public void reset() {
        this.nextOutput = this.defaultVal;
        this.size = 0;
        this.index = 0;
        this.accumulated.clear();
        super.reset();
    }

    @Override
    public Set<SignalProvider> traceProviders() {
        return null;
    }

    @Override
    public void traceProviders(Set<SignalProvider> providers) { }

    @Override
    public Set<SignalConsumer> traceConsumers() {
        return null;
    }

    @Override
    public void traceConsumers(Set<SignalConsumer> consumers) { }

    @Override
    public LongTermMemoryNeuron clone() {
        return new LongTermMemoryNeuron(this);
    }

    @Override
    public List<Param> getTweakingParams() {
        if (this.tweakingParams != null) return this.tweakingParams;

        short[] minParams = toAchieveByMagnitudeOnly(this.delay + 1, 1, this.fadeIn + 1, 1);
        //always offset delay and fadeIn by 1 so 0 can be reached through a logarithmic function
        // achieving "1" means actually achieving delay or fadeIn of 0


        return this.tweakingParams = List.of(new Param(this.defaultVal),
                new Param(minParams[0], Short.MAX_VALUE),
                new Param(minParams[1], Short.MAX_VALUE));
    }

    @Override
    public Long getLastTweakedGeneration() {
        return this.lastTweaked == -1 ? null : this.lastTweaked;
    }

    @Override
    public LongTermMemoryNeuron tweak(short[] params, boolean forTrial) {
        //always offset delay and fadeIn by 1 so 0 can be reached through a logarithmic function
        int delay = (int)Math.round(transformByMagnitudeOnly(this.delay + 1, params[1])) - 1;
        int fadeIn = (int)Math.round(transformByMagnitudeOnly(this.fadeIn + 1, params[2])) - 1;

        if (delay < 0) delay = 0; //possible -1 due to rounding when making the original params minimum
        if (fadeIn < 0) fadeIn = 0;

        return new LongTermMemoryNeuron(this, (short)(this.defaultVal + params[0]), delay, fadeIn, forTrial);
    }

    @Override
    public short[] getTweakingParams(LongTermMemoryNeuron toAchieve) {
        //always offset delay and fadeIn by 1 so 0 can be reached through a logarithmic function
        short[] params = toAchieveByMagnitudeOnly(new short[3], this.delay + 1, toAchieve.delay + 1,
                                                                        this.fadeIn + 1, toAchieve.fadeIn + 1);
        params[2] = params[1];
        params[1] = params[0];
        params[0] = clip((int)toAchieve.defaultVal - (int)this.defaultVal);

        return params;
    }

    public String toString() {
        return "LongTermMemory(" + this.defaultVal + ", " + this.delay + ", " + this.fadeIn + ")";
    }

    public static final long HASH_HEADER = NeuralHash.HEADERS.get(MethodHandles.lookup().lookupClass());

    @Override
    protected long calcNeuralHashFor(LoopingNeuron looper) {
        return HASH_HEADER ^ Long.rotateRight(this.inputs.get(0).getNeuralHashFor(looper), 17)
                ^ Long.rotateLeft(this.defaultVal & 0xffff, 51)
                ^ Long.rotateLeft(this.delay, 37) ^ Long.rotateLeft(this.fadeIn, 23);
    }
}
