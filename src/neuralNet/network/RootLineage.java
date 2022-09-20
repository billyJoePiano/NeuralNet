package neuralNet.network;

import java.util.*;

public record RootLineage(long myHash) implements Lineage {

    public static final long serialVersionUID = -8171313921074150405L;

    @Override
    public KinshipTracker recursiveSearch(Lineage otherLineage) {
        return new KinshipTracker(1, otherLineage.lineageContains(this.myHash));
    }

    @Override
    public double getGenerations() {
        return 1.0;
    }

    @Override
    public double lineageContains(long hash) {
        return hash == this.myHash ? 1.0 : 0.0;
    }

    @Override
    public LineageIterator iterator() {
        return new LineageIterator();
    }

    public class LineageIterator implements Iterator<Long> {
        private boolean providedOwnHash = false;

        @Override
        public boolean hasNext() {
            return !this.providedOwnHash;
        }

        @Override
        public Long next() {
            if (this.providedOwnHash) throw new NoSuchElementException();
            this.providedOwnHash = true;
            return RootLineage.this.myHash;
        }
    }

    public int size() {
        return 1;
    }
}
