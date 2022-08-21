package neuralNet.neuron;

import neuralNet.function.*;

import java.util.*;

public interface Mutatable<N extends Mutatable<N>> extends SignalProvider {
    public List<StatelessMutatableFunction.Param> getMutationParams();
    public N mutate(short[] params);
    public short[] getMutationParams(N toAchieve);
}
