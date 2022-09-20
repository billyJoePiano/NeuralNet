package neuralNet.network;

import neuralNet.util.*;

import java.util.*;

public class MultiLineage implements Lineage {
    public final double sumWeight;
    private final Map<Lineage, Double> parents = new LinkedHashMap<>();
    public final long myHash;

    private transient Long[] ancestors;

    public static Map<Lineage, Double> convert(Collection<NeuralNet<?, ?, ?>> parents) {
        Map<Lineage, Double> converted = new HashMap<>();
        for (NeuralNet<?, ?, ?> parent : parents) {
            converted.put(parent.getLineage(), 1.0);
        }
        return converted;
    }

    public static Map<Lineage, Double> convert(Map<NeuralNet<?, ?, ?>, Double> parents) {
        Map<Lineage, Double> converted = new HashMap<>();
        for (Map.Entry<NeuralNet<?, ?, ?>, Double> entry : parents.entrySet()) {
            converted.put(entry.getKey().getLineage(), entry.getValue());
        }
        return converted;
    }

    public MultiLineage(Map<Lineage, Double> parents, long myHash) {
        this.myHash = myHash;

        if (parents.size() < 2) throw new IllegalArgumentException();

        double sumWeight = 0;

        for (Map.Entry<Lineage, Double> entry
                : parents.entrySet().stream().sorted(Comparator.comparingDouble(Map.Entry::getValue)).toList()) {

            Lineage lineage = entry.getKey();
            Double weight = entry.getValue();

            if (lineage == null || weight == null) throw new NullPointerException();
            if (!(Double.isFinite(weight) && weight > 0)) throw new IllegalArgumentException(weight.toString());

            sumWeight += weight;
            this.parents.put(lineage, weight);
        }

        this.sumWeight = sumWeight;
    }

    @Override
    public KinshipTracker recursiveSearch(Lineage otherLineage) {
        double generations = 0;
        double sharedAncestors = 0;

        for (Map.Entry<Lineage, Double> entry : this.parents.entrySet()) {
            KinshipTracker tracker = entry.getKey().recursiveSearch(otherLineage);
            double weight = entry.getValue();
            generations += tracker.generations * weight;
            sharedAncestors += tracker.sharedAncestors * weight;
        }

        return new KinshipTracker(generations / this.sumWeight, sharedAncestors / this.sumWeight);
    }


    @Override
    public double getGenerations() {
        double generations = 0;

        for (Map.Entry<Lineage, Double> entry : this.parents.entrySet()) {
            generations += entry.getKey().getGenerations() * entry.getValue();
        }

        return generations / this.sumWeight;
    }

    @Override
    public double lineageContains(long hash) {
        if (this.myHash == hash) return 1.0;

        double contains = 0;
        for (Map.Entry<Lineage, Double> entry : this.parents.entrySet()) {
            contains += entry.getKey().lineageContains(hash) * entry.getValue();
        }

        return contains / this.sumWeight;
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
        List<Iterator<Long>> parents = new ArrayList<>(this.parents.size());
        int size = 0;
        for (Lineage parent : this.parents.keySet()) {
            parents.add(parent.iterator());
            size += parent.size();
        }

        LineageIterator iterator = new LineageIterator(parents);
        this.ancestors = new Long[size + 1];

        int i = 0;
        for (Long ancestor : iterator) {
            this.ancestors[i++] = ancestor;
        }
    }

    private class LineageIterator implements Iterator<Long>, Iterable<Long> {
        private int index = -1;
        private final List<Iterator<Long>> parents;

        private LineageIterator(List<Iterator<Long>> parents) {
            this.parents = parents;
        }

        @Override
        public boolean hasNext() {
            return this.parents.size() > 0;
        }

        @Override
        public Long next() {
            if (this.index == -1) {
                this.index = 0;
                return MultiLineage.this.myHash;

            } else if (this.parents.size() == 0) {
                throw new NoSuchElementException();
            }

            Iterator<Long> parent = this.parents.get(this.index);
            Long hash = parent.next();
            if (!parent.hasNext()) this.parents.remove(this.index);

            this.index++;
            while (this.parents.size() > 0) {
                if (this.index > this.parents.size()) this.index = 0;
                if (this.parents.get(this.index).hasNext()) break;
                this.parents.remove(this.index);
            }

            return hash;
        }

        @Override
        public Iterator<Long> iterator() {
            return this;
        }
    }
}
