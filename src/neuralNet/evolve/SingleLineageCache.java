package neuralNet.evolve;

import neuralNet.network.*;
import neuralNet.util.*;

import java.util.*;

public class SingleLineageCache<N extends DecisionProvider<?, N, ?>> extends CachingLineage<N> {
    public final Lineage parent;
    public final double divergence;

    public SingleLineageCache(N net, Lineage parent, double divergence) {
        super(net, parent.getGenerationsCount() + 1);
        this.parent = parent;
        this.divergence = divergence;
    }

    public SingleLineageCache(N net, N parent, double divergence) {
        this(net, parent.getLineage(), divergence);
    }

    @Override
    protected KinshipCache calcKinship(Lineage other) {
        double

        return new KinshipCache(this.parent.getSharedAncestors(other) + other.lineageContains(this.myHash),
                                this.parent.getKinshipScore(other) * this.divergence);
    }

    @Override
    protected Iterator<Long> ancestorsIterator() {
        return this.parent.iterator();
    }

    @Override
    protected String toStringNoSelf() {
        return this.parent.toString(true);
    }

    @Override
    public long getHash() {
        return this.myHash;
    }

    @Override
    public double lineageContains(long hash) {
        return this.myHash == hash ? 1.0 : this.parent.lineageContains(hash);
    }

    @Override
    public double getSharedAncestors(Lineage otherLineage, FuzzyPredicate<Lineage> filter) {
        return 0;
    }
}
