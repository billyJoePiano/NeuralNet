package neuron;

import java.util.*;

import static neuron.StatelessFunction.roundClip;
import static neuron.StatelessMutatableFunction.*;

/**
 * Memories may have a delay period with no signal strength, and then may fade into full strength.
 * Once at full strength, the memory is retained forever (until reset is called) in an accumulated
 * average
 *
 */
public class LongTermMemory extends CachingNeuron implements Mutatable<LongTermMemory> {
    public static final double NaN = Double.NaN;

    /**
     * Signal level to provide before enough memories have accumulated to produce a calculated signal
     */
    public final short defaultVal;

    /**
     * number of rounds (including the most recent round) before the current signal input
     * begins fading in.  It will not be included in any weighted calculations until then
     */
    public final int delay;
    public final double delayDbl;

    /**
     * Number of rounds over which a memory starts to fade in to long-term memory.
     * Weight of signal is round / (fadeIn + 1)
     */
    public final int fadeIn;

    public final double fadeInPlusOne;
    // The weight of all long-term established memories relative to the first (lowest-weighted) fade-in memory which is weighted 1
    // The last fade-in memory (the round before it is added to accumulated store) will have a weight of fadeIn


    public final double fadeInPlusAddToStoreTotalWeight;


    /**
     * Memories that are still delayed or are fading-in
     */
    private final double[] recent;
    private int index = 0;
    private int size = 0;

    private short nextOutput;

    private double[] accumulated = new double[] {
            NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN
        }; //NaN is marker for not initialized... like a primitive version of 'null', to avoid boxing/unboxing overhead

    private List<Param> mutationParams;

    public LongTermMemory(LongTermMemory cloneFrom) {
        super(cloneFrom);
        this.defaultVal = cloneFrom.defaultVal;
        this.delay = cloneFrom.delay;
        this.delayDbl = cloneFrom.delayDbl;
        this.fadeIn = cloneFrom.fadeIn;
        this.fadeInPlusOne = cloneFrom.fadeInPlusOne;
        this.fadeInPlusAddToStoreTotalWeight = cloneFrom.fadeInPlusAddToStoreTotalWeight;
        this.mutationParams = cloneFrom.mutationParams;

        if (delay == 0 && fadeIn == 0) this.recent = null;
        else this.recent = new double[delay + fadeIn];
    }

    public LongTermMemory(LongTermMemory cloneFrom, short defaultVal, int delay, int fadeIn)
            throws IllegalArgumentException {

        super(cloneFrom);

        if (delay < 0) throw new IllegalArgumentException("LongTermMemory delay must be 0 or greater");
        if (fadeIn < 0) throw new IllegalArgumentException("LongTermMemory fadeIn must be 0 or greater");

        this.defaultVal = defaultVal;
        this.nextOutput = defaultVal;
        this.delay = delay;
        this.delayDbl = delay;
        this.fadeIn = fadeIn;
        this.fadeInPlusOne = fadeIn + 1;
        this.fadeInPlusAddToStoreTotalWeight = this.fadeInPlusOne * (this.fadeInPlusOne + 1) / 2;
        // NOTE: x * (x + 1) / 2 is the formula for the sum of all integers between 1 and x (inclusive)

        if (delay == 0 && fadeIn == 0) this.recent = null;
        else this.recent = new double[delay + fadeIn];
    }

    public LongTermMemory(List<SignalProvider> input, int delayAndFadeIn)
            throws IllegalArgumentException {

        this(input, (short)0, delayAndFadeIn, delayAndFadeIn);
    }

    public LongTermMemory(List<SignalProvider> input, short defaultVal, int delay, int fadeIn)
            throws IllegalArgumentException {

        super(input);

        if (delay < 0) throw new IllegalArgumentException("LongTermMemory delay must be 0 or greater");
        if (fadeIn < 0) throw new IllegalArgumentException("LongTermMemory fadeIn must be 0 or greater");

        this.defaultVal = defaultVal;
        this.nextOutput = defaultVal;
        this.delay = delay;
        this.delayDbl = delay;
        this.fadeIn = fadeIn;
        this.fadeInPlusOne = fadeIn + 1;
        this.fadeInPlusAddToStoreTotalWeight = this.fadeInPlusOne * (this.fadeInPlusOne + 1) / 2;
        // NOTE: x * (x + 1) / 2 is the formula for the sum of all integers between 1 and x (inclusive)

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

    public void after() {
        this.getOutput();
        // ensures that the output cache in CachingNeuron is populated with the CURRENT value from this.nextOutput
        // BEFORE this.nextOutput is re-assigned below.  This is done in case there is another memory neuron which
        // relies upon this one and hasn't had its after() method invoked yet, or in case of an un-invoked
        // circular loop where this neuron's input chain relies upon its own output

        List<SignalProvider> inputs = this.getInputs();

        if (this.recent == null) {
            // no delay and no fade in... skip right to adding to the accumulated store
            double addToStore = inputs.get(0).getOutput();
            this.nextOutput = this.processAccumulated(addToStore, addToStore);

        } else if (this.size == this.recent.length) {
            double addToStore = this.recent[this.index]; // move the oldest value into the accumulated array...
            this.recent[this.index] = inputs.get(0).getOutput(); // ...replace with the newest value

            if (this.fadeIn == 0) {
                if (++this.index == this.recent.length) this.index = 0;
                this.nextOutput = this.processAccumulated(addToStore, addToStore);

            } else {
                //the main execution path once long-term memory is established (except when this.recent == null, aka no delay or fadeIn)
                this.nextOutput = this.processAccumulated(addToStore, (processRecent() + addToStore * this.fadeInPlusOne) / this.fadeInPlusAddToStoreTotalWeight);
            }

        } else {
            // nothing to add to accumulated yet
            this.size++;
            this.recent[this.index] = inputs.get(0).getOutput();

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
        int fadeIn = 0;

        if (this.delay == 0) {
            i = this.index;

        } else {
            i = this.index - this.delay;
        }

        double weightSum = (i * (i + 1)) >> 1; // bit shift used as alternative to divide by 2
        // even * odd == even ... therefore, there will never be any fractional from loss dividing by 2...

        while (i != -1) {
            fadeIn++;
            //double weight = fadeIn / this.fadeInPlusOneDbl;

            sum += this.recent[i] * i; // * weight
            //weightSum += fadeIn; // += weight;

            i--;
        }

        if (++this.index == this.recent.length) {
            this.index = 0;
        }

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

        if (++this.index == this.recent.length) {
            this.index = 0;
        }

        return sum;
    }

    /**
     * Each successive index in the accumulated array represents a power of two, in terms of
     * the number of rounds of memories it represents as an accumulated average.  This makes it
     * easy to combine accumulated averages over time, as the number of memories grows.  Doing this
     * avoids creating an excessively long array with time-consuming calculations each time the weighted
     * average needs to be calculated.
     *
     * EXAMPLE: 1 new memory to add to a small established array...
     *
     * 1 -> [ 1 , 2 , - , 8 , 16 , - ]
     *      ...becomes...
     * ---> [1+1, 2 , - , 8 , 16 , - ] <--- combining through powers of two
     *      [ 2 + 2 , - , 8 , 16 , - ] <--- keep combining...
     *      [ - , - , 4 , 8 , 16 , - ] <--- ...until an empty slot is reached
     *
     * (values are the number of memories represented at each index ... dash indicates NaN, aka nothing represented)
     *
     * In the resulting example, only 3 values are needed to calculate the final average representing 28 memories!
     *
     * @param addToStore
     * @param fadeInAvg
     * @return
     */
    private short processAccumulated(double addToStore, double fadeInAvg) { //fadeInAvg ALREADY INCLUDES addToStore
        double weightSum = 0;
        // sum of weights including the fadeInAvg

        double avg = 0;
        //accumulator for the return value, which includes the fadeIn memories

        double currentWeight = 1;
        // always a power of 2.  The weight of the current index in the accumulated array: 2^i

        boolean needToAddFadeInAvg = true;
        // delay adding the fadeInAvg to avg until the weightSum is greater than or equal to this.fadeInTotalWeightPlusOne
        // ...helps prevent loss of precision
        // NOTE that the fadeInAvg INCLUDES addToStore, so even when this.fadeIn == 0 it is still necessary to include it

        for (int i = 0; i < this.accumulated.length; i++) {
            if (/* is NaN */ this.accumulated[i] != this.accumulated[i]) {
                                    // ^^^ Double.isNaN , inlining
                this.accumulated[i] = addToStore;
                addToStore = NaN;
                currentWeight *= 2;
                continue;
            }

            // otherwise, we incorporate this memory into the avg calculation
            // and combine with addToStore if necessary

            double ratio = weightSum / currentWeight;
            avg = (this.accumulated[i] + avg * ratio) / (ratio + 1);
            weightSum += currentWeight;

            if (needToAddFadeInAvg && weightSum >= this.fadeInPlusAddToStoreTotalWeight) {
                ratio = this.fadeInPlusAddToStoreTotalWeight / weightSum;
                avg = (avg + fadeInAvg * ratio) / (ratio + 1);
                weightSum += this.fadeInPlusAddToStoreTotalWeight;

                needToAddFadeInAvg = false;
            }

            if (/* is NOT NaN */ addToStore == addToStore) {
                //combine two accumulated averages, both representing the same power of 2 number of memories
                addToStore = (addToStore + this.accumulated[i]) / 2; // assign to addToStore, so it continues combining, or gets assigned to the next index if it is empty
                this.accumulated[i] = NaN; // empty the current index

                if (i + 1 == this.accumulated.length) {
                    //need more space in the accumulated array... create a new array and copy over the old one
                    double[] old = this.accumulated;
                    this.accumulated = new double[old.length + 10];

                    int j = 0;
                    for (; j < old.length; j++) {
                        this.accumulated[j] = old[j];
                    }

                    //fill-in NaN for the remaining new slots
                    for (; j < this.accumulated.length; j++) {
                        this.accumulated[j] = NaN;
                    }
                }
            }

            currentWeight *= 2;
        }

        if (needToAddFadeInAvg) {
            if (weightSum == 0) {
                // It will only reach this code block on the first time this method is called,
                // because there was nothing but NaN in the accumulated array, so nothing was added to weightSum
                avg = fadeInAvg;

            } else {
                double ratio = this.fadeInPlusAddToStoreTotalWeight / weightSum;
                avg = (avg + fadeInAvg * ratio) / (ratio + 1);
            }
        }

        return roundClip(avg);
    }

    @Override
    public void reset() {
        this.nextOutput = this.defaultVal;
        this.size = 0;
        Arrays.fill(this.accumulated, NaN);

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
    public LongTermMemory clone() {
        return new LongTermMemory(this);
    }

    @Override
    public List<Param> getMutationParams() {
        if (this.mutationParams == null) {
            this.mutationParams = List.of(new Param(this.defaultVal),
                                    new Param((short) -this.delay, Short.MAX_VALUE),
                                    new Param((short) -this.fadeIn, Short.MAX_VALUE));
        }

        return this.mutationParams;
    }

    @Override
    public LongTermMemory mutate(short[] params) {
        return new LongTermMemory(this, (short)(this.defaultVal + params[2]), this.delay + params[0], this.fadeIn + params[1]);
    }

    @Override
    public short[] getMutationParams(LongTermMemory toAchieve) {
        return toAchieve(this.defaultVal, toAchieve.defaultVal, this.delay, toAchieve.delay, this.fadeIn, toAchieve.fadeIn);
    }
}
