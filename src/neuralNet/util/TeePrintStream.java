package neuralNet.util;

import java.io.*;
import java.util.*;

public class TeePrintStream extends PrintStream {
    private final PrintStream[] streams;

    public TeePrintStream(PrintStream ... streams) {
        super(new OutputStream() { //dummy, does nothing
            @Override
            public void write(int i) { }
        });
        this.streams = streams.clone();
    }

    @Override
    public void flush() {
        for (PrintStream stream : streams) {
            stream.flush();
        }
    }

    @Override
    public void close() {
        for (PrintStream stream : streams) {
            stream.close();
        }
    }

    @Override
    public boolean checkError() {
        boolean error = false;
        for (PrintStream stream : streams) {
            error = stream.checkError() || error;
        }
        return error;
    }

    @Override
    protected void setError() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void clearError() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(int b) {
        for (PrintStream stream : streams) {
            stream.write(b);
        }
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        for (PrintStream stream : streams) {
            stream.write(buf, off, len);
        }
    }

    @Override
    public void write(byte[] buf) throws IOException {
        for (PrintStream stream : streams) {
            stream.write(buf);
        }
    }

    @Override
    public void writeBytes(byte[] buf) {
        for (PrintStream stream : streams) {
            stream.writeBytes(buf);
        }
    }

    @Override
    public void print(boolean b) {
        for (PrintStream stream : streams) {
            stream.print(b);
        }
    }

    @Override
    public void print(char c) {
        for (PrintStream stream : streams) {
            stream.print(c);
        }
    }

    @Override
    public void print(int i) {
        for (PrintStream stream : streams) {
            stream.print(i);
        }
    }

    @Override
    public void print(long l) {
        for (PrintStream stream : streams) {
            stream.print(l);
        }
    }

    @Override
    public void print(float f) {
        for (PrintStream stream : streams) {
            stream.print(f);
        }
    }

    @Override
    public void print(double d) {
        for (PrintStream stream : streams) {
            stream.print(d);
        }
    }

    @Override
    public void print(char[] s) {
        for (PrintStream stream : streams) {
            stream.print(s);
        }
    }

    @Override
    public void print(String s) {
        for (PrintStream stream : streams) {
            stream.print(s);
        }
    }

    @Override
    public void print(Object obj) {
        for (PrintStream stream : streams) {
            stream.print(obj);
        }
    }

    @Override
    public void println() {
        for (PrintStream stream : streams) {
            stream.println();
        }
    }

    @Override
    public void println(boolean x) {
        for (PrintStream stream : streams) {
            stream.println(x);
        }
    }

    @Override
    public void println(char x) {
        for (PrintStream stream : streams) {
            stream.println(x);
        }
    }

    @Override
    public void println(int x) {
        for (PrintStream stream : streams) {
            stream.println(x);
        }
    }

    @Override
    public void println(long x) {
        for (PrintStream stream : streams) {
            stream.println(x);
        }
    }

    @Override
    public void println(float x) {
        for (PrintStream stream : streams) {
            stream.println(x);
        }
    }

    @Override
    public void println(double x) {
        for (PrintStream stream : streams) {
            stream.println(x);
        }
    }

    @Override
    public void println(char[] x) {
        for (PrintStream stream : streams) {
            stream.println(x);
        }
    }

    @Override
    public void println(String x) {
        for (PrintStream stream : streams) {
            stream.println(x);
        }
    }

    @Override
    public void println(Object x) {
        for (PrintStream stream : streams) {
            stream.println(x);
        }
    }

    @Override
    public PrintStream printf(String format, Object... args) {
        for (PrintStream stream : streams) {
            stream.printf(format, args);
        }
        return this;
    }

    @Override
    public PrintStream printf(Locale l, String format, Object... args) {
        for (PrintStream stream : streams) {
            stream.printf(l, format, args);
        }
        return this;
    }

    @Override
    public PrintStream format(String format, Object... args) {
        for (PrintStream stream : streams) {
            stream.format(format, args);
        }
        return this;
    }

    @Override
    public PrintStream format(Locale l, String format, Object... args) {
        PrintStream str = null;
        for (PrintStream stream : streams) {
            stream.format(l, format, args);
        }
        return this;
    }

    @Override
    public PrintStream append(CharSequence csq) {
        for (PrintStream stream : streams) {
            stream.append(csq);
        }
        return this;
    }

    @Override
    public PrintStream append(CharSequence csq, int start, int end) {
        for (PrintStream stream : streams) {
            stream.append(csq, start, end);
        }
        return this;
    }

    @Override
    public PrintStream append(char c) {
        for (PrintStream stream : streams) {
            stream.append(c);
        }
        return this;
    }
}
