package neuralNet.network;

import java.util.*;

public record DualLineage(Lineage parent1, Lineage parent2, long myHash) implements Lineage {
    public DualLineage {
        if (parent1 == null || parent2 == null) throw new NullPointerException();
    }

    @Override
    public KinshipTracker recursiveSearch(Lineage otherLineage) {
        KinshipTracker tracker1 = this.parent1.cachingRecursiveSearch(otherLineage);
        KinshipTracker tracker2 = this.parent2.cachingRecursiveSearch(otherLineage);
        tracker1.generations = (tracker1.generations + tracker2.generations) / 2;
        tracker1.sharedAncestors = (tracker1.sharedAncestors + tracker2.generations) / 2;

        return tracker1;
    }


    @Override
    public double getGenerations() {
        return (this.parent1.getGenerations() + this.parent2.getGenerations()) / 2 + 1;
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

    @Override
    public Iterator<Long> iterator() {
        return new LineageIterator(this.parent1.iterator(), this.parent2.iterator());
    }

    private class LineageIterator implements Iterator<Long> {
        private byte state = 0;
        private final Iterator<Long> parent1;
        private final Iterator<Long> parent2;

        private LineageIterator(Iterator<Long> parent1, Iterator<Long> parent2) {
            this.parent1 = parent1;
            this.parent2 = parent2;
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
                case 0:
                    if (this.parent1.hasNext()) this.state = 1;
                    else if (this.parent2.hasNext()) this.state = 4;
                    return DualLineage.this.myHash;

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
