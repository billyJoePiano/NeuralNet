package game2048;

import net.*;
import neuron.*;

import java.util.*;
import java.util.function.*;

public class BoardNet implements NeuralNet<Board, Board, BoardNet> {
    private Board board;

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

    public BoardNet(Board board,
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

         */

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

    @Override
    public BoardNet deepClone() {
        return null;
    }

    public class Sensor implements SensorNode<Board, Sensor, BoardNet> {
        private final int row;
        private final int col;

        private short signal;

        private Setter current = null;

        private Sensor(int row, int col) {
            this.row = row;
            this.col = col;
        }

        @Override
        public Board getObservedObject() {
            return BoardNet.this.board;
        }

        @Override
        public BoardNet getNeuralNet() {
            return BoardNet.this;
        }

        @Override
        public int getOutputId() {
            return row * 4 + col;
        }

        @Override
        public short getOutput() {
            return signal;
        }


        //TODO!!!!!  implement the below
        @Override
        public Set<SignalConsumer> getConsumers() {
            return null;
        }

        @Override
        public boolean addConsumer(SignalConsumer consumer) {
            return false;
        }

        @Override
        public boolean removeConsumer(SignalConsumer consumer) {
            return false;
        }

        @Override
        public void clearConsumers() {

        }

        @Override
        public SignalProvider clone() {
            return null;
        }

        private Setter newSetter() {
            if (this.current != null) {
                this.current.isCurrent = false;
            }
            return this.current = new Setter();
        }

        public class Setter implements SensorNode.Setter<Board, Sensor> {
            private boolean isCurrent = true;

            @Override
            public void setSignal(short signal) {
                if (isCurrent) {
                    Sensor.this.signal = signal;

                } else {
                    throw new IllegalStateException("Attempting to use a sensor setter that was replaced");
                }
            }

            @Override
            public Sensor getSensor() {
                return Sensor.this;
            }

            @Override
            public int hashCode() {
                return Sensor.this.hashCode();
            }

            public boolean equals(Object other) {
                if (other == this) return true;
                if (other == null || other.getClass() != this.getClass()) return false;
                Setter o = (Setter) other;
                return o.getSensor() == this.getSensor();
            }
        }
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
}
