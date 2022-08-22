package neuralNet.network;

import neuralNet.neuron.*;

import java.util.*;

public interface DecisionProvider<S extends Sensable<S>,
                                    P extends DecisionProvider<S, P, C>,
                                    C extends DecisionConsumer<S, C, ?>> {

    public S getSensedObject();
    public void setSensedObject(S sensedObject);

    public List<SensorNode<S, P>> getSensors();
    public List<SignalProvider> getNeurons();
    public List<DecisionNode<P, C>> getDecisionNodes();

    public P clone(); // typically a DEEP clone of the neural network

    public long getRound();

    default public void reset() {
        this.getNeurons().forEach(SignalProvider::reset);
    }

    default public void before() {
        this.getNeurons().forEach(SignalProvider::before);
    }

    default public void sense() {
        this.getSensors().forEach(SensorNode::sense);
    }

    default public void weighDecisions() {
        this.getDecisionNodes().forEach(DecisionNode::getWeight);
    }

    default public void after() {
        this.getNeurons().forEach(SignalProvider::after);
    }

    default public void runRound() {
        this.before();
        this.sense();
        this.weighDecisions();
        this.after();
    }
}
