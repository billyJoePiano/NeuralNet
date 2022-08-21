package neuralNet.network;

import neuralNet.neuron.SignalProvider;

public interface SensorNode<O extends Sensable<O>,
                        S extends SensorNode<O, S, N>,
                        N extends NeuralNet<O, ?, N>>
        extends SignalProvider {

    public O getObservedObject();
    public N getNeuralNet();
    public int getOutputId(); // tells the Sensable object which output to assign the sensor to


    public interface Setter<O extends Sensable<O>, S extends SensorNode<O, S, ?>> {
        public void setSignal(short signal);

        public S getSensor();

        default public int getOutputId() {
            return this.getSensor().getOutputId();
        }
    }
}
