package neuralNet.function;

import neuralNet.neuron.*;

import java.util.*;

import static neuralNet.util.Util.*;

/**
 * AKA AdditionNormalized
 */
public class VariableWeightedAverage implements FunctionWithInputs, Tweakable<VariableWeightedAverage> {
    public static CachingNeuronUsingFunction makeNeuron(double minWeight, double maxWeight, SignalProvider ... inputs) {
        return new CachingNeuronUsingFunction(new VariableWeightedAverage(minWeight, maxWeight), inputs);
    }

    public static CachingNeuronUsingFunction makeNeuron(double minWeight, double maxWeight) {
        return new CachingNeuronUsingFunction(new VariableWeightedAverage(minWeight, maxWeight));
    }

    public static final List<Param> TWEAKING_PARAMS = List.of(Param.DEFAULT, Param.DEFAULT);

    public final double logMin;
    public final double logMax;
    public final double logRange;

    public VariableWeightedAverage(double minWeight, double maxWeight) {
        if (!(minWeight > 0 && maxWeight > 0 && Double.isFinite(minWeight) && Double.isFinite(maxWeight))) {
            throw new IllegalArgumentException();
        }

        this.logMin = Math.log(minWeight);
        this.logMax = Math.log(maxWeight);
        this.logRange = this.logMax - this.logMin;
    }

    public VariableWeightedAverage(Void logInit, double logMin, double logMax) {
        if (!(Double.isFinite(logMin) && Double.isFinite(logMax))) {
            throw new IllegalArgumentException();
        }
        this.logMin = logMin;
        this.logMax = logMax;
        this.logRange = this.logMax - this.logMin;
    }

    @Override
    public int getMinInputs() {
        return 2;
    }

    @Override
    public int getMaxInputs() {
        return 256;
    }

    @Override
    public short calcOutput(List<SignalProvider> inputs) {
        double sum = 0;
        double sumWeight = 0;

        Short val = null;

        for (SignalProvider input : inputs) {
            if (val == null) {
                val = input.getOutput();
                continue;
            }

            double weight = Math.exp((input.getOutput() + ZEROIZE) * this.logRange / RANGE + this.logMin);
            sum += weight * val;
            sumWeight += weight;
            val = null;
        }
        if (val != null) throw new IllegalArgumentException("Odd number of inputs for VariableWeightedAverage: " + inputs.size());

        return roundClip(sum / sumWeight);
    }

    @Override
    public List<Param> getTweakingParams() {
        return TWEAKING_PARAMS;
    }

    @Override
    public VariableWeightedAverage tweak(short[] params) {
        double min = params[0],
               max = params[1];

        // wait until a larger positive value to offset by one, so small increments can be made at the lower values
        // see also Tweakable.transformByMagnitude methods
        if (min > HALF_MAX_PLUS_ONE) min++;
        if (max > HALF_MAX_PLUS_ONE) max++;

        // skip log de-transformation and re-transformation...
        // substitute *addition* (of log-transformed values) for *multiplication* (of original values)
        min = this.logMin + min * LOG4 / MAX_PLUS_ONE;
        max = this.logMax + max * LOG4 / MAX_PLUS_ONE;

        return new VariableWeightedAverage(null, min, max);
    }

    @Override
    public short[] getTweakingParams(VariableWeightedAverage toAchieve) {
        double minDiff = (toAchieve.logMin - this.logMin) * MAX_PLUS_ONE / LOG4;
        double maxDiff = (toAchieve.logMax - this.logMax) * MAX_PLUS_ONE / LOG4;

        if (minDiff > HALF_MAX_PLUS_ONE + 1) minDiff--;
        if (maxDiff > HALF_MAX_PLUS_ONE + 1) maxDiff--;

        return new short[] { roundClip(minDiff), roundClip(maxDiff) };
    }

    @Override
    public boolean pairedInputs() {
        return true;
    }
}
