package neuralNet.function;

import neuralNet.neuron.*;

import java.util.concurrent.*;

public enum RandomValue implements FunctionNoInputs {
    INSTANCE;

    public static final int ORIGIN = Short.MIN_VALUE;
    public static final int BOUND = (int)Short.MAX_VALUE + 1;

    public static CachingProviderUsingFunction makeNeuron() {
        return new CachingProviderUsingFunction(INSTANCE);
    }

    @Override
    public short calcOutput() {
        return (short)ThreadLocalRandom.current().nextInt(ORIGIN, BOUND);
    }
}
