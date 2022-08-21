package neuralNet.neuron;

import java.util.*;

import static neuralNet.function.StatelessMutatableFunction.*;

public class FixedValueNeuron implements SignalProvider, Mutatable<FixedValueNeuron> {
    public final short output;
    private List<Param> mutationParams;

    public FixedValueNeuron() {
        this((short)0);
    }

    public FixedValueNeuron(final short output) {
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
    public FixedValueNeuron mutate(short[] params) {
        if (params[0] == 0) return this;
        return new FixedValueNeuron((short)(this.output + params[0]));
    }

    @Override
    public short[] getMutationParams(FixedValueNeuron toAchieve) {
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

    @Override
    public void clearConsumers() { }

    @Override
    public FixedValueNeuron clone() {
        return this;
    }

    public int hashCode() {
        return this.output;
    }
}
