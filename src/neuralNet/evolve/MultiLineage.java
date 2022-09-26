package neuralNet.evolve;

import neuralNet.network.*;

import java.util.*;
import java.util.stream.*;

public class MultiLineage<N extends DecisionProvider<?, N, ?>> extends CachingLineage<N> {
    public final double sumWeight;
    private final Map<Lineage, Double> parents;

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

    private static ProcessedMap process(Map<Lineage, Double> parents) {
        if (parents.size() < 2) throw new IllegalArgumentException();

        Map<Lineage, Double> sortedCopy = new LinkedHashMap<>();
        double sumWeight = 0;
        double generations = 0;

        for (Map.Entry<Lineage, Double> entry
                : parents.entrySet().stream().sorted(Comparator.comparingDouble(Map.Entry::getValue)).toList()) {

            Lineage lineage = entry.getKey();
            Double weight = entry.getValue();

            if (lineage == null || weight == null) throw new NullPointerException();
            if (!(Double.isFinite(weight) && weight > 0)) throw new IllegalArgumentException(weight.toString());

            sumWeight += weight;
            generations += lineage.getGenerationsCount() * weight;

            sortedCopy.put(lineage, weight);
        }

        return new ProcessedMap(Collections.unmodifiableMap(sortedCopy), generations / sumWeight + 1, sumWeight);
    }

    private record ProcessedMap(Map<Lineage, Double> parents, double generations, double sumWeight) { }

    public MultiLineage(N net, Map<Lineage, Double> parents) {
        this(net, process(parents));
    }

    private MultiLineage(N net, ProcessedMap processed) {
        super(net, processed.generations);

        this.parents = processed.parents;
        this.sumWeight = processed.sumWeight;
    }

    @Override
    protected double searchForSharedAncestors(Lineage other) {
        double ancestors = 0;
        for (Map.Entry<Lineage, Double> entry : this.parents.entrySet()) {
            ancestors += entry.getKey().getSharedAncestors(other) * entry.getValue();
        }
        return ancestors / this.sumWeight + other.lineageContains(this.myHash);
    }

    @Override
    protected double calcKinshipScore(Lineage other, double myGens, double otherGens, double sharedAncestors) {
        return Lineage.calcKinshipScore(sharedAncestors, myGens, otherGens);
    }

    @Override
    protected Iterator<Long> ancestorsIterator() {
        return new LineageIterator();
    }

    @Override
    protected String toStringNoSelf() {
        StringBuilder str = new StringBuilder("{");

        boolean first = true;

        for (Map.Entry<Lineage, Double> entry : this.parents.entrySet()) {
            if (first) first = false;
            else str.append(", ");

            str.append('<').append(entry.getValue()).append('>').append(entry.getKey().toString(true));
            //TODO String.format the weight, for readability (limit sig digits)
        }

        return str + "}";
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

    private class LineageIterator implements Iterator<Long> {
        private int index = 0;
        private final List<Iterator<Long>> parents = MultiLineage.this.parents.keySet().stream()
                                                                .map(Iterable::iterator)
                                                                .collect(Collectors.toList());

        private LineageIterator() {
            for (Iterator<Iterator<Long>> iterator = parents.iterator();
                    iterator.hasNext();) {

                Iterator<Long> lineageIterator = iterator.next();
                if (!lineageIterator.hasNext()) iterator.remove();
            }
        }

        @Override
        public boolean hasNext() {
            return this.parents.size() > 0;
        }

        @Override
        public Long next() {
            if (this.parents.size() == 0) {
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
    }
}
