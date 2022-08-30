package neuralNet.test;

import game2048.*;
import game2048.BoardInterface.*;
import net.openhft.affinity.*;
import neuralNet.function.*;
import neuralNet.neuron.*;

import java.util.*;

import static neuralNet.util.Util.*;

public class TestBoardNet extends Thread {
    public static BoardNet makeEdgeNet() {
        BoardNet net = new BoardNet();
        BoardNet.Sensor[][] sensors = net.getSensorMatrix();

        Neuron top = WeightedAverage.makeNeuron(List.of(
                        sensors[0][0], sensors[0][1], sensors[0][2], sensors[0][3],
                        sensors[1][0], sensors[1][1], sensors[1][2], sensors[1][3]),
                2, 2, 2, 2, 1, 1, 1, 1);

        Neuron bottom = WeightedAverage.makeNeuron(List.of(
                        sensors[3][0], sensors[3][1], sensors[3][2], sensors[3][3],
                        sensors[2][0], sensors[2][1], sensors[2][2], sensors[2][3]),
                2, 2, 2, 2, 1, 1, 1, 1);

        Neuron left = WeightedAverage.makeNeuron(List.of(
                        sensors[0][0], sensors[1][0], sensors[2][0], sensors[3][0],
                        sensors[0][1], sensors[1][1], sensors[2][1], sensors[3][1]),
                2, 2, 2, 2, 1, 1, 1, 1);

        Neuron right = WeightedAverage.makeNeuron(List.of(
                        sensors[0][3], sensors[1][3], sensors[2][3], sensors[3][3],
                        sensors[0][2], sensors[1][2], sensors[2][2], sensors[3][2]),
                2, 2, 2, 2, 1, 1, 1, 1);

        Neuron center = WeightedAverage.makeNeuron(List.of(
                        sensors[1][1], sensors[1][2], sensors[2][1], sensors[2][2],
                        sensors[0][1], sensors[0][2], sensors[1][0], sensors[2][0],
                        sensors[3][1], sensors[3][2], sensors[1][3], sensors[2][3]),
                3, 3, 3, 3, 1, 1, 1, 1, 1, 1, 1, 1);

        Neuron max = Max.makeNeuron(top, bottom, left, right);
        Neuron centerStronger = Widen.makeNeuron(DifferenceClipped.makeNeuron(center, max));

        Neuron negateTop = NegateBalanced.makeNeuron(top);
        Neuron negateBottom = NegateBalanced.makeNeuron(bottom);
        Neuron negateLeft = NegateBalanced.makeNeuron(left);
        Neuron negateRight = NegateBalanced.makeNeuron(right);

        Neuron switchTop = HardSwitch.makeNeuron(centerStronger, top, negateTop);
        Neuron switchBottom = HardSwitch.makeNeuron(centerStronger, bottom, negateBottom);
        Neuron switchLeft = HardSwitch.makeNeuron(centerStronger, left, negateLeft);
        Neuron switchRight = HardSwitch.makeNeuron(centerStronger, right, negateRight);

        List<BoardNet.Decision> decisions = net.getDecisionNodes();

        decisions.get(0).setInputs(List.of(switchTop));
        decisions.get(1).setInputs(List.of(switchBottom));
        decisions.get(2).setInputs(List.of(switchLeft));
        decisions.get(3).setInputs(List.of(switchRight));

        return net.traceNeuronsSet();
        //return net;
    }

    /**
     * Makes a BoardNet which just makes decisions at random (note: NOT random neural arrangement, just
     * a Random Value neuron attached to each decision node)
     *
     * @return
     */
    public static BoardNet makeRandomNet() {
        BoardNet rand = new BoardNet();
        List<BoardNet.Decision> decisions = rand.getDecisionNodes();
        decisions.get(0).setInputs(List.of(RandomValue.makeNeuron()));
        decisions.get(1).setInputs(List.of(RandomValue.makeNeuron()));
        decisions.get(2).setInputs(List.of(RandomValue.makeNeuron()));
        decisions.get(3).setInputs(List.of(RandomValue.makeNeuron()));

        return rand.traceNeuronsSet();
        //return rand;
    }


    public static final int ITERATIONS = 1026;
    public static final int THREADS = 6;

    private final int startIndex, endIndex; // inclusive, exclusive

    private final double[] edgeScores, randScores;

    private int edgeCount, randCount, edgeGameMin, edgeGameMax, randGameMin, randGameMax;
    private double edgeAvg, randAvg, edgeCompMin, edgeCompMax, randCompMin, randCompMax, edgeStd, randStd;

    private TestBoardNet(final int startIndex, final int endIndex,
                         final double[] edgeScores, final double[] randScores) {

        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.edgeScores = edgeScores;
        this.randScores = randScores;
    }

    public static void main(String[] args) {
        double start = System.nanoTime();

        double[] edgeScores = new double[ITERATIONS];
        double[] randScores = new double[ITERATIONS];

        int lastEnd = 0;
        TestBoardNet[] threads = new TestBoardNet[THREADS];

        for (int i = 0; i < THREADS; i++) {
            int thisEnd = (int)Math.round((i + 1) * (double)ITERATIONS / (double)THREADS);
            threads[i] = new TestBoardNet(lastEnd, thisEnd, edgeScores, randScores);
            lastEnd = thisEnd;
            threads[i].start();
        }

        for (TestBoardNet test : threads) {
            try {
                test.join();
            } catch (InterruptedException e) {
                System.err.println(e);
                e.printStackTrace(System.err);
            }
        }


        int edgeCount = 0;
        int randCount = 0;
        double edgeAvg = 0;
        double randAvg = 0;

        int edgeGameMin = Integer.MAX_VALUE;
        int edgeGameMax = Integer.MIN_VALUE;
        int randGameMin = Integer.MAX_VALUE;
        int randGameMax = Integer.MIN_VALUE;

        double edgeCompMin = Double.POSITIVE_INFINITY;
        double edgeCompMax = Double.NEGATIVE_INFINITY;
        double randCompMin = Double.POSITIVE_INFINITY;
        double randCompMax = Double.NEGATIVE_INFINITY;
        
        for (TestBoardNet test : threads) {
            edgeCount += test.edgeCount;
            randCount += test.randCount;
            edgeAvg += test.edgeAvg;
            randAvg += test.randAvg;

            if (test.edgeGameMin < edgeGameMin) edgeGameMin = test.edgeGameMin;
            if (test.edgeGameMax > edgeGameMax) edgeGameMax = test.edgeGameMax;
            if (test.randGameMin < randGameMin) randGameMin = test.randGameMin;
            if (test.randGameMax > randGameMax) randGameMax = test.randGameMax;

            if (test.edgeCompMin < edgeCompMin) edgeCompMin = test.edgeCompMin;
            if (test.edgeCompMax > edgeCompMax) edgeCompMax = test.edgeCompMax;
            if (test.randCompMin < randCompMin) randCompMin = test.randCompMin;
            if (test.randCompMax > randCompMax) randCompMax = test.randCompMax;
        }

        edgeAvg /= THREADS;
        randAvg /= THREADS;

        double edgeStd = 0;
        double randStd = 0;

        for (double edge : edgeScores) {
            double diff = edge - edgeAvg;
            edgeStd += diff * diff;
        }

        for (double rand : randScores) {
            double diff = rand - randAvg;
            randStd += diff * diff;
        }

        edgeStd = Math.sqrt(edgeStd / ITERATIONS);
        randStd = Math.sqrt(randStd / ITERATIONS);

        double end = System.nanoTime();


        System.out.println("FINAL STATS");
        System.out.println();
        System.out.println("Edge\n\tAverage: " + edgeAvg + "\tStd:" + edgeStd + "\tCount: " + edgeCount + "\tComposite Min: " + edgeCompMin + "\tComposite Max: " + edgeCompMax + "\tGame Max: " + edgeGameMax + "\tGame Min: " + edgeGameMin);
        System.out.println("");
        System.out.println("Random\n\tAverage: " + randAvg + "\tStd:" + randStd + "\tCount: " + randCount + "\tComposite Min: " + randCompMin + "\tComposite Max: " + randCompMax + "\tGame Max: " + randGameMax + "\tGame Min: " + randGameMin);
        System.out.println();
        System.out.println("BENCHMARK: " + ((end - start) / BILLION) + " seconds");

    }

    public void run() {
        try (AffinityLock af = obtainUniqueLock()) {
            System.out.println(this.getName() + "\tCPU Id:" + af.cpuId());
            runTrials();
        }
    }

    public void runTrials() {
        BoardNet edgeNet = makeEdgeNet();
        BoardNet randNet = makeRandomNet();

        BoardInterface board = new BoardInterface();

        edgeCount = 0;
        randCount = 0;
        edgeAvg = 0;
        randAvg = 0;

        edgeGameMin = Integer.MAX_VALUE;
        edgeGameMax = Integer.MIN_VALUE;
        randGameMin = Integer.MAX_VALUE;
        randGameMax = Integer.MIN_VALUE;
        
        edgeCompMin = Double.POSITIVE_INFINITY;
        edgeCompMax = Double.NEGATIVE_INFINITY;
        randCompMin = Double.POSITIVE_INFINITY;
        randCompMax = Double.NEGATIVE_INFINITY;

        for (int i = startIndex; i < endIndex; i++) {
            BoardNetFitness edge = board.testFitness(edgeNet, null);
            BoardNetFitness rand = board.testFitness(randNet, null);

            System.out.println("Edge decisions\n" + edge + "\n\nRandom decisions\n" + rand
                    + "\n\n----------------------------------------------------------------------------\n\n\n");

            edgeScores[i] = edge.composite;
            randScores[i] = rand.composite;

            edgeAvg += edge.composite;
            randAvg += rand.composite;

            if (edge.composite > rand.composite) edgeCount++;
            else randCount++;

            if (edge.min < edgeGameMin) edgeGameMin = edge.min;
            if (edge.max > edgeGameMax) edgeGameMax = edge.max;
            if (rand.min < randGameMin) randGameMin = rand.min;
            if (rand.max > randGameMax) randGameMax = rand.max;

            if (edge.composite < edgeCompMin) edgeCompMin = edge.composite;
            if (edge.composite > edgeCompMax) edgeCompMax = edge.composite;
            if (rand.composite < randCompMin) randCompMin = rand.composite;
            if (rand.composite > randCompMax) randCompMax = rand.composite;
        }

        edgeAvg /= endIndex - startIndex;
        randAvg /= endIndex - startIndex;;
    }

}
