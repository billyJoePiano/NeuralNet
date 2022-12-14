package neuralNet.function;

import neuralNet.neuron.*;

import java.util.*;

import static neuralNet.evolve.Tweakable.*;

public class LinearTransformCircular extends LinearTransformClipped {
    public static CachingNeuronUsingTweakableFunction makeNeuron(double coefficient, double offset) {
        return new CachingNeuronUsingTweakableFunction(new LinearTransformCircular(coefficient, offset));
    }

    public LinearTransformCircular(double coefficient, double offset) {
        super(coefficient, offset);
    }

    @Override
    public short calcOutput(List<SignalProvider> inputs) {
        double result = Math.round(this.coefficient * inputs.get(0).getOutput() + this.offset);
        return (short) result;
    }

    @Override
    public LinearTransformCircular tweak(short[] params) {
        return new LinearTransformCircular(
                transformByMagnitudeAndSign(this.coefficient, params[0], params[1]),
                this.offset + params[2]);
    }
}
