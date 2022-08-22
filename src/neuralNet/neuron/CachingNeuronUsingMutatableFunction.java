package neuralNet.neuron;

import neuralNet.function.*;

import java.util.*;

public class CachingNeuronUsingMutatableFunction extends CachingNeuronUsingFunction
        implements Mutatable<CachingNeuronUsingMutatableFunction> {

    public CachingNeuronUsingMutatableFunction(CachingNeuronUsingMutatableFunction cloneFrom) {
        super(cloneFrom);
    }

    public CachingNeuronUsingMutatableFunction(CachingNeuronUsingMutatableFunction cloneFrom,
                                               FunctionWithInputs.Mutatable outputFunction)
            throws NullPointerException {

        super(cloneFrom, outputFunction);
    }

    @Override
    protected short calcOutput(List<SignalProvider> inputs) {
        return this.outputFunction.calcOutput(inputs);
    }

    @Override
    public CachingNeuronUsingMutatableFunction clone() {
        return new CachingNeuronUsingMutatableFunction(this);
    }

    @Override
    public List<Param> getMutationParams() {
        return ((Mutatable)this.outputFunction).getMutationParams();
    }

    @Override
    public CachingNeuronUsingMutatableFunction mutate(short[] params) {
        return new CachingNeuronUsingMutatableFunction(this,
                ((FunctionWithInputs.Mutatable<? extends FunctionWithInputs.Mutatable>)this.outputFunction).mutate(params));
    }

    @Override
    public short[] getMutationParams(CachingNeuronUsingMutatableFunction toAchieve) {
        return new short[0];
    }
}
