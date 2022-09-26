package neuralNet.evolve;

import neuralNet.network.*;
import neuralNet.neuron.*;
import neuralNet.util.*;

import java.util.*;

public abstract class CachingLineage<N extends DecisionProvider<?, N, ?>> implements Lineage {
    public final long myHash;
    public final long generation;
    public final SerializableWeakRef<N> net;

    private final transient double generationsCount;
    private transient long[] ancestors;
    private final transient Map<Lineage, KinshipCache> kinshipCache = new WeakHashMap<>();
    private transient String noSelf, withSelf;

    protected CachingLineage(N net, double generationsCount) {
        this.myHash = net.getNeuralHash();
        this.generation = net.getGeneration();
        this.generationsCount = generationsCount;
        this.net = new SerializableWeakRef<>(net);
    }

    protected record KinshipCache(double lineageContains, double kinshipScore) { }

    /**
     * For subclasses to implement their kinship calculation behavior.
     *
     * calcKinship(other) will only be called when other != null, other != this, and other.getHash() != this.getHash(),
     * so these checks do NOT need to be done within the implementations of calcKinship();
     *
     * This method only needs to be called once per 'other', in order to create the cache that will be used for
     * subsequent calls to getKinship() and getSharedAncestors(), etc...  Note that caches are NOT stored during
     * serialization, so are lazily re-calculated on an as-needed basis for each run of the application.
     *
     * @param other
     * @return
     */
    protected abstract KinshipCache calcKinship(Lineage other);
    protected abstract java.util.Iterator<Long> ancestorsIterator();
    protected abstract String toStringNoSelf();

    protected String toStringWithSelf() {
        String noSelf = this.toString(false);
        return noSelf.charAt(0) + NeuralHash.toHex(this.myHash) + ", " + noSelf.substring(1);
    }

    @Override
    public long getHash() {
        return this.myHash;
    }

    @Override
    public long getGeneration() {
        return this.generation;
    }

    @Override
    public double getGenerationsCount() {
        return this.generationsCount;
    }

    public N getNet() {
        if (this.net == null) return null;
        else return this.net.get();
    }

    @Override
    public String toString(boolean includeSelf) {
        if (includeSelf) {
            if (this.withSelf == null) this.withSelf = toStringWithSelf().intern();
            return this.withSelf;

        } else {
            if (this.noSelf == null) this.noSelf = toStringNoSelf().intern();
            return this.noSelf;
        }
    }

    @Override
    public String toString() {
        if (this.noSelf == null) this.noSelf = toStringNoSelf().intern();
        return this.noSelf;
    }

    @Override
    public Iterator iterator() {
        if (this.ancestors == null) makeAncestorsArray();
        return new Iterator();
    }

    @Override
    public long[] getAncestors() {
        if (this.ancestors == null) makeAncestorsArray();
        return this.ancestors.clone();
    }

    public double lineageContains(long hash) {
        if (this.myHash == hash) return 1.0;

    }

    @Override
    public double getKinshipScore(Lineage other) {
        if (other == this) return 1.0;
        return getOrMakeKinshipCache(other).kinshipScore;
    }

    protected final KinshipCache getOrMakeKinshipCache(Lineage other) {
        if (other == null) throw new NullPointerException();

        KinshipCache cache;
        synchronized (this.kinshipCache) {
            cache = this.kinshipCache.get(other);
        }
        if (cache == null) cache = makeAndVerifyCache(other);
        return cache;
    }

    private static WeakHashMap<Lineage, WeakHashMap<Lineage, KinshipCache>> noncachingCache = new WeakHashMap<>();
    protected static final KinshipCache getOrMakeKinshipCache(Lineage lineage1, Lineage lineage2) {
        if (lineage1 instanceof CachingLineage l1) {
            return l1.getOrMakeKinshipCache(lineage2);

        } else if (lineage2 instanceof CachingLineage l2) {
            return l2.getOrMakeKinshipCache(lineage1);
        }

        Lineage l1, l2;
        int compareTo = lineage1.compareTo(lineage2);
        if (compareTo < 0) {
            l1 = lineage1;
            l2 = lineage2;

        } else if (compareTo > 0) {
            l1 = lineage2;
            l2 = lineage1;

        } else throw new CannotResolveComparisonException(lineage1, lineage2);

        synchronized (l1) {
            return noncachingCache.computeIfAbsent(l1, l -> new WeakHashMap<>()).computeIfAbsent(l2, l -> {
                double lineageContains1 = l1.lineageContains(l2.getHash());
                double lineageContains2 = l2.lineageContains(l1.getHash());
                double kinshipScore1 = l1.getKinshipScore(l2);
                double kinshipScore2 = l2.getKinshipScore(l1);

                if (lineageContains1 != lineageContains2 || kinshipScore1 != kinshipScore2) {
                    throw new IllegalStateException("Inconsistent results from non-caching lineages: "
                            + lineageContains1 + " vs " + lineageContains2 + "\t\t"
                            + kinshipScore1 + " vs " + kinshipScore2
                            + "\n\t" + l1 + "\n\t" + l2);
                }

                return new KinshipCache(lineageContains1, kinshipScore1);
            });
        }

    }


    private void makeAncestorsArray() {
        synchronized (this) {
            if (this.ancestors != null) return;

            LinkedList<Long> list = new LinkedList<>();

            for (java.util.Iterator<Long> iterator = this.ancestorsIterator(); iterator.hasNext(); ) {
                list.add(iterator.next());
            }

            long[] arr = new long[list.size()];
            int i = 0;
            for (Long ancestor : list) {
                arr[i++] = ancestor;
            }

            this.ancestors = arr;
        }
    }

    private KinshipCache makeAndVerifyCache(Lineage other) {
        KinshipCache cache;

        if (!(other instanceof CachingLineage otherCacher)) {
            synchronized (this.kinshipCache) {
                cache = this.kinshipCache.get(other);
                if (cache != null) return cache;
                cache = this.calcKinship(other);
                if (cache == null) throw new NullPointerException();
                this.kinshipCache.put(other, cache);
            }

            double otherScore = other.getKinshipScore(this);
            if (cache.kinshipScore != otherScore) {
                throw new IllegalStateException("KINSHIPS SCORES DO NOT ALIGN: "
                        + cache.kinshipScore + " vs " + otherScore
                        + "\n\t"  + this + "\n\t" + other);
            }

            return cache;
        }

        CachingLineage<?> primary, secondary;
        // ensure that synchronization locks are always grabbed in the same order,
        // regardless of which instance is being invoked

        int compareTo = this.compareTo(other);
        if (compareTo < 0) {
            primary = this;
            secondary = otherCacher;

        } else if (compareTo > 0) {
            primary = otherCacher;
            secondary = this;

        } else throw new IllegalStateException();

        boolean verify;

        synchronized (primary.kinshipCache) {
            cache = primary.kinshipCache.get(secondary);
            if (cache == null)  {
                cache = primary.calcKinship(secondary);
                if (cache == null) throw new NullPointerException();
                primary.kinshipCache.put(secondary, cache);
                verify = true;

            } else verify = false; // means another thread made the cache first
        }

        if (!verify) {
            if (primary == this) return cache;

            // this instance was the secondary ... wait for this kinshipCache to populate from the other thread
            synchronized (this.kinshipCache) {
                while (true) {
                    cache = this.kinshipCache.get(other);
                    if (cache != null) return cache;
                    try { this.kinshipCache.wait(); }
                    catch (InterruptedException e) { e.printStackTrace(System.err); }
                }
            }
        }

        KinshipCache secondaryCache;

        synchronized (secondary.kinshipCache) {
            try {
                if (secondary.kinshipCache.get(primary) != null) throw new IllegalStateException();
                secondaryCache = secondary.calcKinship(primary);
                secondary.kinshipCache.put(primary, secondaryCache);

            } finally {
                secondary.kinshipCache.notifyAll();
            }
        }

        if (cache.kinshipScore != secondaryCache.kinshipScore) {
            throw new IllegalStateException("KINSHIPS SCORES DO NOT ALIGN: "
                    + cache.kinshipScore + " vs " + secondaryCache.kinshipScore
                    + "\n\t"  + primary + "\n\t" + secondary);
        }

        return primary == this ? cache : secondaryCache;
    }

    private class Iterator implements java.util.Iterator<Long> {
        private int index = -1;

        private Iterator() { }

        @Override
        public boolean hasNext() {
            return this.index < CachingLineage.this.ancestors.length;
        }

        @Override
        public Long next() {
            if (this.index == CachingLineage.this.ancestors.length) throw new NoSuchElementException();
            else if (this.index == -1) {
                this.index++;
                return CachingLineage.this.myHash;
            }
            return CachingLineage.this.ancestors[this.index++];
        }
    }
}
