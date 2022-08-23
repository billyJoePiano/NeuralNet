package neuralNet.function;

import neuralNet.neuron.*;

import java.util.*;

import static neuralNet.function.Tweakable.*;

public class Floor implements FunctionWithInputs.Tweakable<Floor> {
    public static CachingNeuronUsingTweakableFunction makeNeuron(short floor) {
        return new CachingNeuronUsingTweakableFunction(new Floor(floor));
    }

    public final short floor;
    private List<Param> mutationParams;

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
        if (this.mutationParams == null) {
            this.mutationParams = List.of(new Param(this.floor));
        }
        return this.mutationParams;
    }

    @Override
    public Floor tweak(short[] params) {
        return new Floor((short)(this.floor + params[0]));
    }

    @Override
    public short[] getTweakingParams(Floor toAchieve) {
        return toAchieve(this.floor, toAchieve.floor);
    }
}
