package neuralNet.network;

import neuralNet.neuron.*;

public interface SensorNode<S extends Sensable<S>,
                        //N extends SensorNode<S, N, P>,
                        P extends DecisionProvider<S, P, ?>>
        extends SignalProvider {

    public S getSensedObject();
    public P getDecisionProvider();
    public void sense();

    //public int getOutputId(); // tells the Sensable object which output to assign the sensor to

    @Override
    default public SignalProvider clone() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}
