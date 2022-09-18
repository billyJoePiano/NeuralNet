package neuralNet.neuron;

import java.util.concurrent.*;

public class RandomValueProvider extends CachingProvider {
    public static final int ORIGIN = Short.MIN_VALUE;
    public static final int BOUND = (int)Short.MAX_VALUE + 1;

    public static final long NEURAL_HASH = NeuralHash.HEADERS.get(RandomValueProvider.class);

    @Override
    public short calcOutput() {
        return (short)ThreadLocalRandom.current().nextInt(ORIGIN, BOUND);
    }

    public RandomValueProvider() { }

    public RandomValueProvider(RandomValueProvider cloneFrom) {
        super(cloneFrom);
    }

    @Override
    public RandomValueProvider clone() {
        return new RandomValueProvider(this);
    }

    @Override
    public long getNeuralHash() {
        return NEURAL_HASH;
    }

    @Override
    public long calcNeuralHash() {
        return NEURAL_HASH;
    }

    @Override
    public long getNeuralHashFor(LoopingNeuron looper) {
        return NEURAL_HASH;
    }
}
