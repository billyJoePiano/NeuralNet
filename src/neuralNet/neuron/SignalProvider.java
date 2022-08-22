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
     * When implementing, return true if the consumer was already in the set of consumers, false if it was not.
     * More formally, return true if the set of consumers was mutated as a result of this call.
     */
    public boolean removeConsumer(SignalConsumer consumer);

    /**
     * When implementing, return true if the oldConsumer was already in the set of consumers, false if it was not.
     * More formally, return true if the set of consumers was mutated as a result of this call.
     */
    public boolean replaceConsumer(SignalConsumer oldConsumer, SignalConsumer newConsumer);

    public void replaceConsumers(Map<SignalConsumer, SignalConsumer> neuronMap);

    //public void clearConsumers();


    /**
     * Invoked by the neural net at the start of each round, before any outputs are requested,
     * and AFTER any action resulting from the decision of the PREVIOUS round is already carried out.
     *
     * NOTE: implementations should take care to NOT invoke neuralNet.neuron.getOutput() of other neurons
     * at this phase, because it is assumed this is where any cached outputs would either be outdated,
     * or in the process of being cleaned up
     */
    default public void before() { }

    /**
     * Invoked by the neural net at the end of each round, after all needed outputs have already
     * been requested and any decision signals have been read by the DecisionConsumer, but BEFORE
     * any decisions are actually carried out.
     */
    default public void after() { }

    /**
     * Invoked by the neural net when restarting the entire net from round zero.  e.g. Instructs
     * memory neurons to wipe their memories, wave neurons to reset their current-phase-position,
     * caching neurons to clear their cache, ETC
     */
    public void reset();

    public SignalProvider clone();
}