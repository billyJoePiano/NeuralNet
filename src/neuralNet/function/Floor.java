package neuralNet.function;

import neuralNet.neuron.*;

import java.lang.invoke.*;
import java.util.*;

import static neuralNet.evolve.Tweakable.*;

public class Floor implements NeuralFunction.Tweakable<Floor> {
    public static CachingNeuronUsingTweakableFunction makeNeuron(short floor) {
        return new CachingNeuronUsingTweakableFunction(new Floor(floor));
    }

    public final short floor;
    private transient List<Param> tweakingParams;

    public Floor(short floor) {
        this.floor = floor;
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
        return input > this.floor ? input : this.floor;
    }

    @Override
    public List<Param> getTweakingParams() {
        if (this.tweakingParams == null) {
            this.tweakingParams = List.of(new Param(this.floor));
        }
        return this.tweakingParams;
    }

    @Override
    public Floor tweak(short[] params) {
        return new Floor((short)(this.floor + params[0]));
    }

    @Override
    public short[] getTweakingParams(Floor toAchieve) {
        return toAchieve(this.floor, toAchieve.floor);
    }

    public static final long HASH_HEADER = NeuralHash.HEADERS.get(MethodHandles.lookup().lookupClass());
    @Override
    public long hashHeader() {
        return HASH_HEADER;
    }

    @Override
    public long hashTweakMask() {
        return Long.rotateLeft(floor & 0xffff, 17);
    }
}
