package neuralNet.neuron;

import java.util.*;

import static neuralNet.util.Util.*;

/**
 * Memories taper into full signal weight over the course of the initial delay number of rounds.
 * After traversing the length number of round at full weight, they fade out of memory over the
 * fade number of rounds.
 *
 * Note that delay, fadeIn, and fadeOut can all be set to zero.  Length must be at least one
 *
 */
public class ShortTermMemoryNeuron extends CachingNeuron {
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
    //public final double fadeOutPlusOneDbl;

    private final double[] memory;
    private int index = 0;
    private int size = 0;

    private short nextOutput;

    public ShortTermMemoryNeuron(ShortTermMemoryNeuron cloneFrom) {
        super(cloneFrom);
        this.defaultVal = cloneFrom.defaultVal;
        this.delay = cloneFrom.delay;
        this.fadeIn = cloneFrom.fadeIn;
        //this.fadeInPlusOneDbl = cloneFrom.fadeInPlusOneDbl;
        this.length = cloneFrom.length;
        this.fadeOut = cloneFrom.fadeOut;
        //this.fadeOutPlusOneDbl = cloneFrom.fadeOutPlusOneDbl;

        this.memory = new double[cloneFrom.memory.length];
    }

    /**
     * Behaves as an immediate short-term memory ... the most recent round (previous round) is at full weight for
     * one round and then fades
     *
     */
    public ShortTermMemoryNeuron(SignalProvider input, int fadeOutOnly)
            throws IllegalArgumentException {

        this(input, 0, 0, 1, fadeOutOnly, (short)0);
    }

    public ShortTermMemoryNeuron(SignalProvider input, int delay, int fadeIn, int length, int fadeOut, short defaultVal)
            throws IllegalArgumentException {

        super(List.of(input));

        if (delay < 0) throw new IllegalArgumentException("ShortTermMemory delay must be 0 or greater");
        if (fadeIn < 0) throw new IllegalArgumentException("ShortTermMemory fadeIn must be 0 or greater");
        if (length < 1) throw new IllegalArgumentException("ShortTermMemory length must be 1 or greater");
        if (fadeOut < 0) throw new IllegalArgumentException("ShortTermMemory fadeOut must be 0 or greater");

        this.defaultVal = defaultVal;
        this.nextOutput = defaultVal;

        this.delay = delay;
        this.fadeIn = fadeIn;
        this.length = length;
        this.fadeOut = fadeOut;
        this.memory = new double[delay + fadeIn + length + fadeOut];

        //this.fadeInPlusOneDbl = fadeIn + 1;
        //this.fadeOutPlusOneDbl = fadeOut + 1;
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
        boolean first;
        int end = this.size == this.memory.length ? this.index : this.memory.length - 1;

        if (this.delay == 0 && this.fadeIn == 0) {
            i = this.index;
            first = this.size == this.memory.length;

        } else {
            i = this.index - this.delay;
            if (i < 0) i += this.memory.length;
            first = false;
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

        if (++this.index == this.memory.length) {
            this.index = 0;
        }

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
}
