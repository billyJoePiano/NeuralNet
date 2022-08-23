package neuralNet.network;

public interface Fitness<C extends DecisionConsumer<?, C, F>,
                         F extends Fitness<C, F>>
        extends Comparable<F> {

    public short getSignal();

    @Override
    public int compareTo(F other);
}
