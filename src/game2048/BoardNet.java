package game2048;

import neuralNet.network.*;
import neuralNet.neuron.*;

import java.util.*;

public class BoardNet extends NeuralNet<BoardInterface, BoardNet, BoardInterface> {
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

    private BoardInterface board;
    public final List<Sensor> sensors = this.makeSensors();

    private List<Sensor> makeSensors() {
        List<Sensor> sensors = new ArrayList<>(16);
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                sensors.add(new Sensor(i, j));
            }
        }
        return Collections.unmodifiableList(sensors);
    }

    @Override
    public List<SignalProvider> getNeurons() {
        return null;
    }

    @Override
    public BoardNet clone() {
        return null;
    }

    @Override
    public List<SensorNode<BoardInterface, BoardNet>> getSensors() {
        return null;
    }

    @Override
    public List<DecisionNode<BoardNet, BoardInterface>> getDecisionNodes() {
        return null;
    }

    public class Sensor extends CachingProvider implements SensorNode<BoardInterface, BoardNet> {
        public final int row;
        public final int col;

        private Sensor(int row, int col) {
            this.row = row;
            this.col = col;
        }

        @Override
        public BoardInterface getObservedObject() {
            return BoardNet.this.board;
        }

        @Override
        public BoardNet getDecisionProvider() {
            return BoardNet.this;
        }

        @Override
        public void sense() {
            this.setCache(NEURAL_OUTPUTS[BoardNet.this.board.getTile(this.row, this.col)]);
        }

        @Override
        protected short calcOutput(List<SignalProvider> inputs) {
            //sense() should always be called before this, therefore the cache should always be
            // already-populated when getOutput() is called, and this method should never be needed
            throw new UnsupportedOperationException();
        }
    }

    //TODO decision nodes
}
