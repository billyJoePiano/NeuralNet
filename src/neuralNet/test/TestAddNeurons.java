package neuralNet.test;

import game2048.*;
import neuralNet.evolve.*;

import java.util.*;

public class TestAddNeurons {
    public static final int GENERATIONS = 256;
    public static final int MAX_NETS_PER_GENERATION = 8;

    public static void main(String[] args) {
        List<List<BoardNet>> generations = new ArrayList(GENERATIONS);
        List<BoardNet> lastGen = List.of(TestBoardNet.makeEdgeNet(), TestBoardNet.makeRandomNet());
        generations.add(lastGen);

        for (int i = 1; i < GENERATIONS; i++) {

            List<BoardNet> thisGen = new ArrayList<>(MAX_NETS_PER_GENERATION);
            generations.add(thisGen);

            double mutationsPerNet = (double)MAX_NETS_PER_GENERATION / (double)lastGen.size();

            for (int n = 0; n < lastGen.size(); n++) {
                int makeMutations = (int)Math.round(mutationsPerNet * (n + 1)) - thisGen.size();

                AddNeurons<BoardNet> mutator = new AddNeurons<>(lastGen.get(n));
                thisGen.addAll(mutator.mutate(makeMutations));
            }

            lastGen = thisGen;
        }

        for (BoardNet net : lastGen) {
            net.checkForUnaccountedNeurons();
        }


        System.out.println();
    }
}
