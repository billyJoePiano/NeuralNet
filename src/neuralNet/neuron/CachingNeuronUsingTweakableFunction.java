package neuralNet.neuron;

import neuralNet.function.*;

import java.util.*;

public class CachingNeuronUsingTweakableFunction extends CachingNeuronUsingFunction
        implements Tweakable<CachingNeuronUsingTweakableFunction> {

    public CachingNeuronUsingTweakableFunction(CachingNeuronUsingTweakableFunction cloneFrom) {
        super(cloneFrom);
    }

    public CachingNeuronUsingTweakableFunction(CachingNeuronUsingTweakableFunction cloneFrom,
                                               FunctionWithInputs.Tweakable outputFunction)
            throws NullPointerException {

        super(cloneFrom, outputFunction);
    }

    @Override
    protected short calcOutput(List<SignalProvider> inputs) {
        return this.outputFunction.calcOutput(inputs);
    }

    @Override
    public CachingNeuronUsingTweakableFunction clone() {
        return new CachingNeuronUsingTweakableFunction(this);
    }

    @Override
    public List<Param> getMutationParams() {
        return ((Tweakable)this.outputFunction).getMutationParams();
    }

    @Override
    public CachingNeuronUsingTweakableFunction mutate(short[] params) {
        return new CachingNeuronUsingTweakableFunction(this,
                ((FunctionWithInputs.Tweakable<? extends FunctionWithInputs.Tweakable>)this.outputFunction).mutate(params));
    }

    @Override
    public short[] getMutationParams(CachingNeuronUsingTweakableFunction toAchieve) {
        return new short[0];
    }
}
