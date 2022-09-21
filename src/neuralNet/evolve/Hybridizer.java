package neuralNet.evolve;

import neuralNet.network.*;
import neuralNet.neuron.*;
import neuralNet.util.*;

import java.lang.ref.*;
import java.util.*;

public class Hybridizer<N extends NeuralNet<?, N, ?>> implements Mutator<N> {
    private static WeakHashMap<NeuralNet, WeakHashMap<NeuralNet, CacheRecord>> cache;


    private final CacheRecord<N> record;
    private final N parent1, parent2;

    public Hybridizer(N parent1, N parent2) {
        this.record = this.getOrMake(parent1, parent2);
        this.parent1 = this.record.parent1;
        this.parent2 = this.record.parent2;
    }


    @Override
    public List<N> mutate(int count) {
        int decisionNodes = this.parent1.getDecisionNodes().size();
        if (decisionNodes != this.parent2.getDecisionNodes().size()) throw new IllegalStateException();
        if (this.record.madeHybrids.size() >= Util.pow2(decisionNodes) - 2) throw new IllegalStateException();

        if (this.record.equivalentProviders.size() == 0) findEquivalentProviders();

        return null;
    }

    public void findEquivalentProviders() {
        List<? extends SensorNode<?, N>> s1 = this.parent1.getSensors(), s2 = this.parent2.getSensors();
        if (s1.size() != s2.size()) throw new IllegalStateException();

        for (Iterator<? extends SensorNode<?, N>> it1 = s1.iterator(), it2 = s2.iterator();
                it1.hasNext();) {

            SensorNode<?, N> sensor1 = it1.next();
            SensorNode<?, N> sensor2 = it2.next();

            this.record.equivalentProviders.put(sensor2, new WeakReference<>(sensor1));
        }

        Map<Long, Set<SignalProvider>> providers2 = new TreeMap<>();

        for (SignalProvider provider : this.parent2.getProviders()) {
            providers2.computeIfAbsent(provider.getNeuralHash(), h -> new HashSet<>()).add(provider);
        }

        for (SignalProvider provider1 : this.parent1.getProviders()) {
            Set<SignalProvider> others2 = providers2.get(provider1.getNeuralHash());
            if (others2 == null) continue;
            for (SignalProvider provider2 : others2) {
                if (provider1.sameBehavior(provider2)) {
                    this.record.equivalentProviders.put(provider2, new WeakReference<>(provider1));
                }
            }
        }
    }








    private record CacheRecord<N extends NeuralNet<?, N, ?>>(
            N parent1,
            N parent2,
            WeakHashMap<SignalProvider, WeakReference<SignalProvider>> equivalentProviders,
            List<boolean[]> madeHybrids) {

        private CacheRecord(N parent1, N parent2) {
            this(parent1, parent2, new WeakHashMap<>(), new LinkedList<>());

        }
    }
    private CacheRecord<N> getOrMake(N parent1, N parent2) {
        if (parent1 == null || parent2 == null) throw new NullPointerException();
        if (parent1 == parent2) throw new IllegalArgumentException();

        long hash1 = parent1.getNeuralHash();
        long hash2 = parent2.getNeuralHash();
        boolean equals = hash1 == hash2;

        CacheRecord<N> record;
        if (equals) {
            for (Iterator<Long> lineage1 = parent1.getLineage().iterator(), lineage2 = parent2.getLineage().iterator(); ; ) {
                if (lineage1.hasNext()) {
                    if (!lineage2.hasNext()) {
                        equals = false;
                        break;
                    }

                } else if (lineage2.hasNext()) {
                    equals = false;
                    hash1 = 1L;
                    hash2 = 0L;

                } else break;

                hash1 = lineage1.next();
                hash2 = lineage2.next();
                if (hash1 != hash2) {
                    equals = false;
                    break;
                }
            }

            if (equals) {
                WeakHashMap<NeuralNet, CacheRecord> recordMap = cache.get(parent1);
                if (recordMap == null) record = null;
                else record = recordMap.get(parent2);

                if (record == null) {
                    recordMap = cache.get(parent2);
                    if (recordMap != null) record = recordMap.get(parent1);

                    if (record == null) {
                        record = new CacheRecord<>(parent1, parent2);
                        cache.computeIfAbsent(parent1, p -> new WeakHashMap<>()).put(parent2, record);
                        return record;
                    }
                }
            }
        }


        N p1, p2;

        if (hash1 > hash2) {
            p1 = parent2;
            p2 = parent1;

        } else {
            p1 = parent1;
            p2 = parent2;
        }

        return cache.computeIfAbsent(p1, p -> new WeakHashMap<>()).computeIfAbsent(p2, p -> new CacheRecord<>(p1, p2));
    }
}
