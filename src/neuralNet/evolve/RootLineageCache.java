package neuralNet.evolve;

import neuralNet.network.*;
import neuralNet.neuron.*;

import java.util.*;

public class RootLineageCache<N extends DecisionProvider<?, N, ?>> extends CachingLineage<N> {

    protected RootLineageCache(N net) {
        super(net, 1);
    }

    @Override
    protected String toStringWithSelf() {
        return "[" + NeuralHash.toHex(this.myHash) + "]";
    }

    @Override
    protected String toStringNoSelf() {
        return "[]";
    }

    @Override
    public double lineageContains(long hash) {
        return hash == this.myHash ? 1.0 : 0.0;
    }

    @Override
    protected double searchForSharedAncestors(Lineage other) {
        return other.lineageContains(this.myHash);
    }

    @Override
    protected double calcKinshipScore(Lineage other, double myGens, double otherGens, double sharedAncestors) {
        return Lineage.calcKinshipScore(sharedAncestors, myGens, otherGens);
    }

    @Override
    protected Iterator<Long> ancestorsIterator() {
        return Collections.emptyIterator();
    }
}
