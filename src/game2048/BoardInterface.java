package game2048;

import neuralNet.network.*;
import neuralNet.util.*;

import java.util.*;

import static neuralNet.util.Util.*;

public class BoardInterface extends Board implements
        Sensable<BoardInterface>,
        DecisionConsumer<BoardInterface, BoardInterface, BoardInterface.BoardNetFitness> {

    public static final double MAX_SCORE = 262140;
    public static final int GAMES_PER_TEST = 256;

    @Override
    public int decisionCount() {
        return 4;
    }

    @Override
    public boolean takeAction(int decisionId) throws IllegalArgumentException {
        switch(decisionId) {
            case 0: return this.up();
            case 1: return this.down();
            case 2: return this.left();
            case 3: return this.right();

            default: throw new IllegalArgumentException(decisionId + "");
        }
    }

    @Override
    public BoardNetFitness testFitness(DecisionProvider<BoardInterface, ?, BoardInterface> boardNet, List<BoardInterface> usingInputs) {
        if (usingInputs != null && (usingInputs.size() > 1 || usingInputs.get(0) != this)) throw new IllegalArgumentException();

        boardNet.setSensedObject(this);

        double arthMean = 0;
        double geoMean = 0;
        int[] scores = new int[GAMES_PER_TEST];
        AccumulatedAverage timePerMove = new AccumulatedAverage();

        //try (AffinityLock af = AffinityLock.acquireCore()) {

            for (int i = 0; i < GAMES_PER_TEST; i++) {
                this.reset();
                boardNet.reset();
                while (this.isActive()) {
                    long start = System.nanoTime();
                    this.runRound(boardNet);
                    long end = System.nanoTime();
                    timePerMove.add((end - start) / BILLION);
                }
                //System.out.println(this + "" + this.getScore() + "\n\n");
                int score = this.getScore();
                arthMean += score;
                geoMean += Math.log(score);
                scores[i] = score;
            }

            arthMean /= GAMES_PER_TEST;
            geoMean = Math.exp(geoMean / GAMES_PER_TEST);

            Arrays.sort(scores);
            double median;
            if (GAMES_PER_TEST % 2 == 1) median = scores[GAMES_PER_TEST / 2];
            else median = (double)(scores[GAMES_PER_TEST / 2 - 1] + scores[GAMES_PER_TEST / 2]) / 2;

            return new BoardNetFitness(scores[0], scores[GAMES_PER_TEST - 1],
                    arthMean, geoMean, median, timePerMove.getAverage());
        //}
    }

    @Override
    public int getMaxNoOpRounds() {
        return 32;
    }

    @Override
    public int numSensorNodesRequired() {
        return 16;
    }


    public static class BoardNetFitness implements Fitness<BoardInterface, BoardNetFitness> {
        public static final double TIME_PER_MOVE_THRESHOLD = 0.25; // if time per move is above this threshold, the score is negatively impacted
        public static final double TIME_WEIGHTING = 0.25; // relative weight of the time-adjusted score vs. the raw score (where raw score is always weighted 1.0)
        public static final double TOTAL_WEIGHTING = TIME_WEIGHTING + 1;

        public final int min;
        public final int max;
        public final double arthMean;
        public final double geoMean;
        public final double median;
        public final double minMaxGeo;

        public final double composite; // doesn't account for timePerMove
        public final double timePerMove;
        public final double weightedScore;

        private BoardNetFitness(final int min, final int max, final double arthMean, final double geoMean, final double median,
                                final double timePerMove) {
            this.min = min;
            this.max = max;
            this.minMaxGeo = Math.sqrt((double)min * (double)max);

            this.arthMean = arthMean;
            this.geoMean = geoMean;
            this.median = median;

            double minMetric = Util.min(arthMean, geoMean, median, minMaxGeo);
            double composite = Math.sqrt(Math.sqrt(arthMean * geoMean) * Math.sqrt(median * this.minMaxGeo)); //geomean of the four score metrics
            this.composite = Math.sqrt(minMetric * composite); //geoMean with the smallest of the four metrics


            this.timePerMove = timePerMove;

            if (timePerMove > TIME_PER_MOVE_THRESHOLD) {
                double diff = timePerMove - TIME_PER_MOVE_THRESHOLD;
                this.weightedScore = (this.composite + TIME_WEIGHTING * (this.composite / (1 + diff))) / TOTAL_WEIGHTING;

            } else {
                this.weightedScore = this.composite;
            }
        }

        public short getSignal() {
            return clip(this.weightedScore * RANGE / MAX_SCORE);
        }

        @Override
        public int compareTo(BoardNetFitness other) {
            if (other == null) return -1;
            if (other == this) return 0;

            if (this.weightedScore > other.weightedScore) return -1;
            if (this.weightedScore < other.weightedScore) return 1;

            // for now... prioritize score over time
            if (this.composite > other.composite) return -1;
            if (this.composite < other.composite) return 1;

            if (this.timePerMove < other.timePerMove) return -1;
            if (this.timePerMove > other.timePerMove) return 1;

            return 0;
        }

        public String toString() {
            return this.getClass().getSimpleName()
                    + "\t\tMean: " + this.arthMean
                    + "\t\tGeoMean: " + this.geoMean
                    + "\t\tMedian: " + this.median
                    + "\t\tMinMaxGeo: " + this.minMaxGeo
                    + "\n\t\t\t\tComposite: " + this.composite
                    + "\t\tTime Per Move: " + this.timePerMove
                    + "\t\tWeighted Score: " + this.weightedScore;
        }
    }
}
