package neuralNet.util;

import neuralNet.network.*;

import java.io.*;
import java.util.*;

public class GenerationHeaderPrintStream extends TeePrintStream {
    private long lastGen = Long.MIN_VALUE;
    private boolean printGen = false;

    public GenerationHeaderPrintStream(PrintStream ... streams) {
        super(streams);
    }

    public synchronized void printGenerationHeader() {
        if (!this.printGen) return;
        long gen = NeuralNet.getCurrentGeneration();
        if (gen == this.lastGen) return;
        super.println("\n\n-----------------------------------------------------\nGENERATION " + gen + "\n\n");
        this.lastGen = gen;
    }

    public synchronized void startGenHeaders() {
        this.printGen = true;
    }

    public synchronized void stopGenHeaders() {
        this.printGen = false;
    }

    public synchronized boolean isPrintingGenHeaders() {
        return this.printGen;
    }

    @Override
    public synchronized void flush() {
        printGenerationHeader();
        super.flush();
    }

    @Override
    public synchronized void write(int b) {
        printGenerationHeader();
        super.write(b);
    }

    @Override
    public synchronized void write(byte[] buf, int off, int len) {
        printGenerationHeader();
        super.write(buf, off, len);
    }

    @Override
    public synchronized void write(byte[] buf) throws IOException {
        printGenerationHeader();
        super.write(buf);
    }

    @Override
    public synchronized void writeBytes(byte[] buf) {
        printGenerationHeader();
        super.writeBytes(buf);
    }

    @Override
    public synchronized void print(boolean b) {
        printGenerationHeader();
        super.print(b);
    }

    @Override
    public synchronized void print(char c) {
        printGenerationHeader();
        super.print(c);
    }

    @Override
    public synchronized void print(int i) {
        printGenerationHeader();
        super.print(i);
    }

    @Override
    public synchronized void print(long l) {
        printGenerationHeader();
        super.print(l);
    }

    @Override
    public synchronized void print(float f) {
        printGenerationHeader();
        super.print(f);
    }

    @Override
    public synchronized void print(double d) {
        printGenerationHeader();
        super.print(d);
    }

    @Override
    public synchronized void print(char[] s) {
        printGenerationHeader();
        super.print(s);
    }

    @Override
    public synchronized void print(String s) {
        printGenerationHeader();
        super.print(s);
    }

    @Override
    public synchronized void print(Object obj) {
        printGenerationHeader();
        super.print(obj);
    }

    @Override
    public synchronized void println() {
        printGenerationHeader();
        super.println();
    }

    @Override
    public synchronized void println(boolean x) {
        printGenerationHeader();
        super.println(x);
    }

    @Override
    public synchronized void println(char x) {
        printGenerationHeader();
        super.println(x);
    }

    @Override
    public synchronized void println(int x) {
        printGenerationHeader();
        super.println(x);
    }

    @Override
    public synchronized void println(long x) {
        printGenerationHeader();
        super.println(x);
    }

    @Override
    public synchronized void println(float x) {
        printGenerationHeader();
        super.println(x);
    }

    @Override
    public synchronized void println(double x) {
        printGenerationHeader();
        super.println(x);
    }

    @Override
    public synchronized void println(char[] x) {
        printGenerationHeader();
        super.println(x);
    }

    @Override
    public synchronized void println(String x) {
        printGenerationHeader();
        super.println(x);
    }

    @Override
    public synchronized void println(Object x) {
        printGenerationHeader();
        super.println(x);
    }

    @Override
    public synchronized PrintStream printf(String format, Object... args) {
        printGenerationHeader();
        return super.printf(format, args);
    }

    @Override
    public synchronized PrintStream printf(Locale l, String format, Object... args) {
        printGenerationHeader();
        return super.printf(l, format, args);
    }

    @Override
    public synchronized PrintStream format(String format, Object... args) {
        printGenerationHeader();
        return super.format(format, args);
    }

    @Override
    public synchronized PrintStream format(Locale l, String format, Object... args) {
        printGenerationHeader();
        return super.format(l, format, args);
    }

    @Override
    public synchronized PrintStream append(CharSequence csq) {
        printGenerationHeader();
        return super.append(csq);
    }

    @Override
    public synchronized PrintStream append(CharSequence csq, int start, int end) {
        printGenerationHeader();
        return super.append(csq, start, end);
    }

    @Override
    public synchronized PrintStream append(char c) {
        printGenerationHeader();
        return super.append(c);
    }
}
