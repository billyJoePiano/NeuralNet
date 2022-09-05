package neuralNet.neuron;

import java.util.*;

public abstract class CachingNeuron extends CachingProvider implements Neuron {
    protected final ArrayList<SignalProvider> inputsMutable;
    public final transient List<SignalProvider> inputs;

    protected CachingNeuron(CachingNeuron deserializedFrom, Void v) {
        super(deserializedFrom, (Void)null);
        this.inputsMutable = deserializedFrom.inputsMutable;
        this.inputs = Collections.unmodifiableList(this.inputsMutable);
    }

    protected CachingNeuron() {
        super();
        this.inputsMutable = new ArrayList<>();
        this.inputs = Collections.unmodifiableList(this.inputsMutable);
    }

    protected CachingNeuron(CachingNeuron cloneFrom) {
        super(cloneFrom);
        this.inputsMutable = new ArrayList<>();
        this.inputs = Collections.unmodifiableList(this.inputsMutable);

        this.inputsMutable.addAll(cloneFrom.inputsMutable);
    }

    protected CachingNeuron(List<SignalProvider> inputs) {
        this.inputsMutable = new ArrayList<>();
        this.inputs = Collections.unmodifiableList(this.inputsMutable);
        this.setInputs(inputs);
    }

    protected CachingNeuron(ArrayList<SignalProvider> inputs, List<SignalProvider> inputsView) {
        this.inputsMutable = inputs;
        this.inputs = inputsView;
    }

    protected CachingNeuron(CachingNeuron cloneConsumersFrom, ArrayList<SignalProvider> inputsMutable, List<SignalProvider> inputsView) {
        super(cloneConsumersFrom);
        this.inputsMutable = inputsMutable;
        this.inputs = inputsView;
    }

    protected abstract short calcOutput(List<SignalProvider> inputs);

    @Override
    public abstract CachingNeuron clone();

    @Override
    public List<SignalProvider> getInputs() {
        return this.inputs;
    }

    @Override
    protected short calcOutput() {
        return this.calcOutput(this.inputs);
    }

    @Override
    public int inputsSize() {
        return this.inputsMutable.size();
    }

    @Override
    public boolean containsInput(SignalProvider provider) {
        return this.inputsMutable.contains(provider);
    }

    @Override
    public void setInputs(List<SignalProvider> inputs) throws IllegalArgumentException {
        if (inputs == null) throw new NullPointerException();

        int size = inputs.size();
        int min = this.getMinInputs();
        int max = this.getMaxInputs();
        if (size < min || size > max || (this.pairedInputs() && (size & 0b1) != 0)) {
            throw new IllegalArgumentException("Input size must be " + min + " - " + max + ".  Actual size: " + size
                    + "  Inputs paired?: " + this.pairedInputs());
        }

        List<SignalProvider> oldInputs = new ArrayList<>(this.inputsMutable);
        this.inputsMutable.clear();
        this.inputsMutable.ensureCapacity(inputs.size());
        this.inputsMutable.addAll(inputs);

        this.populateConsumers(this.inputs, oldInputs);

        if (this.checkForCircularReferences()) {
            //restore previous state, then throw exception
            this.inputsMutable.clear();
            this.inputsMutable.addAll(oldInputs);

            this.populateConsumers(oldInputs, this.inputsMutable);

            throw new IllegalArgumentException("Illegal circular neural loop!");
        }
    }

    @Override
    public void addInput(SignalProvider newProvider) {
        this.inputsMutable.add(newProvider);
        if (newProvider.addConsumer(this) && newProvider instanceof SignalConsumer newConsumer) {
            if (this.traceConsumers(newConsumer)) {
                this.inputsMutable.remove(this.inputsMutable.size() - 1);
                if (!this.inputsMutable.contains(newProvider)) newProvider.removeConsumer(this);
                throw new IllegalArgumentException("Illegal circular neural loop!");
            }
        }
    }

    @Override
    public void addInput(int index, SignalProvider newProvider) {
        this.inputsMutable.add(index, newProvider);
        if (newProvider.addConsumer(this) && newProvider instanceof SignalConsumer newConsumer) {
            if (this.traceConsumers(newConsumer)) {
                this.inputsMutable.remove(index);
                if (!this.inputsMutable.contains(newProvider)) newProvider.removeConsumer(this);
                throw new IllegalArgumentException("Illegal circular neural loop!");
            }
        }
    }

    @Override
    public SignalProvider removeInput(int index) {
        SignalProvider old = this.inputsMutable.remove(index);
        if (!this.inputsMutable.contains(old)) old.removeConsumer(this);
        return old;
    }

    @Override
    public boolean removeInput(SignalProvider removeAll) {
        if (this.inputsMutable == null) return false;
        boolean removed = false;
        for (Iterator<SignalProvider> iterator = this.inputsMutable.iterator();
             iterator.hasNext();) {

            if (iterator.next() == removeAll) {
                iterator.remove();
                removed = true;
            }
        }
        removeAll.removeConsumer(this);
        return removed;
    }

    @Override
    public SignalProvider replaceInput(int index, SignalProvider newProvider)
            throws NullPointerException {

        if (newProvider == null) throw new NullPointerException();
        if (this.inputsMutable == null) throw new IndexOutOfBoundsException(index);

        SignalProvider old = this.inputsMutable.set(index, newProvider);
        if (!this.inputsMutable.contains(old)) old.removeConsumer(this);

        if (newProvider.addConsumer(this) && newProvider instanceof SignalConsumer consumer) {
            //only do this check if this is actually a new consumer for the provider,
            // and there is a possibility of an infinite loop

            if (this.traceConsumers(consumer)) {
                this.inputsMutable.set(index, old);
                old.addConsumer(this);
                if (!this.inputsMutable.contains(newProvider)) newProvider.removeConsumer(this);
                throw new IllegalArgumentException("Illegal circular neural loop!");
            }
        }
        return old;
    }

    @Override
    public boolean replaceInput(SignalProvider oldProvider, SignalProvider newProvider)
            throws IllegalArgumentException, NullPointerException {

        if (oldProvider == null || newProvider == null) throw new NullPointerException();
        if (this.inputsMutable == null) return false;

        boolean[] replaced = new boolean[this.inputsMutable.size()];
        boolean found = false;

        for (int i = 0; i < this.inputsMutable.size(); i++) {
            if (this.inputsMutable.get(i) == oldProvider) {
                replaced[i] = true;
                found = true;
                this.inputsMutable.set(i, newProvider);
            }
        }

        if (!found) {
            return false;
        }

        if (!this.inputsMutable.contains(oldProvider)) {
            oldProvider.removeConsumer(this);
        }

        if (newProvider.addConsumer(this) && newProvider instanceof SignalConsumer newConsumer) {
            //only do this check if this is actually a new consumer for the provider,
            // and there is a possibility of an infinite loop

            if (this.traceConsumers(newConsumer)) {
                //undo everything... then throw exception
                for (int i = 0; i < replaced.length; i++) {
                    if (replaced[i]) {
                        this.inputsMutable.set(i, oldProvider);
                    }
                }
                oldProvider.addConsumer(this);
                if (!this.inputsMutable.contains(newProvider)) newProvider.removeConsumer(this);

                throw new IllegalArgumentException("Illegal circular neural loop!");
            }
        }

        return true;
    }

    @Override
    public void replaceInputs(Map<SignalProvider, SignalProvider> neuronMap) throws NoSuchElementException {
        if (this.inputsMutable == null) return;
        for (ListIterator<SignalProvider> iterator = this.inputsMutable.listIterator();
             iterator.hasNext();) {

            SignalProvider orig = iterator.next();
            SignalProvider replacement = neuronMap.get(orig);
            if (replacement == null) {
                throw new NoSuchElementException();

                /*
                replacement = orig.clone();
                if (replacement == null) throw new IllegalStateException();
                neuronMap.put(orig, replacement);
                 */
            }
            iterator.set(replacement);
        }
    }

    @Override
    public void clearInputs() {
        for (SignalProvider provider : this.inputsMutable) {
            provider.removeConsumer(this);
        }
        this.inputsMutable.clear();
    }

    /*public abstract long getProviderHash();
    public long getNeuralHash() {

        //long hash = this.getProviderHash();
        return 0;
    }

 */
}
