package neuralNet.network;

import neuralNet.neuron.*;

public interface SensorNode<S extends Sensable<S>,
                        //N extends SensorNode<S, N, P>,
                        P extends DecisionProvider<S, P, ?>>
        extends SignalProvider {

    public S getSensedObject();
    public P getDecisionProvider();
    public int getSensorId();
    public short sense();

    @Override
    default public SignalProvider clone() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}
