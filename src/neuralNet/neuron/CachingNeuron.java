package neuralNet.neuron;

import neuralNet.util.*;
import java.util.*;

public abstract class CachingNeuron extends CachingProvider implements Neuron {
    private List<SignalProvider> inputs;
    private List<SignalProvider> inputsView;

    /**
     * SHOULD ONLY BE INVOKED BY IMPLEMENTATIONS WHICH HAVE getMinInputs() == 0 !!!!
     */
    protected CachingNeuron() throws IllegalStateException {
        //if (this.getMinInputs() != 0) throw new IllegalStateException();
    }

    protected CachingNeuron(CachingNeuron cloneFrom) {
        this(cloneFrom, false);
    }

    protected CachingNeuron(CachingNeuron cloneFrom, boolean cloneConsumers) {
        super(cloneFrom, cloneConsumers);

        if (cloneFrom.inputs != null) {
            this.inputs = new ArrayList<>(cloneFrom.inputs);
            this.inputsView = Collections.unmodifiableList(this.inputs);
        }
    }

    protected CachingNeuron(List<SignalProvider> inputs) {
        this.setInputs(inputs);
    }

    protected abstract short calcOutput(List<SignalProvider> inputs);

    @Override
    public abstract CachingNeuron clone();

    @Override
    public List<SignalProvider> getInputs() {
        return this.inputsView;
    }

    @Override
    protected final short calcOutput() {
        return this.calcOutput(this.inputsView);
    }

    @Override
    public int inputsSize() {
        if (this.inputs == null) return 0;
        else return this.inputs.size();
    }

    @Override
    public void setInputs(List<SignalProvider> inputs) throws IllegalArgumentException {
        ListWithView<SignalProvider> oldInputs = new ListWithView<>(this.inputs, this.inputsView);
        ListWithView<SignalProvider> newInputs = this.validateInputs(inputs);

        if (newInputs != null) {
            this.inputs = newInputs.inputs;
            this.inputsView = newInputs.view;

        } else {
            this.inputs = null;
            this.inputsView = null;
        }

        this.populateConsumers(this.inputsView, oldInputs.view);

        if (this.checkForCircularReferences()) {
            //restore previous state, then throw exception
            this.inputs = oldInputs.inputs;
            this.inputsView = oldInputs.view;
            this.populateConsumers(oldInputs.view, this.inputs);
            throw new IllegalArgumentException("Illegal circular neural loop!");
        }
    }

    @Override
    public void addInput(SignalProvider newProvider) {
        if (this.inputs == null) this.inputsView = Collections.unmodifiableList(this.inputs = new ArrayList<>());

        this.inputs.add(newProvider);
        if (newProvider.addConsumer(this) && newProvider instanceof SignalConsumer) {
            if (this.traceConsumers().contains(newProvider)) {
                this.inputs.remove(this.inputs.size() - 1);
                if (!this.inputs.contains(newProvider)) newProvider.removeConsumer(this);
                throw new IllegalArgumentException("Illegal circular neural loop!");
            }
        }
    }

    @Override
    public SignalProvider removeInput(int index) {
        if (this.inputs == null) throw new IndexOutOfBoundsException(index);
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

        if (newProvider.addConsumer(this) && newProvider instanceof SignalConsumer) {
            //TODO PROBLEM WITH VariableWaveNeuron's "split" status between the two inputs, circular loop possibility on the second but not the first
            // ...possibly make custom implementation of this function for VariableWaveNeuron???

            //only do this check if this is actually a new consumer for the provider,
            // and there is a possibility of an infinite loop

            if (this.traceConsumers().contains(newProvider)) {
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

        if (newProvider.addConsumer(this) && newProvider instanceof SignalConsumer) {
            //only do this check if this is actually a new consumer for the provider,
            // and there is a possibility of an infinite loop

            if (this.traceConsumers().contains(newProvider)) {
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
}
