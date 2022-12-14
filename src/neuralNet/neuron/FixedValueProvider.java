package neuralNet.neuron;

import neuralNet.network.*;

import java.lang.invoke.*;
import java.util.*;

import static neuralNet.evolve.Tweakable.*;

public class FixedValueProvider extends CachingProvider
        implements SignalProvider.Tweakable<FixedValueProvider> {

    private static final FixedValueProvider MIN = new FixedValueProvider(Short.MIN_VALUE);
    private static final FixedValueProvider MAX = new FixedValueProvider(Short.MAX_VALUE);
    private static final FixedValueProvider ZERO = new FixedValueProvider();
    private static final FixedValueProvider NEG_ONE = new FixedValueProvider(-1);

    public static FixedValueProvider makeMin() {
        return MIN.clone();
    }

    public static FixedValueProvider makeMax() {
        return MAX.clone();
    }

    public static FixedValueProvider makeZero() {
        return ZERO.clone();
    }

    public static FixedValueProvider makeNegOne() {
        return NEG_ONE.clone();
    }

    public final long lastTweaked;
    public final short value;

    private transient List<Param> tweakingParams;

    /*
    private Object readResolve() throws ObjectStreamException {
        return new FixedValueProvider(this, null);
    }

    private FixedValueProvider(FixedValueProvider deserializedFrom, Void v) {
        super(deserializedFrom, v);
        this.lastTweaked = deserializedFrom.lastTweaked;
        this.value = deserializedFrom.value;
    }
     */

    public FixedValueProvider() {
        this(0);
    }

    public FixedValueProvider(final int value) {
        if (value > Short.MAX_VALUE || value < Short.MIN_VALUE) throw new IllegalArgumentException();
        this.value = (short)value;
        this.lastTweaked = -1;
    }

    public FixedValueProvider(final short value) {
        this.value = value;
        this.lastTweaked = -1;
    }

    public FixedValueProvider(FixedValueProvider cloneFrom) {
        super(cloneFrom);
        this.value = cloneFrom.value;
        this.lastTweaked = cloneFrom.lastTweaked;
    }

    public FixedValueProvider(FixedValueProvider cloneFrom, final short value, final boolean forTrial) {
        super(cloneFrom);
        this.value = value;
        this.lastTweaked = forTrial ? NeuralNet.getCurrentGeneration() : cloneFrom.lastTweaked;
    }

    @Override
    public short getOutput() {
        return this.value;
    }

    @Override
    public boolean sameBehavior(SignalProvider other) {
        if (other == this) return true;
        if (!(other instanceof FixedValueProvider o)) return false;
        return this.value == o.value;
    }

    @Override
    protected short calcOutput() {
        return this.value;
    }

    @Override
    public List<Param> getTweakingParams() {
        if (this.tweakingParams == null) {
            this.tweakingParams = List.of(new Param(this.value));
        }
        return this.tweakingParams;
    }

    @Override
    public FixedValueProvider tweak(short[] params, boolean forTrial) {
        return new FixedValueProvider(this, (short) (this.value + params[0]), forTrial);
    }

    @Override
    public short[] getTweakingParams(FixedValueProvider toAchieve) {
        return toAchieve(this.value, toAchieve.value);
    }

    @Override
    public void before() { }

    @Override
    public void reset() { }

    @Override
    public FixedValueProvider clone() {
        return new FixedValueProvider(this);
    }

    public int hashCode() {
        return this.value;
    }

    public String toString() {
        return "FixedValue(" + this.value + ")";
    }

    @Override
    public Long getLastTweakedGeneration() {
        return this.lastTweaked == -1 ? null : this.lastTweaked;
    }

    public static final long HASH_HEADER = NeuralHash.HEADERS.get(MethodHandles.lookup().lookupClass());
    @Override
    public long calcNeuralHash() {
        return HASH_HEADER | Long.rotateLeft(this.value & 0xffff, 29);
    }
}