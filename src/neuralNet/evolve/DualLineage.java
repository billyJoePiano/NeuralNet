package neuralNet.evolve;

import neuralNet.network.*;

import java.util.*;

public class DualLineage<N extends DecisionProvider<?, N, ?>> extends CachingLineage<N> {
    public final Lineage parent1, parent2;
    private transient double parentsKinshipModifier;

    public DualLineage(N net, Lineage parent1, Lineage parent2) {
        super(net, (parent1.getGenerationsCount() + parent2.getGenerationsCount()) / 2 + 1.0);
        this.parent1 = parent1;
        this.parent2 = parent2;
        this.parentsKinshipModifier = (1.0 + getOrMakeKinshipCache(parent1, parent2).kinshipScore()) / 2;
    }

    @Override
    protected KinshipCache calcKinship(Lineage other) {
        double sharedAncestors1 = parent1.getSharedAncestors(other);
        double sharedAncestors2 = parent2.getSharedAncestors(other);



        return (this.parent1.getSharedAncestors(other) + this.parent2.getSharedAncestors(other)) / 2
                + other.lineageContains(this.myHash);
    }


    protected double calcKinshipScore(Lineage other, double myGens, double otherGens, double sharedAncestors) {
        return Lineage.calcKinshipScore(sharedAncestors, myGens, otherGens);
    }

    @Override
    protected Iterator<Long> ancestorsIterator() {
        return new LineageIterator();
    }

    @Override
    protected String toStringNoSelf() {
        return "(" + this.parent1.toString(true) + ", " + this.parent2.toString(true) + ")";
    }

    @Override
    public long getHash() {
        return this.myHash;
    }

    @Override
    public double lineageContains(long hash) {
        return this.myHash == hash ? 1.0
                : (this.parent1.lineageContains(hash) + this.parent2.lineageContains(hash)) / 2;
    }

    private class LineageIterator implements Iterator<Long> {
        private byte state;
        private final Iterator<Long> parent1 = DualLineage.this.parent1.iterator();
        private final Iterator<Long> parent2 = DualLineage.this.parent2.iterator();

        private LineageIterator() {
            if (parent1.hasNext()) this.state = 1;
            else if (parent2.hasNext()) this.state = 4;
            else this.state = -1;
        }


        @Override
        public boolean hasNext() {
            return this.state != (byte) -1;
        }

        @Override
        public Long next() {
            Long hash;
            //alternate between parents' lineages
            switch (this.state) {
                case 1:
                    hash = this.parent1.next();
                    if (this.parent2.hasNext()) this.state = 2;
                    else if (this.parent1.hasNext()) this.state = 3;
                    else this.state = -1;
                    return hash;

                case 2:
                    hash = this.parent2.next();
                    if (this.parent1.hasNext()) this.state = 1;
                    else if (this.parent2.hasNext()) this.state = 4;
                    else this.state = -1;
                    return hash;

                case 3:
                    hash = this.parent1.next();
                    if (!this.parent1.hasNext()) this.state = -1;
                    return hash;

                case 4:
                    hash = this.parent2.next();
                    if (!this.parent2.hasNext()) this.state = -1;
                    return hash;

                case -1:
                    throw new NoSuchElementException();

                default:
                    throw new IllegalStateException();
            }
        }
    }
}
