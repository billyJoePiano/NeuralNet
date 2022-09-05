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
    public static final long MAX_MS_TRIALS = 5000;
    public static final double MS_CHECK_THRESHOLD = 1.0;
            //If a single round takes more than this ^^ ms, then do a check against quitting time as determined by MAX_MS_TRIALS

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
        //try (UniqueAffinityLock af = UniqueAffinityLock.obtain()) {
            boardNet.setSensedObject(this);

            int gamesRun = 0;
            double arthMean = 0;
            double geoMean = 0;
            int[] scores = new int[GAMES_PER_TEST];
            AccumulatedAverage timePerMove = new AccumulatedAverage(); //in milliseconds

            long quitAfter = System.currentTimeMillis() + MAX_MS_TRIALS;
            do {
                boolean cutShort = false;
                this.reset();
                boardNet.reset();
                while (this.isActive()) {
                    long start = System.nanoTime();
                    this.runRound(boardNet);
                    long end = System.nanoTime();
                    double ms = (end - start) / MILLION;
                    timePerMove.add(ms);
                    if (ms > MS_CHECK_THRESHOLD && System.currentTimeMillis() > quitAfter) {
                        cutShort = true;
                        break;
                    }
                }
                //System.out.println(this + "" + this.getScore() + "\n\n");
                if (cutShort && gamesRun != 0) break;
                        // If gamesRun == 0 we'll record the current game as if it's finished...
                        // otherwise this game will be ignored
                        // When gamesRun == 0, the while condition will still increment gamesRun
                        // and then the time check will cause the loop to break

                int score = this.getScore();
                arthMean += score;
                geoMean += Math.log(score);
                scores[gamesRun] = score;

            } while(++gamesRun < GAMES_PER_TEST && System.currentTimeMillis() <= quitAfter);

            arthMean /= gamesRun;
            geoMean = Math.exp(geoMean / gamesRun);

            Arrays.sort(scores);
            int startIndex = GAMES_PER_TEST - gamesRun;
            double median;
            int medIndex = gamesRun / 2 + startIndex;
            if ((gamesRun & 0b1) == 1) median = scores[medIndex];
            else median = (double)(scores[medIndex - 1] + scores[medIndex]) / 2;

            return new BoardNetFitness((BoardNet)boardNet, scores[startIndex], scores[GAMES_PER_TEST - 1],
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

        public final BoardNet net;

        public final int min;
        public final int max;
        public final double arthMean;
        public final double geoMean;
        public final double median;
        public final double minMaxGeo;

        public final double composite; // doesn't account for timePerMove
        public final double timePerMove;
        public final double weightedScore;

        private BoardNetFitness(BoardNet net, final int min, final int max, final double arthMean, final double geoMean, final double median,
                                final double timePerMove) {
            this.net = net;

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
            return this.getClass().getSimpleName() + " ( " + this.net + " )"
                    + "\n\t\t\t\tMean: " + this.arthMean
                    + "\t\tGeoMean: " + this.geoMean
                    + "\t\tMedian: " + this.median
                    + "\t\tMinMaxGeo: " + this.minMaxGeo
                    + "\n\t\t\t\tComposite: " + this.composite
                    + "\t\tTime Per Move: " + this.timePerMove
                    + "\t\tWeighted Score: " + this.weightedScore;
        }
    }
}
