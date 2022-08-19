package neuron;

import java.util.*;

import static neuron.StatelessMutatableFunction.*;

public class Ceiling implements StatelessMutatableFunction<Ceiling> {
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
    public List<Param> getMutationParams() {
        if (this.mutationParams == null) {
            this.mutationParams = List.of(new Param(this.ceiling));
        }
        return this.mutationParams;
    }

    @Override
    public Ceiling mutate(short[] params) {
        return new Ceiling((short)(this.ceiling + params[0]));
    }

    @Override
    public short[] getMutationParams(Ceiling toAchieve) {
        return toAchieve(this.ceiling, toAchieve.ceiling);
    }
}
