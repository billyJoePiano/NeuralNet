package neuralNet.network;

import neuralNet.neuron.*;

public interface DecisionNode<P extends DecisionProvider<?, P, C>,
                                //N extends DecisionNode<P, N, C>,
                                C extends DecisionConsumer<?, C, ?>>

        extends SignalConsumer, Comparable<DecisionNode<?, C>> {


    public P getDecisionProvider();

    public int getDecisionId();
    default public short getWeight() {
        return this.getInputs().get(0).getOutput();
    }

    @Override
    default public int getMinInputs() {
        return 1;
    }

    @Override
    default public int getMaxInputs() {
        return 1;
    }

    @Override
    default public int compareTo(DecisionNode<?, C> other) {
        if (other == null) return -1;
        if (other == this) return 0;

        short myWeight = this.getWeight();
        short otherWeight = other.getWeight();

        if (myWeight > otherWeight) return -1;
        else if (otherWeight > myWeight) return 1;


        //seemingly random, especially with respect to the order in which objects were instantiated
        // (e.g. hashCode on its own may tend to get incrementally larger when many objects are created sequentially, depending on the JVM)
        // AND reproducible without needing to store the output
        int myMask =  Integer.reverse(this.hashCode())
                    ^ Integer.reverse(this.getClass().hashCode())
                    ^ Integer.reverse(this.getDecisionProvider().hashCode())
                    ^ Integer.reverse((int)this.getDecisionProvider().getRound());

        int otherMask = Integer.reverse(other.hashCode())
                    ^ Integer.reverse(other.getClass().hashCode())
                    ^ Integer.reverse(other.getDecisionProvider().hashCode())
                    ^ Integer.reverse((int)other.getDecisionProvider().getRound());

        return myMask < otherMask ? -1 : 1;
    }
}
