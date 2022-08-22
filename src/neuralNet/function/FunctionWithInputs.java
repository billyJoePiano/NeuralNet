package neuralNet.function;

import neuralNet.neuron.*;

import java.util.*;

public interface FunctionWithInputs {

    public int getMinInputs();
    public int getMaxInputs();

    public short calcOutput(List<SignalProvider> inputs);

    public interface Mutatable<M extends Mutatable<M>>
            extends FunctionWithInputs, neuralNet.function.Mutatable<M> { }
}
