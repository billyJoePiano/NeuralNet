package neuralNet.test;

import game2048.*;

import java.io.*;

import static neuralNet.test.TestUtil.*;

public class TestDeserialize {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        BoardNet[] nets;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(TestSerialize.FILENAME))) {
            nets = (BoardNet[])in.readObject();
        }

        BoardNet[] orig = new BoardNet[] { TestBoardNet.makeEdgeNet(), TestBoardNet.makeRandomNet() };

        boolean equals = compareObjects(nets[0], orig[0]);

        System.out.println("EDGE COMPARISON: " + equals);
        System.out.println("\n\n\n\n");

        equals = compareObjects(nets[1], orig[1]);

        System.out.println("RANDOM COMPARISON: " + equals);
    }
}
