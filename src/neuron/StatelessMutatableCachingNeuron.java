package neuron;

import neuron.StatelessMutatableFunction.*;

import java.util.*;

public class StatelessMutatableCachingNeuron extends StatelessCachingNeuron
        implements Mutatable<StatelessMutatableCachingNeuron> {

    public StatelessMutatableCachingNeuron(StatelessMutatableCachingNeuron cloneFrom) {
        super(cloneFrom);
    }

    public StatelessMutatableCachingNeuron(StatelessMutatableCachingNeuron cloneFrom,
                                           StatelessMutatableFunction outputFunction)
            throws NullPointerException {

        super(cloneFrom, outputFunction);
    }

    @Override
    protected short calcOutput(List<SignalProvider> inputs) {
        return this.outputFunction.calcOutput(inputs);
    }

    @Override
    public StatelessMutatableCachingNeuron clone() {
        return new StatelessMutatableCachingNeuron(this);
    }

    @Override
    public List<Param> getMutationParams() {
        return ((StatelessMutatableFunction)this.outputFunction).getMutationParams();
    }

    @Override
    public StatelessMutatableCachingNeuron mutate(short[] params) {
        return new StatelessMutatableCachingNeuron(this, ((StatelessMutatableFunction)this.outputFunction).mutate(params));
    }

    @Override
    public short[] getMutationParams(StatelessMutatableCachingNeuron toAchieve) {
        return new short[0];
    }
}
