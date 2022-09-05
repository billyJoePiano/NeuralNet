package neuralNet.evolve;

import neuralNet.network.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

public class LegacyNetTracker<N extends NeuralNet<?, N, ?>> implements Set<N> {
    public static final KeepLambda DEFAULT_KEEP_LAMBDA = DefaultKeepLambda.INSTANCE;
    public static final UpdateFittestLambda
            DISCARD_IMMEDIATELY = DefaultUpdateFittestLambdas.DISCARD_IMMEDIATELY,
            HALF = DefaultUpdateFittestLambdas.HALF,
            THIRD = DefaultUpdateFittestLambdas.THIRD,
            QUARTER = DefaultUpdateFittestLambdas.QUARTER,
            DEFAULT_UPDATE_FITTEST_LAMBDA = THIRD;


    private final TreeSet<Legacy> legacies;

    private transient final HashMap<N, Legacy> byNet;

    public final KeepLambda<N> keep;
    public final UpdateFittestLambda<N> updateFittest;

    public LegacyNetTracker() {
        this(DEFAULT_KEEP_LAMBDA, DEFAULT_UPDATE_FITTEST_LAMBDA);
    }

    public LegacyNetTracker(KeepLambda<N> keepLambda) {
        this(keepLambda, DEFAULT_UPDATE_FITTEST_LAMBDA);
    }

    public LegacyNetTracker(UpdateFittestLambda<N> updateFittest) {
        this(DEFAULT_KEEP_LAMBDA, updateFittest);
    }

    public LegacyNetTracker(KeepLambda<N> keepLambda, UpdateFittestLambda<N> updateFittestLambda) {
        if (keepLambda == null || updateFittestLambda == null) throw new NullPointerException();
        this.keep = keepLambda;
        this.updateFittest = updateFittestLambda;
        this.legacies = new TreeSet<>();
        this.byNet = new HashMap<>();
    }

    public interface KeepLambda<N extends NeuralNet<?, N, ?>> {
        public boolean keep(long currentGen, long internedGen, N net);
    }

    public interface UpdateFittestLambda<N extends NeuralNet<?, N , ?>> {
        public long updateInterned(long currentGen, long internedGen, N net);
    }

    public enum DefaultKeepLambda implements KeepLambda {
        INSTANCE;

        @Override
        public boolean keep(long currentGen, long internedGen, NeuralNet net) {
            return currentGen - internedGen <= internedGen - net.generation;
        }
    }

    public enum DefaultUpdateFittestLambdas implements UpdateFittestLambda {
        DISCARD_IMMEDIATELY(0),
        HALF(1),
        THIRD(2),
        QUARTER(3);

        private final long addWhenFittest;

        DefaultUpdateFittestLambdas(long addWhenFittest) {
            this.addWhenFittest = addWhenFittest;
        }

        @Override
        public long updateInterned(long currentGen, long internedGen, NeuralNet net) {
            return currentGen + addWhenFittest;
        }
    }


    private class Legacy implements Comparable<Legacy> {
        private final N net;
        private long interned = NeuralNet.getCurrentGeneration();

        private Legacy(N net) {
            this.net = net;
        }


        @Override
        public int compareTo(Legacy other) {
            return Long.compare(this.interned, other.interned);
        }
    }

    public boolean add(N net) {
        if (byNet.containsKey(net)) return false;
        Legacy legacy = new Legacy(net);
        this.legacies.add(legacy);
        this.byNet.put(net, legacy);
        return true;
    }

    public boolean addAll(Collection<? extends N> nets) {
        boolean changed = false;
        for (N net : nets) {
            if (byNet.containsKey(net)) continue;
            changed = true;
            Legacy legacy = new Legacy(net);
            this.legacies.add(legacy);
            this.byNet.put(net, legacy);
        }
        return changed;
    }

    public Long getInterned(N net) {
        Legacy legacy = this.byNet.get(net);
        if (legacy == null) return null;
        return legacy.interned;
    }

    public boolean updateFittest(N net) {
        Legacy legacy = this.byNet.get(net);
        if (legacy == null) return false;
        legacy.interned = this.updateFittest.updateInterned(NeuralNet.getCurrentGeneration(), legacy.interned, net);
        return true;
    }

    public void updateFittest(Collection<N> nets) {
        for (N net : nets) {
            Legacy legacy = this.byNet.get(net);
            if (legacy == null) continue;
            legacy.interned = this.updateFittest.updateInterned(NeuralNet.getCurrentGeneration(), legacy.interned, net);
        }
    }

    public void prune() {
        this.prune(NeuralNet.getCurrentGeneration());
    }

    public void prune(long currentGen) {
        for (Iterator<Legacy> iterator = legacies.iterator();
             iterator.hasNext();) {

            Legacy legacy = iterator.next();
            if (this.keep.keep(currentGen, legacy.interned, legacy.net)) continue;
            iterator.remove();
            this.byNet.remove(legacy.net);
        }
    }

    

    @Override
    public int size() {
        return this.legacies.size();
    }

    @Override
    public boolean isEmpty() {
        return this.legacies.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return this.byNet.containsKey(o);
    }

    @Override
    public Iterator<N> iterator() {
        return new NetsIterator();
    }

    @Override
    public void forEach(Consumer<? super N> action) {
        this.legacies.forEach(l -> action.accept(l.net));
    }

    @Override
    public Object[] toArray() {
        Object[] arr = new Object[this.legacies.size()];
        int i = 0;
        for (Legacy legacy : this.legacies) {
            arr[i++] = legacy.net;
        }
        return arr;
    }

    @Override
    public <T> T[] toArray(T[] ts) {
        if (ts.length < this.legacies.size()) {
            ts = (T[])Array.newInstance(ts.getClass().getComponentType(), this.legacies.size());
        }

        int i = 0;
        for (Legacy legacy : this.legacies) {
            ts[i++] = (T)legacy.net;
        }

        for (; i < ts.length; i++) {
            ts[i] = null;
        }

        return ts;
    }

    @Override
    public <T> T[] toArray(IntFunction<T[]> generator) {
        return this.toArray(generator.apply(this.legacies.size()));
    }

    @Override
    public boolean remove(Object o) {
        Legacy legacy = this.byNet.get(o);
        if (legacy == null) return false;
        this.legacies.remove(legacy);
        this.byNet.remove(o);
        return true;
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return this.byNet.keySet().containsAll(collection);
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        boolean changed = this.byNet.keySet().retainAll(collection);
        if (changed) {
            this.legacies.retainAll(this.byNet.values());
        }
        return changed;
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        boolean changed = this.byNet.keySet().removeAll(collection);
        if (changed) {
            this.legacies.retainAll(this.byNet.values());
        }
        return changed;
    }

    @Override
    public boolean removeIf(Predicate<? super N> filter) {
        boolean changed = false;
        for (Iterator<Legacy> iterator = this.legacies.iterator();
                iterator.hasNext();) {

            Legacy legacy = iterator.next();
            if (!filter.test(legacy.net)) continue;
            changed = true;
            iterator.remove();
            this.byNet.remove(legacy.net);
        }
        return changed;
    }

    @Override
    public void clear() {
        this.legacies.clear();
        this.byNet.clear();
    }

    public class NetsIterator implements java.util.Iterator<N> {
        private final java.util.Iterator<Legacy> legacyIterator = LegacyNetTracker.this.legacies.iterator();

        private NetsIterator() { }

        @Override
        public boolean hasNext() {
            return this.legacyIterator.hasNext();
        }

        @Override
        public N next() {
            return this.legacyIterator.next().net;
        }
    }
}
