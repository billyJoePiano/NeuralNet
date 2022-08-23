package neuralNet.network;

import java.util.*;
import java.util.stream.*;

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

    public int getMaxNoOpRounds();

    default public void runRound(DecisionProvider<S, ?, C> withProvider) {
        withProvider.runRound();

        ArrayList<DecisionNode<?, C>> decisions = new ArrayList<>(withProvider.getDecisionNodes());
        Collections.sort(decisions);
        for (DecisionNode<?, C> decision : decisions) {
            if (decision.getDecisionId() == -1) {
                runNoOpRounds(withProvider);

            } else if (this.takeAction(decision.getDecisionId())) return;
        }
    }

    default public void runNoOpRounds(DecisionProvider<S, ?, C> withProvider) {
        ArrayList<DecisionNode<?, C>> decisions = new ArrayList<>(withProvider.getDecisionNodes());
        Map<DecisionNode<?, C>, Short> lastAttempt = null;

        int i;
        int end = this.getMaxNoOpRounds();

        for (i = 0; i < end; i++) {
            withProvider.runRound();

            Collections.sort(decisions);
            Map<DecisionNode<?, C>, Short> currentAttempt = decisions.stream().collect(Collectors.toMap(node -> node, DecisionNode::getWeight));
            if (Objects.equals(currentAttempt, lastAttempt)) break;
            lastAttempt = currentAttempt;

            for (DecisionNode<?, C> decision : decisions) {
                if (decision.getDecisionId() == -1) break;
                if (this.takeAction(decision.getDecisionId())) return;
            }
        }

        if (i == end && end != 0) {
            System.err.println("WARNING: Reached " + i + " iterations of NoOp rounds with differing decision weights \t "
                    + this.getClass());
        }

        Collections.sort(decisions);

        for (DecisionNode<?, C> decision : decisions) {
            if (decision.getDecisionId() == -1) continue;
            if (this.takeAction(decision.getDecisionId())) return;
        }
    }
}
