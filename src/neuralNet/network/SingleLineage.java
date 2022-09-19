package neuralNet.network;

import java.util.*;

public class SingleLineage implements Lineage {
    public final Lineage parentLineage;
    public final long myHash;

    public SingleLineage(Lineage parentLineage, long myHash) {
        if (parentLineage == null) throw new NullPointerException();
        this.parentLineage = parentLineage;
        this.myHash = myHash;
    }

    @Override
    public KinshipTracker recurse(Lineage otherLineage) {
        KinshipTracker tracker = this.parentLineage.recurse(otherLineage);
        tracker.sharedAncestors += otherLineage.lineageContains(this.myHash);
        tracker.generations++;
        return tracker;
    }


    @Override
    public double getGenerations() {
        return this.parentLineage.getGenerations() + 1.0;
    }

    @Override
    public double lineageContains(long hash) {
        return this.myHash == hash ? 1.0 : this.parentLineage.lineageContains(hash);
    }

    public static Lineage fromLegacyArray(long[] lineageLegacy, long myHash, Map<Long, Lineage> preexisting) {
        if (lineageLegacy.length == 0) {
            return preexisting.computeIfAbsent(myHash, RootLineage::new);
        }

        Lineage previous = preexisting.computeIfAbsent(lineageLegacy[lineageLegacy.length - 1], RootLineage::new);

        for (int i = lineageLegacy.length - 2; i >= 0; i--) {
            Lineage current = preexisting.get(lineageLegacy[i]);

            if (current == null) {
                current = new SingleLineage(previous, lineageLegacy[i]);
                preexisting.put(lineageLegacy[i], current);

            } else if (!(current instanceof SingleLineage sl && sl.parentLineage == previous)) {
                throw new IllegalStateException();
            }

            previous = current;
        }

        Lineage prev = previous;
        return preexisting.computeIfAbsent(myHash, mh -> new SingleLineage(prev, mh));
    }
}
