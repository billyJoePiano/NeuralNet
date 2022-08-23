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

    public CachingNeuronUsingTweakableFunction(FunctionWithInputs.Tweakable outputFunction)
            throws NullPointerException {

        super(outputFunction);
    }

    public CachingNeuronUsingTweakableFunction(FunctionWithInputs.Tweakable outputFunction,
                                               List<SignalProvider> inputs)
            throws NullPointerException {

        super(outputFunction, inputs);
    }

    public CachingNeuronUsingTweakableFunction(FunctionWithInputs.Tweakable outputFunction,
                                               SignalProvider... inputs) {
        super(outputFunction, inputs);
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
    public List<Param> getTweakingParams() {
        return ((Tweakable)this.outputFunction).getTweakingParams();
    }

    @Override
    public CachingNeuronUsingTweakableFunction tweak(short[] params) {
        return new CachingNeuronUsingTweakableFunction(this,
                ((FunctionWithInputs.Tweakable<? extends FunctionWithInputs.Tweakable>)this.outputFunction).tweak(params));
    }

    @Override
    public short[] getTweakingParams(CachingNeuronUsingTweakableFunction toAchieve) {
        return new short[0];
    }
}
