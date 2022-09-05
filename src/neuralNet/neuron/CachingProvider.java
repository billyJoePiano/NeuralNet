package neuralNet.neuron;

import neuralNet.util.*;

import java.util.*;

public abstract class CachingProvider implements SignalProvider {
    private final SerializableWeakHashSet<SignalConsumer> consumersMutable;
    public final transient Set<SignalConsumer> consumers;

    private transient Short output;

    protected CachingProvider(CachingProvider deserializedFrom, Void v) {
        this.consumersMutable = deserializedFrom.consumersMutable;
        this.consumers = deserializedFrom.consumers;
    }

    protected CachingProvider() {
        this.consumersMutable = new SerializableWeakHashSet<>();
        this.consumers = this.consumersMutable.getView();
    }

    protected CachingProvider(CachingProvider cloneFrom) {
        this.consumersMutable = new SerializableWeakHashSet<>();
        this.consumers = this.consumersMutable.getView();

        this.consumersMutable.addAll(cloneFrom.consumersMutable);
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
        return this.consumers;
    }

    @Override
    public boolean addConsumer(SignalConsumer consumer) {
        return this.consumersMutable.add(consumer);
    }

    @Override
    public boolean addConsumers(Collection<? extends SignalConsumer> consumers) {
        return this.consumersMutable.addAll(consumers);
    }

    @Override
    public boolean removeConsumer(SignalConsumer consumer) {
        return this.consumersMutable.remove(consumer);
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
        Set<SignalConsumer> newConsumers = new HashSet<>(this.consumersMutable.size());

        for (SignalConsumer orig : this.consumersMutable) {
            SignalConsumer replacement = neuronMap.get(orig);
            if (replacement == null) {
                System.err.println("UNEXPECTED: missing SignalConsumer replacement: " + orig);
                //throw new IllegalStateException();
            }
            newConsumers.add(replacement);
        }

        this.consumersMutable.clear();
        this.consumersMutable.addAll(newConsumers);
    }

    /*
    @Override
    public long getNeuralHash() {
        return 0;
    }
     */
}
