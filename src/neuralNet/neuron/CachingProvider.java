package neuralNet.neuron;

import neuralNet.util.*;

import java.io.*;
import java.util.*;

public abstract class CachingProvider implements SignalProvider {
    private final Set<SignalConsumer> consumers = new SerializableWeakHashSet<>();

    private transient Set<SignalConsumer> consumersView
            = ((SerializableWeakHashSet<SignalConsumer>)this.consumers).getView();

    private transient Short output;

    protected Object readResolve() throws ObjectStreamException {
        this.consumersView = ((SerializableWeakHashSet<SignalConsumer>)this.consumers).getView();
        return this;
    }

    protected CachingProvider() { }

    protected CachingProvider(CachingProvider cloneFrom) {
        this.consumers.addAll(cloneFrom.consumers);
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
    public boolean addConsumers(Collection<? extends SignalConsumer> consumers) {
        return this.consumers.addAll(consumers);
    }

    @Override
    public boolean removeConsumer(SignalConsumer consumer) {
        return this.consumers.remove(consumer);
    }

    /*
    @Override
    public boolean replaceConsumer(SignalConsumer oldConsumer, SignalConsumer newConsumer) {
        if (this.consumers.remove(oldConsumer)) {
            this.consumers.add(newConsumer);
            return true;

        } else return false;
    }

    @Override
    public void clearConsumers() {
        this.consumers.clear();
    }
    */

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
