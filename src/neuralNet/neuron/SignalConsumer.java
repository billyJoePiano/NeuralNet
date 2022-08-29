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
    public boolean containsInput(SignalProvider provider);

    public void setInputs(List<SignalProvider> inputs);
    public void addInput(SignalProvider newProvider);
    public void addInput(int index, SignalProvider newProvider);
    public SignalProvider replaceInput(int index, SignalProvider newProvider);
    public boolean replaceInput(SignalProvider oldProvider, SignalProvider newProvider);
    public void replaceInputs(Map<SignalProvider, SignalProvider> replacements) throws IllegalStateException;
    public SignalProvider removeInput(int index);
    public boolean removeInput(SignalProvider removeAll);
    public void clearInputs();

    public SignalConsumer clone();



    /**
     * Convenience method to be invoked by setInputs(), after validateInputs returns and the new
     * inputs list/view has already been assigned to the instance variables.
     *
     * @param newInputs
     * @param oldInputs
     */
    default public void populateConsumers(List<SignalProvider> newInputs, List<SignalProvider> oldInputs) {
        if (newInputs != null) {
            if (oldInputs != null) {
                for (SignalProvider neuron : oldInputs) {
                    neuron.removeConsumer(this);
                }
            }

            for (SignalProvider neuron : newInputs) {
                neuron.addConsumer(this);
            }

        } else if (oldInputs != null) {
            for (SignalProvider neuron : oldInputs) {
                if (!newInputs.contains(neuron)) {
                    neuron.removeConsumer(this);
                }
            }
        }
    }
}
