package neuralNet.neuron;

import java.util.*;

public interface SignalConsumer {
    default public int getMinInputs() {
        return 0;
    }
    default public int getMaxInputs() {
        return 0;
    }

    default public boolean inputOrderMatters() {
        return false;
    }

    public List<SignalProvider> getInputs();
    public void setInputs(List<SignalProvider> inputs);

    public boolean replaceInput(SignalProvider oldProvider, SignalProvider newProvider);
    public SignalProvider replaceInput(int index, SignalProvider newProvider);
    public void replaceInputs(Map<SignalProvider, SignalProvider> replacements) throws IllegalStateException;
}
