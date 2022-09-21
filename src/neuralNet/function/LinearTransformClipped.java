package neuralNet.function;

import neuralNet.neuron.*;

import java.lang.invoke.*;
import java.util.*;

import static neuralNet.evolve.Tweakable.*;
import static neuralNet.util.Util.*;

public class LinearTransformClipped implements NeuralFunction.Tweakable<LinearTransformClipped> {
    public static final List<Param> POS_PARAMS = List.of(Param.DEFAULT, Param.BOOLEAN_NEG, Param.DEFAULT);
    public static final List<Param> NEG_PARAMS = List.of(Param.DEFAULT, Param.BOOLEAN, Param.DEFAULT);

    @Override
    public long hashTweakMask() {
        return Double.doubleToLongBits(coefficient) ^ Double.doubleToLongBits(offset);
    }

    @Override
    public boolean sameBehavior(LinearTransformClipped other) {
        return this.coefficient == other.coefficient && this.offset == other.offset;
    }

    public static CachingNeuronUsingTweakableFunction makeNeuron(double coefficient, double offset) {
        return new CachingNeuronUsingTweakableFunction(new LinearTransformClipped(coefficient, offset));
    }

    public final double coefficient;
    public final double offset;

    public LinearTransformClipped(double coefficient, double offset) {
        if (!(coefficient > 0 || coefficient < 0)) throw new IllegalArgumentException(Double.toString(coefficient));
        this.coefficient = coefficient;
        this.offset = offset;
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
        return roundClip(this.coefficient * inputs.get(0).getOutput() + this.offset);
    }

    @Override
    public List<Param> getTweakingParams() {
        return coefficient > 0 ? POS_PARAMS : NEG_PARAMS;
    }

    @Override
    public LinearTransformClipped tweak(short[] params) {
        return new LinearTransformClipped(
                        transformByMagnitudeAndSign(this.coefficient, params[0], params[1]),
                                                    this.offset + params[2]);
    }

    @Override
    public short[] getTweakingParams(LinearTransformClipped toAchieve) {
        short[] magnitudeParams = toAchieveByMagnitudeAndSign(this.coefficient, toAchieve.coefficient);

        double offsetDiff = Math.round(toAchieve.offset - this.offset);
        if (offsetDiff > Short.MAX_VALUE) offsetDiff = Short.MAX_VALUE;
        else if (offsetDiff < Short.MIN_VALUE) offsetDiff = Short.MIN_VALUE;

        return new short[] { magnitudeParams[0], magnitudeParams[1], (short)offsetDiff };
    }

    public static final long HASH_HEADER = NeuralHash.HEADERS.get(MethodHandles.lookup().lookupClass());
    @Override
    public long hashHeader() {
        return HASH_HEADER;
    }
}
