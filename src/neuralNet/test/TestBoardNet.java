package neuralNet.test;

import game2048.*;
import neuralNet.function.*;
import neuralNet.network.*;
import neuralNet.neuron.*;

import java.util.*;

public class TestBoardNet {
    public static void main(String[] args) {
        BoardNet net = new BoardNet();
        BoardNet.Sensor[][] sensors = net.getSensorMatrix();

        Neuron top = Average.makeNeuron(
                sensors[0][0], sensors[0][1], sensors[0][2], sensors[0][3],
                sensors[0][0], sensors[0][1], sensors[0][2], sensors[0][3],
                sensors[1][0], sensors[1][1], sensors[1][2], sensors[1][3]);

        Neuron bottom = new CachingNeuronUsingFunction(Average.instance,
                sensors[3][0], sensors[3][1], sensors[3][2], sensors[3][3],
                sensors[3][0], sensors[3][1], sensors[3][2], sensors[3][3],
                sensors[2][0], sensors[2][1], sensors[2][2], sensors[2][3]);

        Neuron left = new CachingNeuronUsingFunction(Average.instance,
                sensors[0][0], sensors[1][0], sensors[2][0], sensors[3][0],
                sensors[0][0], sensors[1][0], sensors[2][0], sensors[3][0],
                sensors[0][1], sensors[1][1], sensors[2][1], sensors[3][1]);

        Neuron right = new CachingNeuronUsingFunction(Average.instance,
                sensors[0][3], sensors[1][3], sensors[2][3], sensors[3][3],
                sensors[0][3], sensors[1][3], sensors[2][3], sensors[3][3],
                sensors[0][2], sensors[1][2], sensors[2][2], sensors[3][2]);

        Neuron center = new CachingNeuronUsingFunction(Average.instance,
                sensors[1][1], sensors[1][1], sensors[1][1],
                sensors[1][2], sensors[1][2], sensors[1][2],
                sensors[2][1], sensors[2][1], sensors[2][1],
                sensors[2][2], sensors[2][2], sensors[2][2],
                sensors[0][1], sensors[0][2], sensors[1][0], sensors[2][0],
                sensors[3][1], sensors[3][2], sensors[1][3], sensors[2][3]);

        List<? extends DecisionNode> decisions = net.getDecisionNodes();

        decisions.get(0).setInputs(List.of(top));
        decisions.get(1).setInputs(List.of(bottom));
        decisions.get(2).setInputs(List.of(left));
        decisions.get(3).setInputs(List.of(right));

        BoardInterface board = new BoardInterface();
        Fitness fitness = board.testFitness(net, null);
        System.out.println(fitness);
    }

}
