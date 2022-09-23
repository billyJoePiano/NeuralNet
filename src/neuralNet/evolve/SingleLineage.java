package neuralNet.evolve;

import java.util.*;

public record SingleLineage(Lineage parentLineage, long myHash) implements Lineage {
    public static final long serialVersionUID = 1544874426661108741L;

    public SingleLineage {
        if (parentLineage == null) throw new NullPointerException();
    }

    @Override
    public KinshipTracker recursiveSearch(Lineage otherLineage) {
        KinshipTracker tracker = this.parentLineage.cachingRecursiveSearch(otherLineage);
        tracker.sharedAncestors += otherLineage.lineageContains(this.myHash);
        tracker.generations++;
        return tracker;
    }


    @Override
    public double getGenerations() {
        return this.parentLineage.getGenerations() + 1.0;
    }

    @Override
    public long getHash() {
        return this.myHash;
    }

    @Override
    public double lineageContains(long hash) {
        return this.myHash == hash ? 1.0 : this.parentLineage.lineageContains(hash);
    }

    public static Lineage fromLegacyArray(long[] lineageArray, long myHash, Map<Long, Lineage> preexisting) {
        if (lineageArray.length == 0) {
            return preexisting.computeIfAbsent(myHash, RootLineage::new);
        }

        Lineage previous = preexisting.computeIfAbsent(lineageArray[lineageArray.length - 1], RootLineage::new);

        for (int i = lineageArray.length - 2; i >= 0; i--) {
            Lineage current = preexisting.get(lineageArray[i]);

            if (current == null) {
                current = new SingleLineage(previous, lineageArray[i]);
                preexisting.put(lineageArray[i], current);

            } else if (!(current instanceof SingleLineage sl && sl.parentLineage == previous)) {
                throw new IllegalStateException();
            }

            previous = current;
        }

        Lineage prev = previous;
        return preexisting.computeIfAbsent(myHash, mh -> new SingleLineage(prev, mh));
    }

    @Override
    public Iterator<Long> iterator() {
        return new LineageIterator(this.parentLineage.iterator());
    }

    private class LineageIterator implements Iterator<Long> {
        private boolean providedOwnHash = false;
        private final Iterator<Long> parent;

        private LineageIterator(Iterator<Long> parent) {
            this.parent = parent;
        }

        @Override
        public boolean hasNext() {
            return !this.providedOwnHash || this.parent.hasNext();
        }

        @Override
        public Long next() {
            if (this.providedOwnHash) return this.parent.next();
            this.providedOwnHash = true;
            return SingleLineage.this.myHash;
        }
    }
}
