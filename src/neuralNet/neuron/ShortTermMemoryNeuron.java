package neuralNet.neuron;

import neuralNet.network.*;

import java.io.*;
import java.lang.invoke.*;
import java.util.*;

import static neuralNet.evolve.Tweakable.*;
import static neuralNet.util.Util.*;

/**
 * Memories taper into full signal weight over the course of the initial delay number of rounds.
 * After traversing the length number of round at full weight, they fade out of memory over the
 * fade number of rounds.
 *
 * Note that delay, fadeIn, and fadeOut can all be set to zero.  Length must be at least one
 *
 */
public class ShortTermMemoryNeuron extends MemoryNeuron<ShortTermMemoryNeuron> {
    public static final long serialVersionUID = 2902116119404283037L;

    public final long lastTweaked;
    private transient List<Param> tweakingParams;

    public final short defaultVal;
    public final int delay;

    /**
     * number of rounds (including the most recent round) after delay rounds, before a memory
     * reaches peak weight.  When fadeIn > 0, the fadeIn rounds receive a weight of
     * round / (fadeIn + 1) until they reach peak.
     *
     * If delay == 0 and fadeIn == 0, then the current round's signal is given full weight
     */
    public final int fadeIn;
    //public final double fadeInPlusOneDbl;


    /**
     * number of full-weight memories to include in the calculation
     */
    public final int length;;

    /**
     * Number of rounds (including the most recent round) to fade the memories as they are forgotten,
     * after they leave the full-weight length.
     * Weight is proportional to rounds / (fade + 1) where 'rounds' includes the current round
     */
    public final int fadeOut;

    private final transient double[] memory;
    private transient int index = 0;
    private transient int size = 0;

    private transient short nextOutput;

    private Object readResolve() throws ObjectStreamException {
        return new ShortTermMemoryNeuron(this, null);
    }

    private ShortTermMemoryNeuron(ShortTermMemoryNeuron deserializedFrom, Void v) {
        super(deserializedFrom, null);
        this.lastTweaked = deserializedFrom.lastTweaked;
        this.defaultVal = deserializedFrom.defaultVal;
        this.delay = deserializedFrom.delay;
        this.fadeIn = deserializedFrom.fadeIn;
        this.length = deserializedFrom.length;
        this.fadeOut = deserializedFrom.fadeOut;
        this.memory = new double[delay + fadeIn + length + fadeOut];
    }

    /**
     * Behaves as an immediate short-term memory ... the most recent round (previous round) is at full weight for
     * one round and then fades
     *
     */
    public ShortTermMemoryNeuron(SignalProvider input, int fadeOutOnly)
            throws IllegalArgumentException {

        this(input, (short)0, 0, 0, 1, fadeOutOnly);
    }

    public ShortTermMemoryNeuron(short defaultVal, int delay, int fadeIn, int length, int fadeOut) {
        super();

        if (delay < 0) throw new IllegalArgumentException("ShortTermMemory delay must be 0 or greater");
        if (fadeIn < 0) throw new IllegalArgumentException("ShortTermMemory fadeIn must be 0 or greater");
        if (length < 1) throw new IllegalArgumentException("ShortTermMemory length must be 1 or greater");
        if (fadeOut < 0) throw new IllegalArgumentException("ShortTermMemory fadeOut must be 0 or greater");

        this.lastTweaked = -1;

        this.defaultVal = defaultVal;
        this.nextOutput = defaultVal;

        this.delay = delay;
        this.fadeIn = fadeIn;
        this.length = length;
        this.fadeOut = fadeOut;
        this.memory = new double[delay + fadeIn + length + fadeOut];
    }

    public ShortTermMemoryNeuron(SignalProvider input, short defaultVal, int delay, int fadeIn, int length, int fadeOut)
            throws IllegalArgumentException {

        super(List.of(input));

        if (delay < 0) throw new IllegalArgumentException("ShortTermMemory delay must be 0 or greater");
        if (fadeIn < 0) throw new IllegalArgumentException("ShortTermMemory fadeIn must be 0 or greater");
        if (length < 1) throw new IllegalArgumentException("ShortTermMemory length must be 1 or greater");
        if (fadeOut < 0) throw new IllegalArgumentException("ShortTermMemory fadeOut must be 0 or greater");

        this.lastTweaked = -1;

        this.defaultVal = defaultVal;
        this.nextOutput = defaultVal;

        this.delay = delay;
        this.fadeIn = fadeIn;
        this.length = length;
        this.fadeOut = fadeOut;
        this.memory = new double[delay + fadeIn + length + fadeOut];
    }

    public ShortTermMemoryNeuron(ShortTermMemoryNeuron cloneFrom) {
        super(cloneFrom);
        this.lastTweaked = cloneFrom.lastTweaked;
        this.defaultVal = cloneFrom.defaultVal;
        this.delay = cloneFrom.delay;
        this.fadeIn = cloneFrom.fadeIn;
        this.length = cloneFrom.length;
        this.fadeOut = cloneFrom.fadeOut;

        this.memory = new double[cloneFrom.memory.length];
    }

    public ShortTermMemoryNeuron(ShortTermMemoryNeuron cloneFrom, short defaultVal,
                                 int delay, int fadeIn, int length, int fadeOut, boolean forTrial) {

        super(cloneFrom);

        if (delay < 0) throw new IllegalArgumentException("ShortTermMemory delay must be 0 or greater");
        if (fadeIn < 0) throw new IllegalArgumentException("ShortTermMemory fadeIn must be 0 or greater");
        if (length < 1) throw new IllegalArgumentException("ShortTermMemory length must be 1 or greater");
        if (fadeOut < 0) throw new IllegalArgumentException("ShortTermMemory fadeOut must be 0 or greater");

        this.lastTweaked = forTrial ? NeuralNet.getCurrentGeneration() : cloneFrom.lastTweaked;

        this.defaultVal = defaultVal;
        this.nextOutput = defaultVal;

        this.delay = delay;
        this.fadeIn = fadeIn;
        this.length = length;
        this.fadeOut = fadeOut;
        this.memory = new double[delay + fadeIn + length + fadeOut];
    }

    @Override
    public int getMinInputs() {
        return 1;
    }

    @Override
    public int getMaxInputs() {
        return 1;
    }

    @Override
    protected short calcOutput(List<SignalProvider> inputs) {
        return this.nextOutput;
    }

    public void after() {
        this.getOutput();
        // ensures that the output cache in CachingNeuron is populated with the CURRENT value from this.nextOutput
        // BEFORE this.nextOutput is re-assigned below.  This is done in case there is another memory neuralNet.neuron which
        // relies upon this one and hasn't had its after() method invoked yet, or in case of an un-invoked
        // circular loop where this neuralNet.neuron's input chain relies upon its own output

        // NOTE: the aforementioned circular loop IS legal, precisely because the calculations done below are for
        // the NEXT round, while the neuralNet.neuron's current output is not reliant upon its current input (though current
        // output would rely upon its output from the PREVIOUS round, in the case of the described loop)

        List<SignalProvider> inputs = this.getInputs();
        this.memory[this.index] = inputs.get(0).getOutput();

        if (this.size != memory.length) {
            this.size++;
        }

        double sum = 0;
        double weightSum = 0;

        int fadeIn = 0;
        int length = 0;
        int fadeOut = this.fadeOut;

        int i;
        boolean first; //flag to mark the first iteration when it starts at the ending index (below)
        int end;

        if (this.delay == 0) {
            i = this.index;
            if (this.size == this.memory.length) {
                first = true;
                end = this.index;

            } else {
                first = false;
                end = this.memory.length - 1;
            }

        } else {
            first = false;
            i = this.index - this.delay;
            if (this.size == this.memory.length) {
                end = this.index;
                if (i < 0) i += this.memory.length;

            } else if (i < 0) end = i;
            else end = this.memory.length - 1;
        }

        while (true) {
            if (i == end) {
                if (first) first = false;
                else break;
            }


            if (fadeIn != this.fadeIn) {
                fadeIn++;
                //double weight = fadeIn / this.fadeInPlusOneDbl;
                sum += this.memory[i] * fadeIn; //weight;
                weightSum += fadeIn; //weight; // TODO calculate weightSum using a single polynomial equation instead


            } else if (length != this.length) {
                sum += this.memory[i] * this.length;
                // full-strength memories' weight is added at the end
                length++;

            } else if (fadeOut != 0) {
                //double weight = fadeOut / this.fadeOutPlusOneDbl;
                sum += this.memory[i] * fadeOut; //* weight;
                weightSum += fadeOut; //weight; // TODO calculate weightSum using a single polynomial equation instead
                fadeOut--;

            } else {
                throw new IllegalStateException();
                //break; //should be unreachable if iteration logic works correctly
            }

            if (i != 0) i--;
            else i = this.memory.length - 1;
        }

        weightSum += length * this.length; //more efficient than incrementing/adding to weightSum each time?

        if (++this.index == this.memory.length) this.index = 0;

        if (weightSum == 0) {
            this.nextOutput = this.defaultVal;

        } else {
            this.nextOutput = roundClip(sum / weightSum);
        }
    }

    @Override
    public void reset() {
        this.nextOutput = this.defaultVal;
        this.size = 0;
        this.index = 0;
        super.reset();
    }

    @Override
    public ShortTermMemoryNeuron clone() {
        return new ShortTermMemoryNeuron(this);
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

    public String toString() {
        return "ShortTermMemory(" + this.defaultVal + ", " + this.delay + ", "
                + this.fadeIn + ", " + this.length + ", " + this.fadeOut + ")";
    }

    @Override
    public List<Param> getTweakingParams() {
        if (this.tweakingParams != null) return this.tweakingParams;

        short[] minParams = toAchieveByMagnitudeOnly(this.delay + 1, 1, this.fadeIn + 1, 1,
                                                        this.length, 1, this.fadeOut + 1, 1);
        //always offset delay, fadeIn, and fadeOut by 1 so 0 can be reached through a logarithmic function
        // achieving "1" means actually achieving delay/fadeIn/fadeOut of 0
        // length must be at least one, so it is not shifted

        return this.tweakingParams = List.of(new Param(this.defaultVal),
                new Param(minParams[0], Short.MAX_VALUE),
                new Param(minParams[1], Short.MAX_VALUE),
                new Param(minParams[2], Short.MAX_VALUE),
                new Param(minParams[3], Short.MAX_VALUE));
    }

    @Override
    public Long getLastTweakedGeneration() {
        return this.lastTweaked == -1 ? null : this.lastTweaked;
    }

    @Override
    public ShortTermMemoryNeuron tweak(short[] params, boolean forTrial) {
        //always offset delay and fadeIn by 1 so 0 can be reached through a logarithmic function
        int delay = (int)Math.round(transformByMagnitudeOnly(this.delay + 1, params[1])) - 1;
        int fadeIn = (int)Math.round(transformByMagnitudeOnly(this.fadeIn + 1, params[2])) - 1;
        int length = (int)Math.round(transformByMagnitudeOnly(this.length, params[3])); //no offset, length >= 1
        int fadeOut = (int)Math.round(transformByMagnitudeOnly(this.fadeOut + 1, params[4])) - 1;

        if (delay < 0) delay = 0; //possible -1 due to rounding when making the original params minimum
        if (fadeIn < 0) fadeIn = 0;
        if (length < 1) length = 1;
        if (fadeOut < 0) fadeOut = 0;

        return new ShortTermMemoryNeuron(this, (short)(this.defaultVal + params[0]), delay, fadeIn, length, fadeOut, forTrial);
    }

    @Override
    public short[] getTweakingParams(ShortTermMemoryNeuron toAchieve) {
        //always offset delay and fadeIn by 1 so 0 can be reached through a logarithmic function
        short[] params = toAchieveByMagnitudeOnly(new short[5], this.delay + 1, toAchieve.delay + 1,
                this.fadeIn + 1, toAchieve.fadeIn + 1, this.length, toAchieve.length,
                this.fadeOut + 1, toAchieve.fadeOut + 1);

        params[4] = params[3];
        params[3] = params[2];
        params[2] = params[1];
        params[1] = params[0];
        params[0] = clip((int)toAchieve.defaultVal - (int)this.defaultVal);

        return params;
    }

    public static final long HASH_HEADER = NeuralHash.HEADERS.get(MethodHandles.lookup().lookupClass());

    @Override
    protected long calcHash() {
        return HASH_HEADER ^ Long.rotateRight(this.inputsMutable.get(0).getNeuralHash(), 17)
                ^ Long.rotateLeft(this.defaultVal & 0xffff, 51)
                ^ Long.rotateLeft(this.delay, 37)  ^ Long.rotateLeft(this.fadeIn, 29)
                ^ Long.rotateLeft(this.length, 17) ^ Long.rotateLeft(this.fadeOut, 7);
    }
}
