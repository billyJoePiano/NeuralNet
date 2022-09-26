package neuralNet.evolve;

import neuralNet.util.*;
import neuralNet.util.DualIterable.*;

import java.io.*;

public interface Lineage extends Iterable<Long>, Comparable<Lineage>, Serializable {

    public long getHash();
    public long getGeneration();
    public long[] getAncestors();
    public String toString(boolean includeSelf);

    public double lineageContains(long hash);
    public double getGenerationsCount();
    public double getKinshipScore(Lineage otherLineage, FuzzyPredicate<Lineage> filter);
    public double getKinshipScore(Lineage otherLineage);

    public static double calcKinshipScore(double sharedAncestors, double genCount1, double genCount2) {
        double genCountAvg = (genCount1 + genCount2) / 2;
        if (genCountAvg >= sharedAncestors) return 1.0;
        else return sharedAncestors / genCountAvg;
    }


    default public int compareTo(Lineage other) throws CannotResolveComparisonException {
        if (other == this) return 0;
        if (other == null) return -1;

        long mine = this.getHash(), theirs = other.getHash();
        if (mine != theirs) return Long.compare(mine, theirs);

        for (Pair<Long> pair : new DualIterable<>(this, other)) {
            Long val1 = pair.value1(), val2 = pair.value2();

            if (val1 != null) {
                if (val2 == null) return -1;

            } else if (val2 != null) return 1;
            else break;

            if (!val1.equals(val2)) return Long.compare(val1, val2);
        }

        mine = this.getGeneration();
        theirs = other.getGeneration();
        if (mine != theirs) return Long.compare(mine, theirs);

        double myGens = this.getGenerationsCount(), theirGens = other.getGenerationsCount();
        if (myGens != theirGens) return Double.compare(myGens, theirGens);

        throw new CannotResolveComparisonException(this, other);
    }
}
