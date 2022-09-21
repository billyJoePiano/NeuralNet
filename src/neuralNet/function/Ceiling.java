package neuralNet.function;

import neuralNet.neuron.*;

import java.lang.invoke.*;
import java.util.*;

import static neuralNet.evolve.Tweakable.*;

public class Ceiling implements NeuralFunction.Tweakable<Ceiling> {
    public static CachingNeuronUsingTweakableFunction makeNeuron(short ceiling) {
        return new CachingNeuronUsingTweakableFunction(new Ceiling(ceiling));
    }

    public final short ceiling;
    private transient List<Param> tweakingParams;

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
        if (this.tweakingParams == null) {
            this.tweakingParams = List.of(new Param(this.ceiling));
        }
        return this.tweakingParams;
    }

    @Override
    public boolean sameBehavior(Ceiling other) {
        return this.ceiling == other.ceiling;
    }

    @Override
    public Ceiling tweak(short[] params) {
        return new Ceiling((short)(this.ceiling + params[0]));
    }

    @Override
    public short[] getTweakingParams(Ceiling toAchieve) {
        return toAchieve(this.ceiling, toAchieve.ceiling);
    }

    public static final long HASH_HEADER = NeuralHash.HEADERS.get(MethodHandles.lookup().lookupClass());
    @Override
    public long hashHeader() {
        return HASH_HEADER;
    }

    @Override
    public long hashTweakMask() {
        return Long.rotateLeft(ceiling & 0xffff, 17);
    }
}
