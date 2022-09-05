package neuralNet.network;

import neuralNet.neuron.*;

import java.lang.invoke.*;

public interface SensorNode<S extends Sensable<S>,
                        //N extends SensorNode<S, N, P>,
                        P extends DecisionProvider<S, P, ?>>
        extends SignalProvider {

    public S getSensedObject();
    public P getDecisionProvider();
    public int getSensorId();
    public short sense();

    //public int getOutputId(); // tells the Sensable object which output to assign the sensor to

    @Override
    default public SignalProvider clone() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public static final long HASH_HEADER = NeuralHash.HEADERS.get(MethodHandles.lookup().lookupClass());
    @Override
    default public long getNeuralHash() {
        return HASH_HEADER ^ Long.rotateLeft(this.getSensorId(), 29);
    }
}
