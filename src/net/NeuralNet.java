package net;

import java.util.*;

public interface NeuralNet<O extends Sensable<O>,
                            C extends DecisionConsumer<C>,
                            N extends NeuralNet<O, C, N>> {
    public <S extends SensorNode<O, S, N>> Set<SensorNode<O, S, N>> getSensors();
    public void setSensedObject(O sensedObject);
    public O getSensedObject();

    public List<DecisionNode<C, ?, N>> getDecisionNodes();

    public N deepClone();
}
