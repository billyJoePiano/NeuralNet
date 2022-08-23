package neuralNet.network;

public interface Sensable<O extends Sensable<O>> {
    public int numSensorNodesRequired();
}
