package neuralNet.test;

import game2048.*;
import neuralNet.evolve.*;
import neuralNet.network.*;
import neuralNet.util.*;

import java.io.*;
import java.util.*;

import static neuralNet.util.Util.MILLION;

public class TestAddNeurons implements MutatorFactory<BoardNet, BoardInterface> {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        EvolutionaryEngine engine = new EvolutionaryEngine(new TestAddNeurons());
        engine.replaceSystemStreams();

        if (args.length > 0) engine.loadSaved(args[0]);

        engine.run();
    }

    private Map<BoardNet, AccumulatedAverage> testTimes = new WeakHashMap<>();


    @Override
    public void newFitnessResults(Collection<? extends Fitness<BoardInterface, ?>> fitnesses) {
        for (Fitness<BoardInterface, ?> fitness : fitnesses) {
            double msSq = fitness.getTestTime() / MILLION;
            msSq *= msSq;

            this.testTimes.computeIfAbsent((BoardNet)fitness.getDecisionProvider(), bn -> new AccumulatedAverage())
                            .add(msSq);
        }
    }

    @Override
    public Double estimatedFitnessTestTime(BoardNet net) {
        AccumulatedAverage avg = this.testTimes.get(net);
        if (avg == null) {
            System.err.println("MutatorFactory testTimes map couldn't find established net:\n\t" + net);
            return Double.MAX_VALUE;

        } else {
            return avg.getAverage();
        }
    }

    @Override
    public ArrayList<Mutator<BoardNet>> makeMutators(int totalMutants,
                                                  Collection<? extends BoardNet> parents,
                                                  SortedSet<? extends Fitness<BoardInterface, ?>> fitnesses) {

        int[] counts = MutatorFactory.calcCounts(parents.size(), totalMutants);
        ArrayList<Mutator<BoardNet>> result = new ArrayList<>(counts.length);

        int i = 0;
        for (BoardNet parent : parents) {
            int count = counts[i++];
            if (count == 0) break;
            result.add(new AddNeurons<>(parent, count, this.testTimes.get(parent).getAverage() * 16));
        }

        return result;
    }


    private enum KeepEdgeAndRand implements NetTracker.KeepLambda<BoardNet> {
        INSTANCE;

        @Override
        public boolean keep(long currentGen, long genRating, BoardNet net) {
            throw new UnsupportedOperationException();
        }
    }
}
