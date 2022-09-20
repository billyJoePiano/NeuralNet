package neuralNet.evolve;

import neuralNet.network.*;
import neuralNet.neuron.*;
import neuralNet.util.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import static neuralNet.neuron.NeuralHash.toHex;

public class NetTracker<N extends NeuralNet<?, N, ?>, F extends Fitness<?, F>> implements Set<N>, Serializable {
    public static final long serialVersionUID = 7073140645268169066L;

    public static final KeepLambda DEFAULT_KEEP_LAMBDA = DefaultKeepLambda.INSTANCE;
    public static final UpdateFittestLambda
            DISCARD_IMMEDIATELY = DefaultUpdateFittestLambdas.DISCARD_IMMEDIATELY,
            HALF = DefaultUpdateFittestLambdas.HALF,
            THIRD = DefaultUpdateFittestLambdas.THIRD,
            QUARTER = DefaultUpdateFittestLambdas.QUARTER,
            DEFAULT_UPDATE_FITTEST_LAMBDA = THIRD;

    private final TreeSet<LegacyRecord> legacies = new TreeSet<>();
    private final TreeMap<Long, Set<N>> hashes = new TreeMap<>();
    private TreeMap<String, Set<N>> specialNets = new TreeMap<>();

    public final KeepLambda<N> keep;
    public final UpdateFittestLambda<N> updateFittest;

    private transient HashMap<N, LegacyRecord> nets = new HashMap<>();;
    private transient TreeSet<F> fitnesses = new TreeSet<>();
    private transient NavigableSet<F> fitnessesView = Collections.unmodifiableNavigableSet(this.fitnesses);
    private transient NavigableMap<Long, Set<N>> hashSetsView = new TreeMap<>();
    private transient NavigableMap<Long, Set<N>> hashView = Collections.unmodifiableNavigableMap(this.hashSetsView);
    private transient NavigableMap<String, Set<N>> specialNetsView = Collections.unmodifiableNavigableMap(this.specialNets);

    protected Object readResolve() throws ObjectStreamException {
        this.nets = new HashMap<>();
        this.fitnesses = new TreeSet<>();
        this.fitnessesView = Collections.unmodifiableNavigableSet(this.fitnesses);
        this.hashSetsView = new TreeMap<>();
        this.hashView = Collections.unmodifiableNavigableMap(this.hashSetsView);

        if (this.specialNets == null) this.specialNets = new TreeMap<>();
        this.specialNetsView = Collections.unmodifiableNavigableMap(this.specialNets);

        for (LegacyRecord legacy : this.legacies) {
            this.nets.put(legacy.net, legacy);
        }

        for (Map.Entry<Long, Set<N>> entry : this.hashes.entrySet()) {
            long hash = entry.getKey();
            Set<N> setView = Collections.unmodifiableSet(entry.getValue());
            this.hashSetsView.put(hash, setView);
        }
        return this;
    }

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

    public NetTracker(NetTracker<N, F> cloneUsingDefaultLambdas) {
        this(cloneUsingDefaultLambdas, DEFAULT_KEEP_LAMBDA, DEFAULT_UPDATE_FITTEST_LAMBDA);
    }

    public NetTracker(NetTracker<N, F> cloneUsingDefaultUpdateFittestLambda, KeepLambda<N> keepLambda) {
        this(cloneUsingDefaultUpdateFittestLambda, keepLambda, DEFAULT_UPDATE_FITTEST_LAMBDA);
    }

    public NetTracker(NetTracker<N, F> cloneUsingDefaultKeepLambda, UpdateFittestLambda<N> updateFittest) {
        this(cloneUsingDefaultKeepLambda, DEFAULT_KEEP_LAMBDA, updateFittest);
    }

    public NetTracker(NetTracker<N, F> clone, KeepLambda<N> keepLambda, UpdateFittestLambda<N> updateFittestLambda) {
        this(keepLambda, updateFittestLambda);
        this.copyFrom(clone);

    }

    public void copyFrom(NetTracker<N, F> cloneFrom) {
        for (LegacyRecord cloneLegacy : cloneFrom.legacies) {
            LegacyRecord myLegacy = new LegacyRecord(cloneLegacy);
            this.legacies.add(myLegacy);
            this.nets.put(myLegacy.net, myLegacy);
        }

        for (Map.Entry<Long, Set<N>> entry : cloneFrom.hashes.entrySet()) {
            long hash = entry.getKey();
            Set<N> nets = entry.getValue();

            for (N net : nets) {
                long netHash = net.getNeuralHash();
                if (netHash != hash) {
                    System.err.println("Old hash no longer matches current algorithm: "
                            + hash + "\n" + net);
                }
                this.addToHashes(net);
            }
        }
        if (cloneFrom.fitnesses != null) {
            this.fitnesses.addAll(cloneFrom.fitnesses);
        }

        if (cloneFrom.specialNets == null) return;

        Var.Bool doAddAll = new Var.Bool();
        for (Map.Entry<String, Set<N>> entry : cloneFrom.specialNets.entrySet()) {
            String name = entry.getKey();
            Set<N> set = entry.getValue();
            doAddAll.value = true;
            if (name == null || set == null) throw new NullPointerException();
            Set<N> mySet = this.specialNets.computeIfAbsent(name, n -> {
                doAddAll.value = false;
                return new LinkedHashSet<>(set);
            });
            if (doAddAll.value) mySet.addAll(set);
        }
    }

    public interface KeepLambda<N extends NeuralNet<?, N, ?>> extends Serializable {
        public boolean keep(long currentGen, long genRating, N net);
    }

    public interface UpdateFittestLambda<N extends NeuralNet<?, N , ?>> extends Serializable {
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

    public static class KeepIncluding<N extends NeuralNet<?, N, ?>> implements KeepLambda<N> {
        private final KeepLambda<? super N> keepLambda;
        private final List<N> keepList;

        public KeepIncluding(Collection<? extends N> keepList) {
            this(DEFAULT_KEEP_LAMBDA, keepList);
        }

        public KeepIncluding(KeepLambda<? super N> keepLambda, Collection<? extends N> keepList) {
            this.keepLambda = keepLambda;
            this.keepList = new ArrayList<>(keepList);
        }


        @Override
        public boolean keep(long currentGen, long genRating, N net) {
            return keepList.contains(net) || keepLambda.keep(currentGen, genRating, net);
        }
    }


    public class LegacyRecord implements Comparable<LegacyRecord>, Serializable {
        public final N net;
        private long genRating;
        private transient F fitness;

        private LegacyRecord(NetTracker<N, F>.LegacyRecord clonedFrom) {
            this.net = clonedFrom.net;
            this.genRating = clonedFrom.genRating;
            this.fitness = clonedFrom.fitness;
        }

        private LegacyRecord(N net, F fitness) {
            this.net = net;
            this.fitness = fitness;
            this.genRating = NeuralNet.getCurrentGeneration();
            NetTracker.this.addToHashes(net);
        }


        @Override
        public int compareTo(LegacyRecord other) {
            if (this == other) return 0;
            if (this.net == other.net) {
                System.err.println("NetTracker: Unexpected duplicate LegacyRecords with the same net:\n" + this.net);

                if (this.genRating != other.genRating) {
                    this.genRating = Math.round(((double)this.genRating + (double)other.genRating) / 2);
                }

                if (this.fitness == null) this.fitness = other.fitness;
                else if (other.fitness == null) other.fitness = this.fitness;

                return 0;
            }

            if (this.genRating != other.genRating) return Long.compare(this.genRating, other.genRating);

            if (this.fitness == null) return 1;
            if (other.fitness == null) return -1;

            int mine = this.hashCode();
            int theirs = other.hashCode();
            if (mine != theirs) return Integer.compare(this.hashCode(), other.hashCode());

            mine = this.net.hashCode();
            theirs = other.net.hashCode();
            if (mine != theirs) return Integer.compare(this.hashCode(), other.hashCode());

            if (this.fitness == other.fitness) throw new IllegalStateException();
            mine = this.fitness.hashCode();
            theirs = other.fitness.hashCode();
            if (mine != theirs) return Integer.compare(this.hashCode(), other.hashCode());

            ThreadLocalRandom rand = ThreadLocalRandom.current();
            return rand.nextBoolean() ? -1 : 1;
        }
    }

    public NavigableSet<F> getFitnesses() {
        return this.fitnessesView;
    }

    public NavigableMap<Long, Set<N>> getHashes() { return this.hashView; }


    public boolean add(N net) {
        LegacyRecord legacy = this.nets.get(net);
        if (legacy != null) return false;

        legacy = new LegacyRecord(net, null);
        this.nets.put(net, legacy);
        this.legacies.add(legacy);
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

    public F getFitness(N net) {
        LegacyRecord legacy = this.nets.get(net);
        if (legacy == null) return null;
        return legacy.fitness;
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

        if (legacy.fitness != fitness) {
            changed = true;
            this.fitnesses.remove(legacy.fitness);
            legacy.fitness = fitness;
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
            this.legacies.add(legacy);
            return true;

        } else if (legacy.genRating != genRating) {
            this.legacies.remove(legacy);
            legacy.genRating = genRating;
            this.legacies.add(legacy);
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

        } else if (fitness != legacy.fitness) {
            this.fitnesses.remove(legacy.fitness);
            legacy.fitness = fitness;
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
            if (legacy == null || legacy.fitness == fittness) continue;
            if (legacy.fitness != null) this.fitnesses.remove(legacy.fitness);
            this.fitnesses.add(fittness);
            legacy.fitness = fittness;
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
        Set<Long> extinctLineages = new TreeSet<>();
        for (Iterator<LegacyRecord> iterator = legacies.iterator();
             iterator.hasNext();) {

            LegacyRecord legacy = iterator.next();
            if (this.keep.keep(currentGen, legacy.genRating, legacy.net)) continue;
            iterator.remove();
            this.nets.remove(legacy.net);
            if(legacy.fitness != null) this.fitnesses.remove(legacy.fitness);
            //extinctLineages.add(legacy.net.getNeuralHash()); //with new Lineage system, the hash of the net itself is included during iteration, as the first element
            for (Long ancestor : legacy.net.getLineage()) {
                extinctLineages.add(ancestor);
            }
        }

        for (Iterator<Long> iterator = extinctLineages.iterator();
                iterator.hasNext();) {

            long hash = iterator.next();
            for (N net : this.nets.keySet()) {
                if (net.getLineage().lineageContains(hash) > 0.0) {
                    iterator.remove();
                    break;
                }
            }
        }

        for (long extinct : extinctLineages) {
            Set<N> nets = this.hashes.get(extinct);
            if (nets == null) {
                System.err.println("Unexpected missing nets set when removing extinct lineage, for hash " + toHex(extinct));
                continue;
            }
            if (nets.size() == 1) {
                this.hashes.remove(extinct);
                this.hashSetsView.remove(extinct);
                continue;
            }

            boolean remove = true;

            for (N net1 : nets) {
                boolean reachedNet1 = false;
                for (N net2 : nets) {
                    if (!reachedNet1) {
                        reachedNet1 = net1 == net2;
                        continue;
                    }
                    if (NeuralHash.checkForHashCollision(extinct, net1, net2)) {
                        remove = false;
                        break;
                    }
                }
                if (!remove) break;
            }

            if (remove) {
                this.hashes.remove(extinct);
                this.hashSetsView.remove(extinct);
            }
        }
    }

    public Set<N> getHash(long forHash) {
        return this.hashSetsView.get(forHash);
    }

    public boolean addToHashes(N net) {
        return NetTracker.this.hashes.computeIfAbsent(net.getNeuralHash(), hash -> {
            Set<N> set = new LinkedHashSet<>();
            Set<N> setView = Collections.unmodifiableSet(set);
            NetTracker.this.hashSetsView.put(hash, setView);
            return set;

        }).add(net);
    }

    public Map<String, Set<N>> getSpecialNets() {
        return this.specialNetsView;
    }

    /**
     * Returns the mutable set of nets with the given name
     *
     * @param name
     * @return
     */
    public Set<N> getSpecialNets(String name) {
        if (name == null) throw new NullPointerException();
        return this.specialNets.get(name);
    }

    public boolean addToSpecialNets(String name, N ... nets) {
        if (name == null || nets == null) throw new NullPointerException();
        for (N net : nets) if (net == null) throw new NullPointerException();

        Var.Bool changed = new Var.Bool();
        Set<N> set = this.specialNets.computeIfAbsent(name, n -> {
            changed.value = true;
            return new LinkedHashSet<>();
        });

        for (N net : nets) {
            if (set.add(net)) changed.value = true;
        }
        return changed.value;
    }

    public boolean addToSpecialNets(String name, Collection<? extends N> nets) {
        if (name == null || nets == null) throw new NullPointerException();
        for (N net : nets) if (net == null) throw new NullPointerException();

        Var.Bool changed = new Var.Bool();
        Set<N> set = this.specialNets.computeIfAbsent(name, n -> {
            changed.value = true;
            return new LinkedHashSet<>();
        });

        for (N net : nets) {
            if (set.add(net)) changed.value = true;
        }
        return changed.value;
    }

    public Set<N> removeSpecialNets(String name) {
        if (name == null) throw new NullPointerException();
        return this.specialNets.remove(name);
    }

    public boolean removeFromSpecialNets(String name, N ... nets) {
        if (name == null || nets == null) throw new NullPointerException();
        for (N net : nets) if (net == null) throw new NullPointerException();

        Set<N> set = this.specialNets.get(name);
        if (set == null) return false;

        boolean changed = false;
        for (N net : nets) {
            if (set.remove(net)) changed = true;
        }
        return changed;
    }

    public boolean removeFromSpecialNets(String name, Collection<? extends N> nets) {
        if (name == null || nets == null) throw new NullPointerException();
        for (N net : nets) if (net == null) throw new NullPointerException();

        Set<N> set = this.specialNets.get(name);
        if (set == null) return false;

        boolean changed = false;
        for (N net : nets) {
            if (set.remove(net)) changed = true;
        }
        return changed;
    }

    public boolean specialNetsContains(String name, N net) {
        if (name == null || net == null) throw new NullPointerException();
        Set<N> set = this.specialNets.get(name);
        return set != null && set.contains(net);
    }

    public boolean removeFromAll(N net) {
        LegacyRecord legacy = this.nets.get(net);
        if (legacy != null) {
            this.legacies.remove(legacy);
            this.nets.remove(net);
        }

        Var.Bool found = new Var.Bool(false),
                removed = new Var.Bool(false);

        this.hashes.computeIfPresent(net.getNeuralHash(), (hash, nets) -> {
            found.value = true;
            removed.value = nets.remove(net);
            if (nets.size() == 0) {
                this.hashSetsView.remove(hash);
                return null;
            }
            else return nets;
        });
        if (!found.value) {
            System.err.println("netTracker.removeFromAll(net) could not find this net ");
        }

        boolean inSpecialNets = false;
        for (Set<N> nets : this.specialNets.values()) {
            if (nets.remove(net)) inSpecialNets = true;
        }

        return legacy != null || removed.value || inSpecialNets;
    }

    /**
     * Only removes from the set of active nets.  Does NOT remove from the hashes map.  To remove
     * from hashes map use removeFromAll(net)
     * @param o
     * @return
     */
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
    public String toString() {
        return this.legacies.toString();
    }

    /*
    // below is for debugging

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
    */

}
