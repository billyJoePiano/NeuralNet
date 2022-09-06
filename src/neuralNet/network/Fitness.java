package neuralNet.network;

public interface Fitness<C extends DecisionConsumer<?, C, F>,
                         F extends Fitness<C, F>>
        extends Comparable<F> {

    public short getSignal();

    @Override
    public int compareTo(F other);

    public DecisionProvider<?, ?, C> getDecisionProvider();
    public long getGeneration(); // generation of the Fitness test, NOT the Net/DecisionProvider it was testing
}
