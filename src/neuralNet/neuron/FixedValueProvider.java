package neuralNet.neuron;

import neuralNet.function.*;

import java.util.*;

import static neuralNet.function.Tweakable.*;

public class FixedValueProvider implements SignalProvider, Tweakable<FixedValueProvider> {
    public final short output;
    private List<Param> mutationParams;

    public FixedValueProvider() {
        this((short)0);
    }

    public FixedValueProvider(final short output) {
        this.output = output;
    }

    public short getOutput() {
        return this.output;
    }

    @Override
    public List<Param> getMutationParams() {
        if (this.mutationParams == null) {
            this.mutationParams = List.of(new Param(this.output));
        }
        return this.mutationParams;
    }

    @Override
    public FixedValueProvider mutate(short[] params) {
        if (params[0] == 0) return this;
        return new FixedValueProvider((short)(this.output + params[0]));
    }

    @Override
    public short[] getMutationParams(FixedValueProvider toAchieve) {
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
}
