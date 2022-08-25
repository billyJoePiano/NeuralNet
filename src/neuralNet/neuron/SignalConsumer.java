package neuralNet.neuron;

import java.io.*;
import java.util.*;

public interface SignalConsumer extends Serializable {
    default public int getMinInputs() {
        return 0;
    }
    default public int getMaxInputs() {
        return 0;
    }

    default public boolean inputOrderMatters() {
        return false;
    }
    default public boolean pairedInputs() { return false; }

    public List<SignalProvider> getInputs();
    public int inputsSize();

    public void setInputs(List<SignalProvider> inputs);
    public void addInput(SignalProvider newProvider);
    public SignalProvider replaceInput(int index, SignalProvider newProvider);
    public boolean replaceInput(SignalProvider oldProvider, SignalProvider newProvider);
    public void replaceInputs(Map<SignalProvider, SignalProvider> replacements) throws IllegalStateException;
    public SignalProvider removeInput(int index);
    public boolean removeInput(SignalProvider removeAll);
}
