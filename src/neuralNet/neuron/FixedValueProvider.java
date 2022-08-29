package neuralNet.neuron;

import neuralNet.network.*;

import java.util.*;

import static neuralNet.evolve.Tweakable.*;

public class FixedValueProvider extends CachingProvider
        implements SignalProvider.Tweakable<FixedValueProvider> {

    private static final FixedValueProvider MIN = new FixedValueProvider(Short.MIN_VALUE);
    private static final FixedValueProvider MAX = new FixedValueProvider(Short.MAX_VALUE);
    private static final FixedValueProvider ZERO = new FixedValueProvider();

    public static FixedValueProvider makeZero() {
        return ZERO.clone();
    }

    public static FixedValueProvider makeMin() {
        return MIN.clone();
    }

    public static FixedValueProvider makeMax() {
        return MAX.clone();
    }

    public final long lastTweaked;
    public final short value;

    private transient List<Param> tweakingParams;

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
}