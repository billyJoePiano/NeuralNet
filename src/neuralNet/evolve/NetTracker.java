package neuralNet.evolve;

import neuralNet.network.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;

public class NetTracker<N extends NeuralNet<?, N, ?>, F extends Fitness<?, F>> implements Set<N> {
    public static final KeepLambda DEFAULT_KEEP_LAMBDA = DefaultKeepLambda.INSTANCE;
    public static final UpdateFittestLambda
            DISCARD_IMMEDIATELY = DefaultUpdateFittestLambdas.DISCARD_IMMEDIATELY,
            HALF = DefaultUpdateFittestLambdas.HALF,
            THIRD = DefaultUpdateFittestLambdas.THIRD,
            QUARTER = DefaultUpdateFittestLambdas.QUARTER,
            DEFAULT_UPDATE_FITTEST_LAMBDA = THIRD;

    private final TreeSet<LegacyRecord> legacies = new TreeSet<>();
    public final KeepLambda<N> keep;
    public final UpdateFittestLambda<N> updateFittest;

    private transient HashMap<N, LegacyRecord> nets = new HashMap<>();;
    private transient TreeSet<F> fitnesses = new TreeSet<>();
    private transient NavigableSet<F> fitnessesView = Collections.unmodifiableNavigableSet(this.fitnesses);

    private Object readResolve() throws ObjectStreamException {
        this.nets = new HashMap<>();
        this.fitnessesView = Collections.unmodifiableNavigableSet(this.fitnesses);

        for (LegacyRecord legacy : this.legacies) {
            this.nets.put(legacy.net, legacy);
        }
        return this;
    }

    /*
    private NetTracker(NetTracker<N, F> deserializedFrom, Void v) {
        this.legacies = deserializedFrom.legacies;
        this.keep = deserializedFrom.keep;
        this.updateFittest = deserializedFrom.updateFittest;
        this.nets = new HashMap<>();

        for (LegacyRecord legacy : this.legacies) {
            this.nets.put(legacy.net, legacy);
        }
    }
     */

    public NetTracker() {
        this(DEFAULT_KEEP_LAMBDA, DEFAULT_UPDATE_FITTEST_LAMBDA);
    }

    public NetTracker(KeepLambda<N> keepLambda) {
        this(keepLambda, DEFAULT_UPDATE_FITTEST_LAMBDA);
    }

    public NetTracker(UpdateFittestLambda<N> updateFittest) {
        this(DEFAULT_KEEP_LAMBDA, updateFittest);
    }

    public NetTracker(KeepLambda<N> keepLambda, UpdateFittestLambda<N> updateFittestLambda) {
        if (keepLambda == null || updateFittestLambda == null) throw new NullPointerException();
        this.keep = keepLambda;
        this.updateFittest = updateFittestLambda;
    }

    public interface KeepLambda<N extends NeuralNet<?, N, ?>> {
        public boolean keep(long currentGen, long genRating, N net);
    }

    public interface UpdateFittestLambda<N extends NeuralNet<?, N , ?>> {
        public long updateGenRating(long currentGen, long genRating, N net);
    }

    public enum DefaultKeepLambda implements KeepLambda {
        INSTANCE;

        @Override
        public boolean keep(long currentGen, long genRating, NeuralNet net) {
            return currentGen - genRating <= genRating - net.generation;
        }
    }

    public enum DefaultUpdateFittestLambdas implements UpdateFittestLambda {
        //Amount the genRating variable is incremented each time a bet makes the fittest set
        DISCARD_IMMEDIATELY(0), //remove from legacy as soon as it doesn't make the fittest set, based on the current keepLambda
        HALF(1), // it would have to be judged fittest 1/2 of the time to remain in the legacy set, based on the default keepLambda
        THIRD(2), // ...third of the time
        QUARTER(3); //... 1/4 of the time

        private final long addWhenFittest;

        DefaultUpdateFittestLambdas(long addWhenFittest) {
            this.addWhenFittest = addWhenFittest;
        }

        @Override
        public long updateGenRating(long currentGen, long genRating, NeuralNet net) {
            return genRating + this.addWhenFittest;
        }
    }


    public class LegacyRecord implements Comparable<LegacyRecord> {
        public final N net;
        private long genRating = NeuralNet.getCurrentGeneration();
        private transient Fitness<?, ?> lastFitness;

        private LegacyRecord(N net, Fitness<?, ?> lastFitness) {
            this.net = net;
            this.lastFitness = lastFitness;
        }


        @Override
        public int compareTo(LegacyRecord other) {
            if (this.genRating != other.genRating) return Long.compare(this.genRating, other.genRating);
            if (this == other) return 0;

            if (this.lastFitness == null) return 1;
            if (other.lastFitness == null) return -1;

            int mine = this.hashCode();
            int theirs = other.hashCode();
            if (mine != theirs) return Integer.compare(this.hashCode(), other.hashCode());

            if (this.net == other.net) throw new IllegalStateException();
            mine = this.net.hashCode();
            theirs = other.net.hashCode();
            if (mine != theirs) return Integer.compare(this.hashCode(), other.hashCode());

            if (this.lastFitness == other.lastFitness) throw new IllegalStateException();
            mine = this.lastFitness.hashCode();
            theirs = other.lastFitness.hashCode();
            if (mine != theirs) return Integer.compare(this.hashCode(), other.hashCode());

            ThreadLocalRandom rand = ThreadLocalRandom.current();
            return rand.nextBoolean() ? -1 : 1;
        }
    }

    public NavigableSet<F> getFitnesses() {
        return this.fitnessesView;
    }

    public boolean add(N net) {
        LegacyRecord legacy = this.nets.get(net);
        if (legacy != null) return false;

        legacy = new LegacyRecord(net, null);
        this.nets.put(net, legacy);
        legacies.add(legacy);
        return true;
    }


    public boolean addAll(Collection<? extends N> nets) {
        boolean changed = false;
        for (N net : nets) {
            if (net == null) throw new NullPointerException();
            if (this.nets.containsKey(net)) continue;
            changed = true;
            LegacyRecord legacy = new LegacyRecord(net, null);
            this.legacies.add(legacy);
            this.nets.put(net, legacy);
        }
        return changed;
    }

    public Long getGenRating(N net) {
        LegacyRecord legacy = this.nets.get(net);
        if (legacy == null) return null;
        return legacy.genRating;
    }

    public Fitness getFitness(N net) {
        LegacyRecord legacy = this.nets.get(net);
        if (legacy == null) return null;
        return legacy.lastFitness;
    }

    public boolean setGenRatingAndFitness(N net, long genRating, F fitness) {
        if (net == null || fitness == null) throw new NullPointerException();
        if (net != fitness.getDecisionProvider()) throw new IllegalStateException();

        LegacyRecord legacy = this.nets.get(net);
        if (legacy == null) {
            legacy = new LegacyRecord(net, fitness);
            legacy.genRating = genRating;
            this.nets.put(net, legacy);
            this.legacies.add(legacy);
            this.fitnesses.add(fitness);
            return true;

        }

        boolean changed = false;
        if (legacy.genRating != genRating) {
            changed = true;
            legacies.remove(legacy);
            legacy.genRating = genRating;
            legacies.add(legacy);
        }

        if (legacy.lastFitness != fitness) {
            changed = true;
            this.fitnesses.remove(legacy.lastFitness);
            legacy.lastFitness = fitness;
            this.fitnesses.add(fitness);
        }

        return changed;
    }

    public boolean setGenRating(N net, long genRating) {
        LegacyRecord legacy = this.nets.get(net);
        if (legacy == null) {
            legacy = new LegacyRecord(net, null);
            legacy.genRating = genRating;
            this.nets.put(net, legacy);
            legacies.add(legacy);
            return true;

        } else if (legacy.genRating != genRating) {
            legacies.remove(legacy);
            legacy.genRating = genRating;
            legacies.add(legacy);
            return true;

        } else return false;
    }

    public boolean setFitness(N net, F fitness) {
        if (net == null || fitness == null) throw new NullPointerException();
        LegacyRecord legacy = this.nets.get(net);
        if (legacy == null) {
            legacy = new LegacyRecord(net, fitness);
            this.legacies.add(legacy);
            this.nets.put(net, legacy);
            this.fitnesses.add(fitness);
            return true;

        } else if (fitness != legacy.lastFitness) {
            this.fitnesses.remove(legacy.lastFitness);
            legacy.lastFitness = fitness;
            this.fitnesses.add(fitness);
            return true;
        }

        return false;
    }

    /*
    /** For when the set of fittest net is already determined
     *
     * @param nets
     * @return
     *//*
    public boolean judgedFittest(Collection<? extends N> nets) {
        return judgedFittest(nets, NeuralNet.getCurrentGeneration());
    }


    /** For when the set of fittest net is already determined
     *
     * @param nets
     * @return
     *//*
    public boolean judgedFittest(Collection<? extends N> nets, long gen) {
        boolean changed = false;
        for (N net : nets) {
            if (net == null) throw new NullPointerException();
            LegacyRecord legacy = this.nets.get(net);
            if (legacy == null) {
                changed = true;
                legacy = new LegacyRecord(net, null);
                this.legacies.add(legacy);
                this.nets.put(net, legacy);

            } else {
                long genRating = this.updateFittest.updateGenRating(NeuralNet.getCurrentGeneration(), legacy.genRating, legacy.net);
                if (genRating > gen) genRating = gen;
                if (genRating == legacy.genRating) continue;
                changed = true;
                this.legacies.remove(legacy);
                legacy.genRating = genRating;
                this.legacies.add(legacy);
            }
        }
        return changed;
    }
    */


    public Set<N> addFittest(SortedSet<? extends F> fitnesses, int keepTop) {
        return addFittest(fitnesses, keepTop, NeuralNet.getCurrentGeneration());
    }

    /**
     * Adds the fittest nets as determined by keepTop, and updates all of the fitness records for existing
     * legacy nets that are already held by this tracker even if they are not in the fittest group.  Unrecognized
     * nets which are not in the fittest group will be ignored.
     *
     * @param fitnesses the full set of sorted fitness record to select the best nets from, and to update records
     *                  for any existing legacy nets
     * @param keepTop the number of fittest nets to record and return
     * @return the set of the top nets, with size determined by keepTop
     */
    public Set<N> addFittest(SortedSet<? extends F> fitnesses, int keepTop, long currentGen) {
        for (F fittness : fitnesses) {
            LegacyRecord legacy = this.nets.get(fittness.getDecisionProvider());
            if (legacy == null || legacy.lastFitness == fittness) continue;
            if (legacy.lastFitness != null) this.fitnesses.remove(legacy.lastFitness);
            this.fitnesses.add(fittness);
            legacy.lastFitness = fittness;
        }


        Iterator<? extends F> newF = fitnesses.iterator();
        Iterator<? extends F> oldF = this.fitnesses.iterator();

        F n = null, o = null;

        List<F> toAdd = new ArrayList<>(keepTop);
        Set<N> toReturn = new LinkedHashSet<>(keepTop);
        int kept = 0;

        while (kept < keepTop) {
            if (n == null) {
                if (newF.hasNext()) n = newF.next();
                else break;
            }

            if (o == null) {
                if (oldF.hasNext()) o = oldF.next();
                else break;
            }

            if (n == null || o == null) throw new NullPointerException();

            if (((n == o) && (n = null) == null) || (n.compareTo(o) > 0)){
                N net = (N)o.getDecisionProvider();
                if (net == null) throw new NullPointerException();

                kept++;
                toReturn.add(net);

                LegacyRecord legacy = this.nets.get(net);

                long genRating = this.updateFittest.updateGenRating(currentGen, legacy.genRating, legacy.net);
                if (genRating > currentGen) genRating = currentGen;
                if (genRating != legacy.genRating) {
                    this.legacies.remove(legacy);
                    legacy.genRating = genRating;
                    this.legacies.add(legacy);
                }

                o = null;
                continue;
            }

            N net = (N)n.getDecisionProvider();
            if (net == null) throw new NullPointerException();
            if (this.nets.containsKey(net)) throw new IllegalStateException();

            kept++;
            toAdd.add(n);
            toReturn.add(net);
            LegacyRecord legacy = new LegacyRecord(net, n);

            this.legacies.add(legacy);
            this.nets.put(net, legacy);
            n = null;
        }

        this.fitnesses.addAll(toAdd);

        if (toReturn.size() != keepTop) throw new IllegalStateException();

        return toReturn;
    }

    public void cullOld() {
        this.cullOld(NeuralNet.getCurrentGeneration());
    }

    public void cullOld(long currentGen) {
        for (Iterator<LegacyRecord> iterator = legacies.iterator();
             iterator.hasNext();) {

            LegacyRecord legacy = iterator.next();
            if (this.keep.keep(currentGen, legacy.genRating, legacy.net)) continue;
            iterator.remove();
            this.nets.remove(legacy.net);
            if(legacy.lastFitness != null) this.fitnesses.remove(legacy.lastFitness);
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
        return this.nets.containsKey(o);
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
        for (LegacyRecord legacy : this.legacies) {
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
        for (LegacyRecord legacy : this.legacies) {
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
        LegacyRecord legacy = this.nets.get(o);
        if (legacy == null) return false;
        this.legacies.remove(legacy);
        this.nets.remove(o);
        return true;
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return this.nets.keySet().containsAll(collection);
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        boolean changed = this.nets.keySet().retainAll(collection);
        if (changed) {
            this.legacies.retainAll(this.nets.values());
        }
        return changed;
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        boolean changed = this.nets.keySet().removeAll(collection);
        if (changed) {
            this.legacies.retainAll(this.nets.values());
        }
        return changed;
    }

    @Override
    public boolean removeIf(Predicate<? super N> filter) {
        boolean changed = false;
        for (Iterator<LegacyRecord> iterator = this.legacies.iterator();
             iterator.hasNext();) {

            LegacyRecord legacy = iterator.next();
            if (!filter.test(legacy.net)) continue;
            changed = true;
            iterator.remove();
            this.nets.remove(legacy.net);
        }
        return changed;
    }

    @Override
    public void clear() {
        this.legacies.clear();
        this.nets.clear();
    }

    public Map<N, Long> toMap() {
        Map<N, Long> map = new LinkedHashMap<>(this.legacies.size());

        for (LegacyRecord legacy : this.legacies) {
            map.put(legacy.net, legacy.genRating);
        }
        return map;
    }

    public class NetsIterator implements java.util.Iterator<N> {
        private final java.util.Iterator<LegacyRecord> legacyIterator = NetTracker.this.legacies.iterator();

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

    List<LegacyRecord> findNetsLegacyDiscrepancy() {
        return this.nets.values().stream().filter(l -> !this.legacies.contains(l)).collect(Collectors.toList());
    }

    List<N> findNetsFitnessDiscrepancy() {
        List<N> fitnessNets = this.fitnesses.stream().map(f -> (N) f.getDecisionProvider()).toList();
        return this.nets.keySet().stream().filter(n -> !fitnessNets.contains(n)).collect(Collectors.toList());
    }

    List<N> findFitnessLegacyDiscrepancy() {
        List<N> legacyNets = this.legacies.stream().map(l -> l.net).toList();
        return this.fitnesses.stream().map(f -> (N)f.getDecisionProvider()).filter(n -> !legacyNets.contains(n)).collect(Collectors.toList());
    }

    List<N> findFitnessNetsDiscrepancy() {
        return this.fitnesses.stream().map(f -> (N)f.getDecisionProvider()).filter(n -> !this.nets.containsKey(n)).collect(Collectors.toList());
    }

    List<N> findLegacyFitnessDiscrepancy() {
        List<N> fitnessNets = this.fitnesses.stream().map(f -> (N) f.getDecisionProvider()).toList();
        return this.legacies.stream().map(l -> l.net).filter(n -> !fitnessNets.contains(n)).collect(Collectors.toList());
    }

    List<LegacyRecord> findLegacyNetsDiscrepancy() {
        return this.legacies.stream().filter(l -> !this.nets.values().contains(l)).toList();
    }
}
