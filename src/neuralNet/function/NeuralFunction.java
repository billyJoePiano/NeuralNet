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

    default public boolean sameBehavior(NeuralFunction other) {
        return this == other;
    }

    public interface Tweakable<M extends Tweakable<M>>
            extends NeuralFunction, neuralNet.evolve.Tweakable<M> {

        public long hashTweakMask();

        @Override
        default public long getNeuralHash() {
            return this.hashHeader() ^ this.hashTweakMask();
        }

        @Override
        default public boolean sameBehavior(NeuralFunction other) {
            if (other == this) return true;
            if (other == null || this.getClass() != other.getClass()) return false;
            return this.sameBehavior((M) other);
        }

        public boolean sameBehavior(M other);
    }
}
