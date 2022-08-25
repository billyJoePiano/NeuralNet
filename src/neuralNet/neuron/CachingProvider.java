package neuralNet.neuron;

import java.io.*;
import java.util.*;

public abstract class CachingProvider implements SignalProvider {
    private transient Set<SignalConsumer> consumers = Collections.newSetFromMap(new WeakHashMap<>());
    private transient Set<SignalConsumer> consumersView = Collections.unmodifiableSet(this.consumers);

    private transient Short output;

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.writeObject(new ArrayList<>(consumers));
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        this.consumers = Collections.newSetFromMap(new WeakHashMap<>());
        this.consumers.addAll((List<SignalConsumer>)stream.readObject());
        this.consumersView = Collections.unmodifiableSet(this.consumers);
    }

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
        if (cloneConsumers) {
            this.consumers.addAll(cloneFrom.consumers);
        }
    }

    protected abstract short calcOutput();

    public abstract CachingProvider clone();

    @Override
    public void before() {
        this.output = null;
    }

    @Override
    public void reset() {
        this.output = null;
    }

    protected final Short getCache() { return this.output; }
    protected final void setCache(short value) {
        this.output = value;
    }

    @Override
    public short getOutput() {
        if (this.output != null) return this.output;
        return this.output = this.calcOutput();
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
        Set<SignalConsumer> newConsumers = new HashSet<>(this.consumers.size());

        for (SignalConsumer orig : this.consumers) {
            SignalConsumer replacement = neuronMap.get(orig);
            if (replacement == null) {
                System.err.println("UNEXPECTED: missing SignalConsumer replacement: " + orig);
                //throw new IllegalStateException();
            }
            newConsumers.add(replacement);
        }

        this.consumers.clear();
        this.consumers.addAll(newConsumers);
    }
}
