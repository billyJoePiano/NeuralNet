package neuralNet.neuron;

import neuralNet.network.*;

import java.util.*;

import static neuralNet.function.Tweakable.*;

public class FixedValueProvider implements SignalProvider.Tweakable<FixedValueProvider> {
    public static final FixedValueProvider MIN = new FixedValueProvider(Short.MIN_VALUE);
    public static final FixedValueProvider MAX = new FixedValueProvider(Short.MAX_VALUE);
    public static final FixedValueProvider ZERO = new FixedValueProvider((short)0);

    public final long lastTweaked;
    public final short output;
    private transient List<Param> tweakingParams;

    public FixedValueProvider() {
        this((short)0);
    }

    public FixedValueProvider(final short output, final long lastTweaked) {
        this.output = output;
        this.lastTweaked = lastTweaked;
    }

    public FixedValueProvider(final short output) {
        this.output = output;
        this.lastTweaked = -1;
    }

    public short getOutput() {
        return this.output;
    }

    @Override
    public List<Param> getTweakingParams() {
        if (this.tweakingParams == null) {
            this.tweakingParams = List.of(new Param(this.output));
        }
        return this.tweakingParams;
    }

    @Override
    public FixedValueProvider tweak(short[] params, boolean forTrial) {
        if (forTrial) {
            return new FixedValueProvider((short) (this.output + params[0]), NeuralNet.getCurrentGeneration());

        } else if (params[0] == 0) {
            return this;

        } else {
            return new FixedValueProvider((short) (this.output + params[0]), this.lastTweaked);
        }
    }

    @Override
    public short[] getTweakingParams(FixedValueProvider toAchieve) {
        return toAchieve(this.output, toAchieve.output);
    }

    @Override
    public Set<SignalConsumer> getConsumers() {
        return null;
    }

    @Override
    public boolean addConsumer(SignalConsumer consumer) {
        return false;
    }

    @Override
    public boolean removeConsumer(SignalConsumer consumer) {
        return false;
    }

    //@Override
    //public void clearConsumers() { }

    @Override
    public boolean replaceConsumer(SignalConsumer oldConsumer, SignalConsumer newConsumer) {
        return false;
    }

    @Override
    public void replaceConsumers(Map<SignalConsumer, SignalConsumer> neuronMap) { }

    @Override
    public void reset() { }

    @Override
    public FixedValueProvider clone() {
        return this;
    }

    public int hashCode() {
        return this.output;
    }

    public String toString() {
        return "FixedValue(" + this.output + ")";
    }

    @Override
    public Long getLastTweakedGeneration() {
        return this.lastTweaked == -1 ? null : this.lastTweaked;
    }
}