package neuralNet.network;

import java.util.*;

public interface DecisionProvider<S extends Sensable<S>,
                                    P extends DecisionProvider<S, P, C>,
                                    C extends DecisionConsumer<C>> {
    public List<DecisionNode>
}
