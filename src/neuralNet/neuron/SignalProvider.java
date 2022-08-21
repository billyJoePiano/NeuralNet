package neuralNet.neuron;

import java.util.*;

public interface SignalProvider {
    public short getOutput();

    public Set<SignalConsumer> getConsumers();

    /**
     * When implementing, return true if the consumer was not already in the set of consumers, false if it was.
     * More formally, return true if the set of consumers was mutated as a result of this call.
     */
    public boolean addConsumer(SignalConsumer consumer);

    /**
     * When implementing, return true if the consumer was already in the set consumers, false if it was not.
     * More formally, return true if the set of consumers was mutated as a result of this call.
     */
    public boolean removeConsumer(SignalConsumer consumer);

    public void clearConsumers();

    public SignalProvider clone();
}
