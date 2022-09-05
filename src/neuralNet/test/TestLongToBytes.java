package neuralNet.test;

import neuralNet.neuron.*;

import java.util.concurrent.*;

import static neuralNet.neuron.NeuralHash.toHex;

public class TestLongToBytes {
    public static final int ITERATIONS = 1024;
    public static void main(String[] args) {
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        for (int i = 0; i < ITERATIONS; i++) {
            long in = rand.nextLong();
            byte[] bytes = NeuralHash.longToBytes(in);
            long out = NeuralHash.bytesToLong(bytes);


            System.out.println(toHex(in));
            System.out.println(NeuralHash.toHex(bytes));
            System.out.println();

            assert in == out;
        }
    }
}
