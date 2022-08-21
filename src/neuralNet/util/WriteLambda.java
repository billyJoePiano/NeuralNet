package neuralNet.util;

import java.io.*;

public interface WriteLambda {
    public void writeln(CharSequence text) throws IOException;

    default public void writeln() throws IOException {
        writeln("");
    }
}
