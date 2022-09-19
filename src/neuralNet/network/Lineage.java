package neuralNet.network;

public interface Lineage {
    default public double getKinshipScore(Lineage otherLineage) {
        return this.recurse(otherLineage).getKinshipScore();
    }

    public double lineageContains(long hash);
    public double getGenerations();
    public KinshipTracker recurse(Lineage otherLineage);

    public class KinshipTracker {
        public double generations;
        public double sharedAncestors;

        public double getKinshipScore() {
            return this.sharedAncestors / this.generations;
        }

        public KinshipTracker() { }

        public KinshipTracker(double generations, double sharedAncestors) {
            this.generations = generations;
            this.sharedAncestors = sharedAncestors;
        }
    }
}
