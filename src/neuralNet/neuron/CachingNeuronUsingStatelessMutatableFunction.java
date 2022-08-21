package neuralNet.neuron;

import neuralNet.function.*;
import neuralNet.function.StatelessMutatableFunction.*;

import java.util.*;

public class CachingNeuronUsingStatelessMutatableFunction extends CachingNeuronUsingStatelessFunction
        implements Mutatable<CachingNeuronUsingStatelessMutatableFunction> {

    public CachingNeuronUsingStatelessMutatableFunction(CachingNeuronUsingStatelessMutatableFunction cloneFrom) {
        super(cloneFrom);
    }

    public CachingNeuronUsingStatelessMutatableFunction(CachingNeuronUsingStatelessMutatableFunction cloneFrom,
                                                        StatelessMutatableFunction outputFunction)
            throws NullPointerException {

        super(cloneFrom, outputFunction);
    }

    @Override
    protected short calcOutput(List<SignalProvider> inputs) {
        return this.outputFunction.calcOutput(inputs);
    }

    @Override
    public CachingNeuronUsingStatelessMutatableFunction clone() {
        return new CachingNeuronUsingStatelessMutatableFunction(this);
    }

    @Override
    public List<Param> getMutationParams() {
        return ((StatelessMutatableFunction)this.outputFunction).getMutationParams();
    }

    @Override
    public CachingNeuronUsingStatelessMutatableFunction mutate(short[] params) {
        return new CachingNeuronUsingStatelessMutatableFunction(this, ((StatelessMutatableFunction)this.outputFunction).mutate(params));
    }

    @Override
    public short[] getMutationParams(CachingNeuronUsingStatelessMutatableFunction toAchieve) {
        return new short[0];
    }
}
