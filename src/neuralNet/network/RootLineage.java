package neuralNet.network;

public class RootLineage implements Lineage {
    public final long myHash;

    public RootLineage(long myHash) {
        this.myHash = myHash;
    }

    @Override
    public KinshipTracker recurse(Lineage otherLineage) {
        return new KinshipTracker(1, otherLineage.lineageContains(this.myHash));
    }

    @Override
    public double getGenerations() {
        return 1.0;
    }

    @Override
    public double lineageContains(long hash) {
        return hash == this.myHash ? 1.0 : 0.0;
    }
}
