package neuralNet.function;

import neuralNet.neuron.*;

import java.util.*;

public interface FunctionWithInputs {

    public int getMinInputs();
    public int getMaxInputs();

    default public boolean inputOrderMatters() {
        return false;
    }

    public short calcOutput(List<SignalProvider> inputs);

    public interface Tweakable<M extends Tweakable<M>>
            extends FunctionWithInputs, neuralNet.function.Tweakable<M> { }
}
