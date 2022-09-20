package neuralNet.network;

import neuralNet.util.*;

import java.util.*;

public class SingleLineage implements Lineage {
    public static final long serialVersionUID = 1544874426661108741L;

    public final Lineage parentLineage;
    public final long myHash;

    private transient Long[] ancestors;

    public SingleLineage(Lineage parentLineage, long myHash) {
        if (parentLineage == null) throw new NullPointerException();
        this.parentLineage = parentLineage;
        this.myHash = myHash;
    }

    @Override
    public KinshipTracker recursiveSearch(Lineage otherLineage) {
        KinshipTracker tracker = this.parentLineage.recursiveSearch(otherLineage);
        tracker.sharedAncestors += otherLineage.lineageContains(this.myHash);
        tracker.generations++;
        return tracker;
    }


    @Override
    public double getGenerations() {
        return this.parentLineage.getGenerations() + 1.0;
    }

    @Override
    public double lineageContains(long hash) {
        return this.myHash == hash ? 1.0 : this.parentLineage.lineageContains(hash);
    }

    public static Lineage fromLegacyArray(long[] lineageLegacy, long myHash, Map<Long, Lineage> preexisting) {
        if (lineageLegacy.length == 0) {
            return preexisting.computeIfAbsent(myHash, RootLineage::new);
        }

        Lineage previous = preexisting.computeIfAbsent(lineageLegacy[lineageLegacy.length - 1], RootLineage::new);

        for (int i = lineageLegacy.length - 2; i >= 0; i--) {
            Lineage current = preexisting.get(lineageLegacy[i]);

            if (current == null) {
                current = new SingleLineage(previous, lineageLegacy[i]);
                preexisting.put(lineageLegacy[i], current);

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
        if (this.ancestors == null) makeAncestorsArray();
        return new UnmodifiableArrayIterator<>(this.ancestors);
    }

    @Override
    public int size() {
        if (this.ancestors == null) this.iterator();
        return this.ancestors.length;
    }

    private synchronized void makeAncestorsArray() {
        if (this.ancestors != null) return;
        LineageIterator iterator = new LineageIterator(this.parentLineage.iterator());
        this.ancestors = new Long[this.parentLineage.size() + 1];

        int i = 0;
        for (Long ancestor : iterator) {
            this.ancestors[i++] = ancestor;
        }
    }

    private class LineageIterator implements Iterator<Long>, Iterable<Long> {
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

        @Override
        public Iterator<Long> iterator() {
            return this;
        }
    }
}
