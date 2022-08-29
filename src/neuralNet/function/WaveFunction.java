package neuralNet.function;

import static neuralNet.evolve.Tweakable.*;
import static neuralNet.neuron.StaticWaveProvider.*;

public interface WaveFunction {
    public double calc(double phasePosition);


    default public Param getMutationParam() {
        return WAVE_FUNCTION_PARAMS.get(WAVE_FUNCTIONS.indexOf(this));
    }

    default public short getMutationParam(WaveFunction toAchieve) {
        if (toAchieve == this) return 0;
        return (short)(WAVE_FUNCTIONS.indexOf(toAchieve) - WAVE_FUNCTIONS.indexOf(this));
    }

    default public WaveFunction mutate(short param) {
        return WAVE_FUNCTIONS.get(WAVE_FUNCTIONS.indexOf(this) + param);
    }

    default public int getIndex() {
        return WAVE_FUNCTIONS.indexOf(this);
    }
}
