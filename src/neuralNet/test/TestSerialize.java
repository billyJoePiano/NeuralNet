package neuralNet.test;

import game2048.*;

import java.io.*;

public class TestSerialize {
    public static final String FILENAME = "TestSerialize.nnt";

    public static void main(String[] args) throws IOException {
        BoardNet edge = TestBoardNet.makeEdgeNet();
        BoardNet rand = TestBoardNet.makeRandomNet();
        BoardInterface board = new BoardInterface();

        board.testFitness(edge, null);
        board.testFitness(rand, null);

        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(FILENAME))) {
            out.writeObject(new BoardNet[] {edge, rand});
        }
    }
}
