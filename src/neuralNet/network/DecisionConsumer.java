package neuralNet.network;

public interface DecisionConsumer<C extends DecisionConsumer<C>> {

    public int get

    default public void makeDecision() {
        /*
        //based on assumption of only one
        Set<DecisionNode<O, ? extends DecisionNode>> decisions = new TreeSet<>(this.getDecisionNodes());
        for (DecisionNode decision : decisions) {
            if (decision.executeDecision()) break;
        }
         */
    }
}
