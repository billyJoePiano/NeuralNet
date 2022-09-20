package neuralNet.test;

import game2048.*;
import neuralNet.evolve.*;

import java.io.*;
import java.util.*;
import java.util.stream.*;

public class TestAddNeurons implements MutatorFactory<BoardNet> {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        EvolutionaryEngine engine = new EvolutionaryEngine(new TestAddNeurons());
        engine.replaceSystemStreams();

        if (args.length > 0) engine.loadSaved(args[0]);

        engine.run();
    }


    @Override
    public Collection<? extends Mutator<? extends BoardNet>> makeMutators(Collection<? extends BoardNet> forProviders) {
        return forProviders.stream().map(AddNeurons::new).collect(Collectors.toList());
    }


    private enum KeepEdgeAndRand implements NetTracker.KeepLambda<BoardNet> {
        INSTANCE;

        @Override
        public boolean keep(long currentGen, long genRating, BoardNet net) {
            throw new UnsupportedOperationException();
        }
    }
}
