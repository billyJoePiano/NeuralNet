package neuralNet.neuron;

import java.util.*;

public abstract class CachingProvider implements SignalProvider {
    private List<SignalProvider> inputs;
    private List<SignalProvider> inputsView;

    private final Set<SignalConsumer> consumers = Collections.newSetFromMap(new WeakHashMap<>());
    private final Set<SignalConsumer> consumersView = Collections.unmodifiableSet(this.consumers);


    private Short output;

    /**
     * SHOULD ONLY BE INVOKED BY IMPLEMENTATIONS WHICH HAVE getMinInputs() == 0 !!!!
     */
    protected CachingProvider() throws IllegalStateException {
        //if (this.getMinInputs() != 0) throw new IllegalStateException();
    }

    protected CachingProvider(CachingProvider cloneFrom) {
        this(cloneFrom, false);
    }

    protected CachingProvider(CachingProvider cloneFrom, boolean cloneConsumers) {
        if (cloneFrom.inputs != null) {
            this.inputs = new ArrayList<>(cloneFrom.inputs);
            this.inputsView = Collections.unmodifiableList(this.inputs);
        }

        if (cloneConsumers) {
            this.consumers.addAll(cloneFrom.consumers);
        }
    }

    protected abstract short calcOutput(List<SignalProvider> inputs);

    @Override
    public void before() {
        this.output = null;
    }

    @Override
    public void reset() {
        this.output = null;
    }

    protected final void setCache(short value) {
        this.output = value;
    }

    @Override
    public short getOutput() {
        if (this.output != null) return this.output;

        return this.output = this.calcOutput(this.inputsView);
    }

    @Override
    public Set<SignalConsumer> getConsumers() {
        return this.consumersView;
    }

    @Override
    public boolean addConsumer(SignalConsumer consumer) {
        return this.consumers.add(consumer);
    }

    @Override
    public boolean removeConsumer(SignalConsumer consumer) {
        return this.consumers.remove(consumer);
    }

    @Override
    public boolean replaceConsumer(SignalConsumer oldConsumer, SignalConsumer newConsumer) {
        if (this.consumers.remove(oldConsumer)) {
            this.consumers.add(newConsumer);
            return true;

        } else return false;
    }

    /* @Override
    public void clearConsumers() {
        this.consumers.clear();
    } */

    @Override
    public void replaceConsumers(Map<SignalConsumer, SignalConsumer> neuronMap) {
        Set<SignalConsumer> newConsumers = Collections.newSetFromMap(new HashMap<>(this.consumers.size()));

        for (Iterator<SignalConsumer> iterator = this.consumers.iterator();
                 iterator.hasNext();) {

            SignalConsumer orig = iterator.next();
            SignalConsumer replacement = neuronMap.get(iterator.next());
            if (replacement == null) throw new IllegalStateException();
            newConsumers.add(replacement);
        }

        this.consumers.clear();
        this.consumers.addAll(newConsumers);
    }
}
