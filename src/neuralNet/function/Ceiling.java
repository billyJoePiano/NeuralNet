package neuralNet.function;

import neuralNet.neuron.*;

import java.util.*;

import static neuralNet.function.Tweakable.*;

public class Ceiling implements FunctionWithInputs.Tweakable<Ceiling> {
    public static CachingNeuronUsingTweakableFunction makeNeuron(short ceiling) {
        return new CachingNeuronUsingTweakableFunction(new Ceiling(ceiling));
    }

    public final short ceiling;
    private List<Param> mutationParams;

    public Ceiling(short ceiling) {
        this.ceiling = ceiling;
    }

    @Override
    public int getMinInputs() {
        return 1;
    }

    @Override
    public int getMaxInputs() {
        return 1;
    }

    @Override
    public short calcOutput(List<SignalProvider> inputs) {
        short input = inputs.get(0).getOutput();
        return input < this.ceiling ? input : this.ceiling;
    }

    @Override
    public List<Param> getTweakingParams() {
        if (this.mutationParams == null) {
            this.mutationParams = List.of(new Param(this.ceiling));
        }
        return this.mutationParams;
    }

    @Override
    public Ceiling tweak(short[] params) {
        return new Ceiling((short)(this.ceiling + params[0]));
    }

    @Override
    public short[] getTweakingParams(Ceiling toAchieve) {
        return toAchieve(this.ceiling, toAchieve.ceiling);
    }
}
