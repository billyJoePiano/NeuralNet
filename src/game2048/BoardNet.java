package game2048;

import neuralNet.network.*;
import neuralNet.neuron.*;

import java.util.*;

public class BoardNet extends NeuralNet<BoardInterface, BoardNet, BoardInterface> {
    private static final long serialVersionUID = 9054824145968924217L;

    private static final short[] NEURAL_OUTPUTS = new short[] {
            Short.MIN_VALUE,        //empty
            -28913,     //2
            -25058,     //4
            -21203,     //8
            -17348,     //16
            -13493,     //32
            -9638,      //64
            -5783,      //128
            -1928,      //256
            1928,       //512
            5783,       //1024
            9638,       //2048
            13493,      //4096
            17348,      //8192
            21203,      //16384
            25058,      //32768
            28913,      //65536
            Short.MAX_VALUE     //131072
        };

    private final Sensor[][] matrix;
    private final List<Sensor> sensors;


    public final List<Decision> decisionNodes = List.of(new Up(), new Down(), new Left(), new Right(), this.getNoOp());

    /*
    protected Object readResolve() throws ObjectStreamException {
        return new BoardNet(this, null);
    }

    private BoardNet(BoardNet deserializedFrom, Void v) {
        super(deserializedFrom, null);
        this.matrix = deserializedFrom.matrix;
        this.sensors = deserializedFrom.sensors;
    }
     */

    public BoardNet() {
        super();
        this.matrix = makeSensors();
        this.sensors = makeSensorList();
    }


    //TODO constructors for consumers substitutions map
    public BoardNet(BoardNet cloneFrom) {
        super(cloneFrom);
        this.matrix = makeSensors();
        this.sensors = makeSensorList();
        this.cloneNeurons(cloneFrom, null, null);
    }

    public BoardNet(BoardNet cloneFrom,
                    Map<SignalProvider, SignalProvider> providersMap,
                    Map<SignalConsumer, SignalConsumer> consumersMap) {

        super(cloneFrom);
        this.matrix = makeSensors();
        this.sensors = makeSensorList();
        this.cloneNeurons(cloneFrom, providersMap, consumersMap);
    }

    private Sensor[][] makeSensors() {
        //List<Sensor> sensors = new ArrayList<>(16);
        Sensor[][] matrix = new Sensor[4][4];

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                matrix[i][j] = new Sensor(i, j);
            }
        }
        return matrix;
    }

    private List<Sensor> makeSensorList() {
        return List.of(matrix[0][0], matrix[0][1], matrix[0][2], matrix[0][3],
                matrix[1][0], matrix[1][1], matrix[1][2], matrix[1][3],
                matrix[2][0], matrix[2][1], matrix[2][2], matrix[2][3],
                matrix[3][0], matrix[3][1], matrix[3][2], matrix[3][3]);
    }

    @Override
    public BoardNet clone() {
        return new BoardNet(this);
    }


    @Override
    public BoardNet cloneWith(Map<SignalProvider, SignalProvider> providersMap,
                              Map<SignalConsumer, SignalConsumer> consumersMap) {

        return new BoardNet(this, providersMap, consumersMap);
    }

    @Override
    public List<Sensor> getSensors() {
        return this.sensors;
    }

    @Override
    public List<Decision> getDecisionNodes() {
        return this.decisionNodes;
    }

    public Sensor[][] getSensorMatrix() {
        return this.matrix.clone();
    }

    public class Sensor extends NeuralNet<BoardInterface, BoardNet, BoardInterface>.Sensor {
        public final int row;
        public final int col;

        private Sensor(int row, int col) {
            this.row = row;
            this.col = col;
        }

        @Override
        public int getSensorId() {
            return this.row * 4 + this.col;
        }

        @Override
        public short sense() {
            short val = NEURAL_OUTPUTS[this.getSensedObject().getTile(this.row, this.col)];
            this.setCache(val);
            return val;
        }
    }

    public class Up extends Decision {
        private Up() { }

        @Override
        public int getDecisionId() {
            return 0;
        }
    }

    public class Down extends Decision {
        private Down() { }

        @Override
        public int getDecisionId() {
            return 1;
        }
    }

    public class Left extends Decision {
        private Left() { }

        @Override
        public int getDecisionId() {
            return 2;
        }
    }

    public class Right extends Decision {
        private Right() { }

        @Override
        public int getDecisionId() {
            return 3;
        }
    }
}
