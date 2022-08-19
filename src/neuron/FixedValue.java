package neuron;

import java.util.*;

import static neuron.StatelessMutatableFunction.*;

public class FixedValue implements StatelessMutatableFunction<FixedValue> {
    public static StatelessCachingNeuron makeNeuron(short output) {
        return new StatelessCachingNeuron(new FixedValue(output));
    }

    public final short output;
    private List<Param> mutationParams;

    public FixedValue() {
        this((short)0);
    }

    public FixedValue(final short output) {
        this.output = output;
    }

    @Override
    public short calcOutput(List<SignalProvider> inputs) {
        return this.output;
    }

    @Override
    public List<Param> getMutationParams() {
        if (this.mutationParams == null) {
            this.mutationParams = List.of(new Param(this.output));
        }
        return this.mutationParams;
    }

    @Override
    public FixedValue mutate(short[] params) {
        return new FixedValue((short)(this.output + params[0]));
    }

    @Override
    public short[] getMutationParams(FixedValue toAchieve) {
        return toAchieve(this.output, toAchieve.output);
    }
}
