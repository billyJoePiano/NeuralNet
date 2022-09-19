package neuralNet.network;

import java.util.*;

public class MultiLineage implements Lineage {
    public final double sumWeight;
    private final Map<Lineage, Double> parents = new LinkedHashMap<>();
    public final long myHash;

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
    public KinshipTracker recurse(Lineage otherLineage) {
        double generations = 0;
        double sharedAncestors = 0;

        for (Map.Entry<Lineage, Double> entry : this.parents.entrySet()) {
            KinshipTracker tracker = entry.getKey().recurse(otherLineage);
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
}
