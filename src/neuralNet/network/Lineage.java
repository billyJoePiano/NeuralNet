package neuralNet.network;

import java.io.*;
import java.util.*;
import java.util.function.*;

public interface Lineage extends Iterable<Long>, Serializable {

    default public double getKinshipScore(Lineage otherLineage) {
        synchronized (this) {
            KinshipRecord record = KinshipResultsCache.INSTANCE.getRecord(this, otherLineage);
            if (record != null) return record.kinshipScore;

            return KinshipResultsCache.INSTANCE.record(this, otherLineage, this.cachingRecursiveSearch(otherLineage));
        }
    }

    public long getHash();

    public double lineageContains(long hash);
    public double getGenerations();
    public KinshipTracker recursiveSearch(Lineage otherLineage);

    default public KinshipTracker cachingRecursiveSearch(Lineage otherLineage) {
        synchronized (this) {
            KinshipRecord record = KinshipResultsCache.INSTANCE.getRecord(this, otherLineage);
            if (record != null) return record.makeTracker();
            KinshipTracker tracker = recursiveSearch(otherLineage);
            KinshipResultsCache.INSTANCE.record(this, otherLineage, tracker);
            return tracker;
        }
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

    public static KinshipTracker trackerFromRecord(Lineage lineage, Lineage otherLineage) {
        return KinshipResultsCache.INSTANCE.trackerFromRecord(lineage, otherLineage);
    }

    public static KinshipRecord getRecord(Lineage lineage, Lineage otherLineage) {
        return KinshipResultsCache.INSTANCE.getRecord(lineage, otherLineage);
    }

    public enum KinshipResultsCache implements Function<Lineage, WeakHashMap<Lineage, KinshipRecord>> {
        INSTANCE;

        private final WeakHashMap<Lineage, WeakHashMap<Lineage, KinshipRecord>>
                trackerRecords = new WeakHashMap<>();

        /**
         * Used by trackerRecords.computeIfAbsent
         *
         * @param lineage
         * @return
         */
        @Override
        public WeakHashMap<Lineage, KinshipRecord> apply(Lineage lineage) {
            return new WeakHashMap<>();
        }

        private KinshipTracker trackerFromRecord(Lineage lineage, Lineage otherLineage) {

            Map<Lineage, KinshipRecord> recordMap = trackerRecords.get(lineage);
            if (recordMap == null) return null;

            KinshipRecord record = recordMap.get(otherLineage);
            if (record == null) return null;
            else return record.makeTracker();
        }

        private double record(Lineage lineage, Lineage otherLineage, KinshipTracker tracker) {
            Map<Lineage, KinshipRecord> recordMap = trackerRecords.computeIfAbsent(lineage, this);
            if (recordMap.containsKey(otherLineage)) throw new IllegalStateException();

            double kinshipScore = tracker.getKinshipScore(otherLineage.getGenerations());
            recordMap.put(otherLineage, new KinshipRecord(kinshipScore, tracker.generations, tracker.sharedAncestors));

            return kinshipScore;
        }

        private KinshipRecord getRecord(Lineage lineage, Lineage otherLineage) {
            Map<Lineage, KinshipRecord> recordMap = trackerRecords.get(lineage);
            if (recordMap == null) return null;
            else return recordMap.get(otherLineage);
        }


    }
}
