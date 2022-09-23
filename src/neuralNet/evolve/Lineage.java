package neuralNet.evolve;

import java.io.*;
import java.util.*;
import java.util.function.*;

public interface Lineage extends Iterable<Long>, Serializable {

    public long getHash();
    public double lineageContains(long hash);
    public double getGenerations();
    public KinshipTracker recursiveSearch(Lineage otherLineage);

    default public double getKinshipScore(Lineage otherLineage) {
        synchronized (this) {
            KinshipRecord record = KinshipResultsCache.getRecord(this, otherLineage);
            if (record != null) return record.kinshipScore;

            return KinshipResultsCache.record(this, otherLineage, this.cachingRecursiveSearch(otherLineage));
        }
    }

    default public KinshipTracker cachingRecursiveSearch(Lineage otherLineage) {
        synchronized (this) {
            KinshipRecord record = KinshipResultsCache.getRecord(this, otherLineage);
            if (record != null) return record.makeTracker();
            KinshipTracker tracker = recursiveSearch(otherLineage);
            KinshipResultsCache.record(this, otherLineage, tracker);
            return tracker;
        }
    }

    default public long[] getAncestors() {
        LinkedList<Long> list = new LinkedList<>();

        boolean first = true; //omit own hash
        for (Long ancestor : this) {
            if (first) first = false;
            else list.add(ancestor);
        }

        long[] arr = new long[list.size()];
        int i = 0;
        for (Long ancestor : list) {
            arr[i++] = ancestor;
        }

        return arr;
    }

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

    public record KinshipRecord(double kinshipScore, double generations, double sharedAncestors) {
        public KinshipTracker makeTracker() {
            return new KinshipTracker(this.generations, this.sharedAncestors);
        }
    }

    public enum KinshipResultsCache {
        ;

        private static final Function<Lineage, WeakHashMap<Lineage, KinshipRecord>> computeIfAbsent = l -> new WeakHashMap<>();

        private static final WeakHashMap<Lineage, WeakHashMap<Lineage, KinshipRecord>>
                trackerRecords = new WeakHashMap<>();

        private static KinshipTracker trackerFromRecord(Lineage lineage, Lineage otherLineage) {

            Map<Lineage, KinshipRecord> recordMap = trackerRecords.get(lineage);
            if (recordMap == null) return null;

            KinshipRecord record = recordMap.get(otherLineage);
            if (record == null) return null;
            else return record.makeTracker();
        }

        private static double record(Lineage lineage, Lineage otherLineage, KinshipTracker tracker) {
            Map<Lineage, KinshipRecord> recordMap = trackerRecords.computeIfAbsent(lineage, computeIfAbsent);
            if (recordMap.containsKey(otherLineage)) throw new IllegalStateException();

            double kinshipScore = tracker.getKinshipScore(otherLineage.getGenerations());
            recordMap.put(otherLineage, new KinshipRecord(kinshipScore, tracker.generations, tracker.sharedAncestors));

            return kinshipScore;
        }

        private static KinshipRecord getRecord(Lineage lineage, Lineage otherLineage) {
            Map<Lineage, KinshipRecord> recordMap = trackerRecords.get(lineage);
            if (recordMap == null) return null;
            else return recordMap.get(otherLineage);
        }


    }
}
