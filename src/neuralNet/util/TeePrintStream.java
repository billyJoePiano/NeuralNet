package neuralNet.util;

import java.io.*;
import java.util.*;

public class TeePrintStream extends PrintStream {
    private final PrintStream[] streams;

    public TeePrintStream(PrintStream ... streams) {
        super(new OutputStream() { //dummy, does nothing
            @Override
            public synchronized void write(int i) { }
        });
        this.streams = streams.clone();
    }

    @Override
    public synchronized void flush() {
        for (PrintStream stream : streams) {
            stream.flush();
        }
    }

    @Override
    public synchronized void close() {
        for (PrintStream stream : streams) {
            stream.close();
        }
    }

    @Override
    public synchronized boolean checkError() {
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
    public synchronized void write(int b) {
        for (PrintStream stream : streams) {
            stream.write(b);
        }
    }

    @Override
    public synchronized void write(byte[] buf, int off, int len) {
        for (PrintStream stream : streams) {
            stream.write(buf, off, len);
        }
    }

    @Override
    public synchronized void write(byte[] buf) throws IOException {
        for (PrintStream stream : streams) {
            stream.write(buf);
        }
    }

    @Override
    public synchronized void writeBytes(byte[] buf) {
        for (PrintStream stream : streams) {
            stream.writeBytes(buf);
        }
    }

    @Override
    public synchronized void print(boolean b) {
        for (PrintStream stream : streams) {
            stream.print(b);
        }
    }

    @Override
    public synchronized void print(char c) {
        for (PrintStream stream : streams) {
            stream.print(c);
        }
    }

    @Override
    public synchronized void print(int i) {
        for (PrintStream stream : streams) {
            stream.print(i);
        }
    }

    @Override
    public synchronized void print(long l) {
        for (PrintStream stream : streams) {
            stream.print(l);
        }
    }

    @Override
    public synchronized void print(float f) {
        for (PrintStream stream : streams) {
            stream.print(f);
        }
    }

    @Override
    public synchronized void print(double d) {
        for (PrintStream stream : streams) {
            stream.print(d);
        }
    }

    @Override
    public synchronized void print(char[] s) {
        for (PrintStream stream : streams) {
            stream.print(s);
        }
    }

    @Override
    public synchronized void print(String s) {
        for (PrintStream stream : streams) {
            stream.print(s);
        }
    }

    @Override
    public synchronized void print(Object obj) {
        for (PrintStream stream : streams) {
            stream.print(obj);
        }
    }

    @Override
    public synchronized void println() {
        for (PrintStream stream : streams) {
            stream.println();
        }
    }

    @Override
    public synchronized void println(boolean x) {
        for (PrintStream stream : streams) {
            stream.println(x);
        }
    }

    @Override
    public synchronized void println(char x) {
        for (PrintStream stream : streams) {
            stream.println(x);
        }
    }

    @Override
    public synchronized void println(int x) {
        for (PrintStream stream : streams) {
            stream.println(x);
        }
    }

    @Override
    public synchronized void println(long x) {
        for (PrintStream stream : streams) {
            stream.println(x);
        }
    }

    @Override
    public synchronized void println(float x) {
        for (PrintStream stream : streams) {
            stream.println(x);
        }
    }

    @Override
    public synchronized void println(double x) {
        for (PrintStream stream : streams) {
            stream.println(x);
        }
    }

    @Override
    public synchronized void println(char[] x) {
        for (PrintStream stream : streams) {
            stream.println(x);
        }
    }

    @Override
    public synchronized void println(String x) {
        for (PrintStream stream : streams) {
            stream.println(x);
        }
    }

    @Override
    public synchronized void println(Object x) {
        for (PrintStream stream : streams) {
            stream.println(x);
        }
    }

    @Override
    public synchronized PrintStream printf(String format, Object... args) {
        for (PrintStream stream : streams) {
            stream.printf(format, args);
        }
        return this;
    }

    @Override
    public synchronized PrintStream printf(Locale l, String format, Object... args) {
        for (PrintStream stream : streams) {
            stream.printf(l, format, args);
        }
        return this;
    }

    @Override
    public synchronized PrintStream format(String format, Object... args) {
        for (PrintStream stream : streams) {
            stream.format(format, args);
        }
        return this;
    }

    @Override
    public synchronized PrintStream format(Locale l, String format, Object... args) {
        PrintStream str = null;
        for (PrintStream stream : streams) {
            stream.format(l, format, args);
        }
        return this;
    }

    @Override
    public synchronized PrintStream append(CharSequence csq) {
        for (PrintStream stream : streams) {
            stream.append(csq);
        }
        return this;
    }

    @Override
    public synchronized PrintStream append(CharSequence csq, int start, int end) {
        for (PrintStream stream : streams) {
            stream.append(csq, start, end);
        }
        return this;
    }

    @Override
    public synchronized PrintStream append(char c) {
        for (PrintStream stream : streams) {
            stream.append(c);
        }
        return this;
    }
}
