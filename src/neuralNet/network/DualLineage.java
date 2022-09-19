package neuralNet.network;

public class DualLineage implements Lineage {
    public final Lineage parent1, parent2;
    public final long myHash;

    public DualLineage(Lineage parent1, Lineage parent2, long myHash) {
        if (parent1 == null || parent2 == null) throw new NullPointerException();
        this.parent1 = parent1;
        this.parent2 = parent2;
        this.myHash = myHash;
    }

    @Override
    public KinshipTracker recurse(Lineage otherLineage) {
        KinshipTracker tracker1 = this.parent1.recurse(otherLineage);
        KinshipTracker tracker2 = this.parent2.recurse(otherLineage);
        tracker1.generations = (tracker1.generations + tracker2.generations) / 2;
        tracker1.sharedAncestors = (tracker1.sharedAncestors + tracker2.generations) / 2;

        return tracker1;
    }


    @Override
    public double getGenerations() {
        return (this.parent1.getGenerations() + this.parent2.getGenerations()) / 2 + 1;
    }

    @Override
    public double lineageContains(long hash) {
        return this.myHash == hash ? 1.0
                : (this.parent1.lineageContains(hash) + this.parent2.lineageContains(hash)) / 2;
    }
}
