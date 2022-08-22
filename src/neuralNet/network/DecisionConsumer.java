package neuralNet.network;

import java.util.*;

public interface DecisionConsumer<S extends Sensable<S>,
                                    C extends DecisionConsumer<S, C, F>,
                                    F extends Fitness<C, F>> {

    public int decisionCount();

    /**
     * Attempt to take the given action by decisionId, and return true if it succeeded, false if not
     * @param decisionId
     * @return
     */
    public boolean takeAction(int decisionId) throws IllegalArgumentException;

    public F testFitness(DecisionProvider<S, ?, C> decisionProvider, List<S> usingInputs);

    default public void runRound(DecisionProvider<S, ?, C> withProvider, S input) {
        withProvider.runRound();

        ArrayList<DecisionNode<?, C>> decisions = new ArrayList<>(withProvider.getDecisionNodes());
        Collections.sort(decisions);
        for (DecisionNode decision : decisions) {
            if (this.takeAction(decision.getDecisionId())) break;
        }
    }
}
