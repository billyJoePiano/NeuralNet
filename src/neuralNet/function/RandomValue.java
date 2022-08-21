package neuralNet.function;

import neuralNet.neuron.*;

import java.util.*;
import java.util.concurrent.*;

public class RandomValue implements StatelessFunction {
    public static final int ORIGIN = Short.MIN_VALUE;
    public static final int BOUND = (int)Short.MAX_VALUE + 1;

    public static final RandomValue instance = new RandomValue();

    public static CachingNeuronUsingStatelessFunction makeNeuron() {
        return new CachingNeuronUsingStatelessFunction(instance);
    }

    private RandomValue() { }

    @Override
    public short calcOutput(List<SignalProvider> inputs) {
        return (short)ThreadLocalRandom.current().nextInt(ORIGIN, BOUND);
    }
}
