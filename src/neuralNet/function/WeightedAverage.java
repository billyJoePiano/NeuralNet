package neuralNet.function;

import neuralNet.neuron.*;

import java.lang.invoke.*;
import java.util.*;

import static neuralNet.evolve.Tweakable.*;
import static neuralNet.util.Util.*;

/**
 *
 */
public class WeightedAverage implements NeuralFunction.Tweakable<WeightedAverage> {
    public static CachingNeuronUsingFunction makeNeuron(List<SignalProvider> inputs, double ... weights) {
        if (inputs.size() != weights.length) throw new IllegalArgumentException("Inputs list and weights array must have the same length!");
        return new CachingNeuronUsingFunction(new WeightedAverage(weights), inputs);
    }

    public static CachingNeuronUsingFunction makeNeuron(double ... weights) {
        return new CachingNeuronUsingFunction(new WeightedAverage(weights));
    }


    private final double[] weights;
    private final double weightSum;
    private transient List<Param> tweakingParams;

    public WeightedAverage(double ... weights) {
        if (weights.length < 2 || weights.length > 256) throw new IllegalArgumentException();
        this.weights = new double[weights.length];

        double weightSum = 0;

        for (int i = 0; i < weights.length; i++) {
            double weight = weights[i];
            if (!Double.isFinite(weight) || weight <= 0.0) throw new IllegalArgumentException();
            this.weights[i] = weight;
            weightSum += weight;
        }

        this.weightSum = weightSum;
    }

    private WeightedAverage(Void privateInit, double ... weights) {
        //skips most checks, and doesn't clone the weights array
        double sum = 0;
        for (double weight : weights) {
            sum += weight;
        }

        if (!Double.isFinite(sum)) throw new IllegalStateException();

        this.weights = weights;
        this.weightSum = sum;
    }

    @Override
    public int getMinInputs() {
        return weights.length;
    }

    @Override
    public int getMaxInputs() {
        return weights.length;
    }

    @Override
    public short calcOutput(List<SignalProvider> inputs) {
        if (inputs.size() != weights.length) throw new IllegalStateException();

        double sum = 0;
        int i = 0;

        for (SignalProvider neuron : inputs) {
            sum += neuron.getOutput() * this.weights[i++];
        }

        return roundClip(sum / this.weightSum);
    }

    @Override
    public List<Param> getTweakingParams() {
        if (this.tweakingParams != null) return this.tweakingParams;

        //TODO??? Place limits on extreme cases, where the smallest weight is effectively zero (due to rounding) in relation to the largest weight

        int len = Math.max(Math.min(this.weights.length + 1, 256), 2);
        return this.tweakingParams = Collections.nCopies(len, Param.DEFAULT);
    }

    @Override
    public WeightedAverage tweak(short[] params) {
        double[] weights = new double[params.length];

        int stop = Math.min(params.length, this.weights.length);

        for (int i = 0; i < stop; i++) {
            weights[i] = transformByMagnitudeOnly(this.weights[i], params[i]);
        }

        for (int i = stop; i < params.length; i++) {
            weights[i] = transformByMagnitudeOnly(1.0, params[i]);
        }

        return new WeightedAverage(null, weights);
    }

    @Override
    public short[] getTweakingParams(WeightedAverage toAchieve) {
        double[] values = new double[Math.max(this.weights.length, toAchieve.weights.length)];

        for (int i = 0, v = 0; i < values.length; i++) {
            if (i < this.weights.length) values[v++] = this.weights[i];
            else values[v++] = 1.0;

            if (i < toAchieve.weights.length) values[v++] = toAchieve.weights[i];
            else values[v++] = 1.0;
        }
        return toAchieveByMagnitudeOnly(values);
    }

    @Override
    public boolean paramsPerInput() {
        return true;
    }

    @Override
    public boolean inputOrderMatters() {
        return true;
    }

    public static final long HASH_HEADER = NeuralHash.HEADERS.get(MethodHandles.lookup().lookupClass());
    @Override
    public long hashHeader() {
        return HASH_HEADER;
    }


    @Override
    public long hashTweakMask() {
        long hash = 0;
        for (double weight : this.weights) {
            hash ^= Double.doubleToLongBits(weight);
            hash = Long.rotateRight(hash, 17);
        }
        return hash;
    }

    @Override
    public boolean sameBehavior(WeightedAverage other) {
        if (this.weightSum != other.weightSum) return false;
        if (this.weights.length != other.weights.length) return false;

        for (int i = 0; i < this.weights.length; i++) {
            if (this.weights[i] != other.weights[i]) return false;
        }
        return true;
    }
}
