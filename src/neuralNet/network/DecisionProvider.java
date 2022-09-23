package neuralNet.network;

import neuralNet.evolve.*;
import neuralNet.neuron.*;

import java.io.*;
import java.util.*;

public interface DecisionProvider<S extends Sensable<S>,
                                    P extends DecisionProvider<S, P, C>,
                                    C extends DecisionConsumer<S, C, ?>>
        extends Serializable {

    public long getGeneration();
    public Lineage getLineage();

    public S getSensedObject();
    public void setSensedObject(S sensedObject);

    public List<? extends SensorNode<S, P>> getSensors();
    public List<? extends DecisionNode<P, C>> getDecisionNodes();

    public Set<SignalProvider> getProviders();
    public Set<SignalConsumer> getConsumers();

    public P clone(); // typically a DEEP clone of the neural network

    public long getRound();

    default public void reset() {
        this.getProviders().forEach(SignalProvider::reset);
    }

    default public void before() {
        this.getProviders().forEach(SignalProvider::before);
    }

    default public void sense() {
        this.getSensors().forEach(SensorNode::sense);
    }

    default public void weighDecisions() {
        this.getDecisionNodes().forEach(DecisionNode::getWeight);
    }

    default public void after() {
        this.getProviders().forEach(SignalProvider::after);
    }

    default public void runRound() {
        this.before();
        this.sense();
        this.weighDecisions();
        this.after();
    }

    public long getNeuralHash();
}
