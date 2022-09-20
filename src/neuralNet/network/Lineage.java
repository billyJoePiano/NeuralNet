package neuralNet.network;

import java.io.*;

public interface Lineage extends Iterable<Long>, Serializable {
    default public double getKinshipScore(Lineage otherLineage) {
        return this.recursiveSearch(otherLineage).getKinshipScore(otherLineage.getGenerations());
    }

    public double lineageContains(long hash);
    public double getGenerations();
    public KinshipTracker recursiveSearch(Lineage otherLineage);

    public int size();

    public class KinshipTracker {
        public double generations;
        public double sharedAncestors;

        public double getKinshipScore(double otherGenerations) {
            if (otherGenerations < 1 || this.generations < 1) throw new IllegalStateException();

            double midpointGen = (otherGenerations + this.generations) / 2 - 1; // don't include the current generation

            if (this.sharedAncestors > midpointGen) return 1.0;
            else if (midpointGen == 0.0) return 0.0;
            else return this.sharedAncestors / midpointGen;
        }

        public KinshipTracker() { }

        public KinshipTracker(double generations, double sharedAncestors) {
            this.generations = generations;
            this.sharedAncestors = sharedAncestors;
        }
    }
}
