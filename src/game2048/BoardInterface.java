package game2048;

import net.openhft.affinity.*;
import neuralNet.network.*;
import neuralNet.util.*;

import java.util.*;

import static neuralNet.util.Util.*;

public class BoardInterface extends Board implements
        Sensable<BoardInterface>,
        DecisionConsumer<BoardInterface, BoardInterface, BoardInterface.BoardNetFitness> {

    public static final double MAX_SCORE = 262140;
    public static final double GAMES_PER_TEST = 16;

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

        double score = 0;
        AccumulatedAverage timePerMove = new AccumulatedAverage();

        try (AffinityLock af = AffinityLock.acquireCore()) {

            for (int i = 0; i < GAMES_PER_TEST; i++) {
                this.reset();
                while (this.isActive()) {
                    long start = System.nanoTime();
                    this.runRound(boardNet);
                    long end = System.nanoTime();
                    timePerMove.add((end - start) / BILLION);
                }
                System.out.println(this + "" + this.getScore() + "\n\n");
                score += this.getScore();
            }
            System.out.println(score / GAMES_PER_TEST);
            return new BoardNetFitness(score / GAMES_PER_TEST, timePerMove.getAverage());
        }
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

        public final double avgScore;
        public final double timePerMove;
        public final double weightedScore;

        private BoardNetFitness(final double avgScore, final double timePerMove) {
            this.avgScore = avgScore;
            this.timePerMove = timePerMove;

            if (timePerMove > TIME_PER_MOVE_THRESHOLD) {
                double diff = timePerMove - TIME_PER_MOVE_THRESHOLD;
                this.weightedScore = (avgScore + TIME_WEIGHTING * (avgScore / (1 + diff))) / TOTAL_WEIGHTING;

            } else {
                this.weightedScore = avgScore;
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
            if (this.avgScore > other.avgScore) return -1;
            if (this.avgScore < other.avgScore) return 1;

            if (this.timePerMove < other.timePerMove) return -1;
            if (this.timePerMove > other.timePerMove) return 1;

            return 0;
        }
    }

    /*
    private SignalProvider up;
    private SignalProvider down;
    private SignalProvider left;
    private SignalProvider right;

    private final Set<Sensor> sensors = makeSensors();

    private Set<Sensor> makeSensors() {
        Set<Sensor> sensors = new HashSet<>();
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                sensors.add(new Sensor(i, j));
            }
        }
        return Collections.unmodifiableSet(sensors);
    }

    public BoardInterface(Board board,
                          SignalProvider up,
                          SignalProvider down,
                          SignalProvider left,
                          SignalProvider right)
            throws NullPointerException {

        if (board == null || up == null || down == null || left == null || right == null) {
            throw new NullPointerException();
        }

        setBoard(board);

        this.up = up;
        this.down = down;
        this.left = left;
        this.right = right;
    }


    public Board getBoard() {
        return board;
    }

    public void setBoard(Board board) throws NullPointerException {
        if (board == null) throw new NullPointerException();
        if (this.board != null) {
            for (Sensor sensor : sensors) {
                board.removeSensor(sensor.current);
            }
        }

        this.board = board;

        for (Sensor sensor : sensors) {
            board.addSensor(sensor.newSetter());
        }
    }


    public SignalProvider getUp() {
        return up;
    }

    public void setUp(SignalProvider up) throws NullPointerException {
        if (up == null) throw new NullPointerException();
        this.up = up;
    }

    public SignalProvider getDown() {
        return down;
    }

    public void setDown(SignalProvider down) {
        if (down == null) throw new NullPointerException();
        this.down = down;
    }

    public SignalProvider getLeft() {
        return left;
    }

    public void setLeft(SignalProvider left) throws NullPointerException {
        if (left == null) throw new NullPointerException();
        this.left = left;
    }

    public SignalProvider getRight() {
        return right;
    }

    public void setRight(SignalProvider right) throws NullPointerException {
        if (right == null) throw new NullPointerException();
        this.right = right;
    }

    public boolean makeMove() {
        /*
        this.up.clearCache();
        this.down.clearCache();
        this.left.clearCache();
        this.right.clearCache();

         *//*

        List<Decision> decisions = new ArrayList<>();

        decisions.add(new Decision(this.board::up, this.up.getOutput()));
        decisions.add(new Decision(this.board::down, this.down.getOutput()));
        decisions.add(new Decision(this.board::left, this.left.getOutput()));
        decisions.add(new Decision(this.board::right, this.right.getOutput()));

        Collections.sort(decisions);

        for (Decision decision : decisions) {
            if (decision.action.get()) return true;
        }
        return false;
    }

    @Override
    public Set<Sensor> getSensors() {
        return this.sensors;
    }

    @Override
    public void setSensedObject(Board board) {
        this.setBoard(board);
    }

    @Override
    public Board getSensedObject() {
        return null;
    }

    @Override
    public List<DecisionNode<Board, ?, BoardNet>> getDecisionNodes() {
        return null;
    }




    private class Decision implements Comparable<Decision> {
        private final Supplier<Boolean> action;
        private final short weight;
        private Double rand; // for rare cases where two Decisions have the same weight

        private Decision(Supplier<Boolean> action, short weight) {
            this.action = action;
            this.weight = weight;
        }

        @Override
        public int compareTo(Decision other) {
            if (other == this) return 0;
            else if (this.weight > other.weight) return -1;
            else if (other.weight < this.weight) return 1;

            if (this.rand == null) this.rand = Math.random();
            if (other.rand == null) other.rand = Math.random();

            return this.rand < other.rand ? -1 : 1;
        }
    }
    */
}
