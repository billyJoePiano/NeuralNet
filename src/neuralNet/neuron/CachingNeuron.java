package neuralNet.neuron;

import java.io.*;
import java.util.*;

public abstract class CachingNeuron extends CachingProvider implements Neuron {
    protected final ArrayList<SignalProvider> inputs;
    private transient List<SignalProvider> inputsView;

    @Override
    protected Object readResolve() throws ObjectStreamException {
        this.inputsView = Collections.unmodifiableList(this.inputs);
        return super.readResolve();
    }

    protected CachingNeuron() {
        this.inputs = new ArrayList<>();
        this.inputsView = Collections.unmodifiableList(this.inputs);
    }

    protected CachingNeuron(CachingNeuron cloneFrom) {
        super(cloneFrom);
        this.inputs = new ArrayList<>();
        this.inputsView = Collections.unmodifiableList(this.inputs);

        this.inputs.addAll(cloneFrom.inputs);
    }

    protected CachingNeuron(List<SignalProvider> inputs) {
        this.inputs = new ArrayList<>();
        this.inputsView = Collections.unmodifiableList(this.inputs);
        this.setInputs(inputs);
    }

    protected CachingNeuron(ArrayList<SignalProvider> inputs, List<SignalProvider> inputsView) {
        this.inputs = inputs;
        this.inputsView = inputsView;
    }

    protected CachingNeuron(CachingNeuron cloneConsumersFrom, ArrayList<SignalProvider> inputs, List<SignalProvider> inputsView) {
        super(cloneConsumersFrom);
        this.inputs = inputs;
        this.inputsView = inputsView;
    }

    protected abstract short calcOutput(List<SignalProvider> inputs);

    @Override
    public abstract CachingNeuron clone();

    @Override
    public List<SignalProvider> getInputs() {
        return this.inputsView;
    }

    @Override
    protected short calcOutput() {
        return this.calcOutput(this.inputsView);
    }

    @Override
    public int inputsSize() {
        return this.inputs.size();
    }

    @Override
    public boolean containsInput(SignalProvider provider) {
        return this.inputs.contains(provider);
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

        List<SignalProvider> oldInputs = new ArrayList<>(this.inputs);
        this.inputs.clear();
        this.inputs.ensureCapacity(inputs.size());
        this.inputs.addAll(inputs);

        this.populateConsumers(this.inputsView, oldInputs);

        if (this.checkForCircularReferences()) {
            //restore previous state, then throw exception
            this.inputs.clear();
            this.inputs.addAll(oldInputs);

            this.populateConsumers(oldInputs, this.inputs);

            throw new IllegalArgumentException("Illegal circular neural loop!");
        }
    }

    @Override
    public void addInput(SignalProvider newProvider) {
        this.inputs.add(newProvider);
        if (newProvider.addConsumer(this) && newProvider instanceof SignalConsumer newConsumer) {
            if (this.traceConsumers(newConsumer)) {
                this.inputs.remove(this.inputs.size() - 1);
                if (!this.inputs.contains(newProvider)) newProvider.removeConsumer(this);
                throw new IllegalArgumentException("Illegal circular neural loop!");
            }
        }
    }

    @Override
    public void addInput(int index, SignalProvider newProvider) {
        this.inputs.add(index, newProvider);
        if (newProvider.addConsumer(this) && newProvider instanceof SignalConsumer newConsumer) {
            if (this.traceConsumers(newConsumer)) {
                this.inputs.remove(index);
                if (!this.inputs.contains(newProvider)) newProvider.removeConsumer(this);
                throw new IllegalArgumentException("Illegal circular neural loop!");
            }
        }
    }

    @Override
    public SignalProvider removeInput(int index) {
        SignalProvider old = this.inputs.remove(index);
        if (!this.inputs.contains(old)) old.removeConsumer(this);
        return old;
    }

    @Override
    public boolean removeInput(SignalProvider removeAll) {
        if (this.inputs == null) return false;
        boolean removed = false;
        for (Iterator<SignalProvider> iterator = this.inputs.iterator();
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
        if (this.inputs == null) throw new IndexOutOfBoundsException(index);

        SignalProvider old = this.inputs.set(index, newProvider);
        if (!this.inputs.contains(old)) old.removeConsumer(this);

        if (newProvider.addConsumer(this) && newProvider instanceof SignalConsumer consumer) {
            //only do this check if this is actually a new consumer for the provider,
            // and there is a possibility of an infinite loop

            if (this.traceConsumers(consumer)) {
                this.inputs.set(index, old);
                old.addConsumer(this);
                if (!this.inputs.contains(newProvider)) newProvider.removeConsumer(this);
                throw new IllegalArgumentException("Illegal circular neural loop!");
            }
        }
        return old;
    }

    @Override
    public boolean replaceInput(SignalProvider oldProvider, SignalProvider newProvider)
            throws IllegalArgumentException, NullPointerException {

        if (oldProvider == null || newProvider == null) throw new NullPointerException();
        if (this.inputs == null) return false;

        boolean[] replaced = new boolean[this.inputs.size()];
        boolean found = false;

        for (int i = 0; i < this.inputs.size(); i++) {
            if (this.inputs.get(i) == oldProvider) {
                replaced[i] = true;
                found = true;
                this.inputs.set(i, newProvider);
            }
        }

        if (!found) {
            return false;
        }

        if (!this.inputs.contains(oldProvider)) {
            oldProvider.removeConsumer(this);
        }

        if (newProvider.addConsumer(this) && newProvider instanceof SignalConsumer newConsumer) {
            //only do this check if this is actually a new consumer for the provider,
            // and there is a possibility of an infinite loop

            if (this.traceConsumers(newConsumer)) {
                //undo everything... then throw exception
                for (int i = 0; i < replaced.length; i++) {
                    if (replaced[i]) {
                        this.inputs.set(i, oldProvider);
                    }
                }
                oldProvider.addConsumer(this);
                if (!this.inputs.contains(newProvider)) newProvider.removeConsumer(this);

                throw new IllegalArgumentException("Illegal circular neural loop!");
            }
        }

        return true;
    }

    @Override
    public void replaceInputs(Map<SignalProvider, SignalProvider> neuronMap) throws NoSuchElementException {
        if (this.inputs == null) return;
        for (ListIterator<SignalProvider> iterator = this.inputs.listIterator();
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
        for (SignalProvider provider : this.inputs) {
            provider.removeConsumer(this);
        }
        this.inputs.clear();
    }

    /*public abstract long getProviderHash();
    public long getNeuralHash() {

        //long hash = this.getProviderHash();
        return 0;
    }

 */
}
