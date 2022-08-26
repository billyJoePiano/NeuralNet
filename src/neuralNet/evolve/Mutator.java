package neuralNet.evolve;

import neuralNet.network.*;

public interface Mutator<S extends Sensable<S>,
                            P extends DecisionProvider<S, P, C>,
                            C extends DecisionConsumer<S, C, ?>> {

    default public long getGeneration() {

    }


}
