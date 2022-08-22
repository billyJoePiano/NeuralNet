package game2048;

import neuralNet.network.*;
import neuralNet.neuron.*;

import java.util.*;
import java.util.function.*;

public class BoardInterface extends Board implements
        Sensable<BoardInterface>,
        DecisionConsumer<BoardInterface, BoardInterface, BoardInterface.BoardNetFitness> {

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
        this.reset();
    }

    @Override
    public int numOutputs() {
        return 0;
    }

    @Override
    public void updateSensors() {

    }

    @Override
    public void addSensor(SensorNode.Setter<O, ?> sensorSetter) throws IllegalStateException, NullPointerException {

    }

    @Override
    public boolean removeSensor(SensorNode.Setter<O, ?> sensorSetter) throws IllegalStateException, NullPointerException {
        return false;
    }

    @Override
    public boolean findAndRemoveSensor(SensorNode<BoardInterface, ?> sensor) throws IllegalStateException, NullPointerException {
        return false;
    }

    @Override
    public Set<SensorNode.Setter<O, ?>> getSensorSetters(int outputId) {
        return null;
    }

    @Override
    public Map<Integer, Set<SensorNode.Setter<O, ?>>> getSensorSettersMap() {
        return null;
    }

    @Override
    public Set<SensorNode.Setter<O, ?>> getAllSensorSetters() {
        return null;
    }


    public class BoardNetFitness implements Fitness<BoardInterface, BoardNetFitness> {

        @Override
        public int compareTo(BoardNetFitness boardNetFitness) {
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
