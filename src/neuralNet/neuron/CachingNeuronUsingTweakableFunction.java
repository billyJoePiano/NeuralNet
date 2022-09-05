package neuralNet.neuron;

import neuralNet.function.*;
import neuralNet.network.*;

import java.util.*;

public class CachingNeuronUsingTweakableFunction extends CachingNeuronUsingFunction
        implements SignalProvider.Tweakable<CachingNeuronUsingTweakableFunction> {

    public final long lastTweaked;

    public CachingNeuronUsingTweakableFunction(CachingNeuronUsingTweakableFunction cloneFrom) {
        super(cloneFrom);
        this.lastTweaked = cloneFrom.lastTweaked;
    }

    public CachingNeuronUsingTweakableFunction(CachingNeuronUsingTweakableFunction cloneFrom,
                                               NeuralFunction.Tweakable outputFunction,
                                               boolean forTrial)
            throws NullPointerException {

        super(cloneFrom, outputFunction);
        this.lastTweaked = forTrial ? NeuralNet.getCurrentGeneration() : cloneFrom.lastTweaked;
    }

    public CachingNeuronUsingTweakableFunction(NeuralFunction.Tweakable outputFunction)
            throws NullPointerException {

        super(outputFunction);
        this.lastTweaked = -1;
    }

    public CachingNeuronUsingTweakableFunction(NeuralFunction.Tweakable outputFunction,
                                               List<SignalProvider> inputs)
            throws NullPointerException {

        super(outputFunction, inputs);
        this.lastTweaked = -1;
    }

    public CachingNeuronUsingTweakableFunction(NeuralFunction.Tweakable outputFunction,
                                               SignalProvider... inputs) {
        super(outputFunction, inputs);
        this.lastTweaked = -1;
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
        return ((NeuralFunction.Tweakable)this.outputFunction).getTweakingParams();
    }

    @Override
    public Long getLastTweakedGeneration() {
        return this.lastTweaked == -1 ? null : this.lastTweaked;
    }

    @Override
    public CachingNeuronUsingTweakableFunction tweak(short[] params, boolean forTrial) {
        return new CachingNeuronUsingTweakableFunction(this,
                ((NeuralFunction.Tweakable<? extends NeuralFunction.Tweakable>)this.outputFunction).tweak(params),
                forTrial);
    }

    @Override
    public short[] getTweakingParams(CachingNeuronUsingTweakableFunction toAchieve) {
        return ((NeuralFunction.Tweakable)this.outputFunction)
                    .getTweakingParams((NeuralFunction.Tweakable)toAchieve.outputFunction);
    }
}
