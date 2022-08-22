package neuralNet.function;

import neuralNet.neuron.*;

import java.util.concurrent.*;

public class RandomValue implements FunctionNoInputs {
    public static final int ORIGIN = Short.MIN_VALUE;
    public static final int BOUND = (int)Short.MAX_VALUE + 1;

    public static final RandomValue instance = new RandomValue();

    public static CachingProviderUsingFunction makeNeuron() {
        return new CachingProviderUsingFunction(instance);
    }

    private RandomValue() { }

    @Override
    public short calcOutput() {
        return (short)ThreadLocalRandom.current().nextInt(ORIGIN, BOUND);
    }
}
