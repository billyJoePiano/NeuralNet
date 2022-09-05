package neuralNet.function;

import neuralNet.neuron.*;

import java.io.*;
import java.util.*;

public interface NeuralFunction extends Serializable {

    public int getMinInputs();
    public int getMaxInputs();

    default public boolean inputOrderMatters() {
        return false;
    }
    default public boolean pairedInputs() {
        return false;
    }

    public short calcOutput(List<SignalProvider> inputs);

    default public long getNeuralHash() {
        return this.hashHeader();
    }
    public long hashHeader();

    public interface Tweakable<M extends Tweakable<M>>
            extends NeuralFunction, neuralNet.evolve.Tweakable<M> {

        public long hashTweakMask();

        @Override
        default public long getNeuralHash() {
            return this.hashHeader() ^ this.hashTweakMask();
        }
    }
}
