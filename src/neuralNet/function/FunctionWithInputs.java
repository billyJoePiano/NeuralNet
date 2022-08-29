package neuralNet.function;

import neuralNet.neuron.*;

import java.io.*;
import java.util.*;

public interface FunctionWithInputs extends Serializable {

    public int getMinInputs();
    public int getMaxInputs();

    default public boolean inputOrderMatters() {
        return false;
    }
    default public boolean pairedInputs() {
        return false;
    }

    public short calcOutput(List<SignalProvider> inputs);

    public interface Tweakable<M extends Tweakable<M>>
            extends FunctionWithInputs, neuralNet.evolve.Tweakable<M> { }
}
