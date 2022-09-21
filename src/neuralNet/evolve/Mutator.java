package neuralNet.evolve;

import neuralNet.network.*;

import java.util.*;

public interface Mutator<//S extends Sensable<S>,
                            P extends DecisionProvider<?, P, ?>> {
                            //C extends DecisionConsumer<S, C, ?>> {

    public List<P> mutate(int count);
}
