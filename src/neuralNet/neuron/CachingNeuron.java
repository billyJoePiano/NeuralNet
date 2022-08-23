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

        this.populateConsumers(this.inputs, oldInputs.view);

        if (this.checkForCircularReferences()) {
            //restore previous state, then throw exception
            this.inputs = oldInputs.inputs;
            this.inputsView = oldInputs.view;
            this.populateConsumers(oldInputs.view, this.inputs);
            throw new IllegalArgumentException("Illegal circular neural loop!");
        }
    }

    @Override
    public SignalProvider replaceInput(int index, SignalProvider newProvider)
            throws NullPointerException {

        if (newProvider == null) throw new NullPointerException();

        SignalProvider old = this.inputs.set(index, newProvider);

        if (this.traceConsumers().contains(newProvider)) {
            this.inputs.set(index, old);
            throw new IllegalArgumentException("Illegal circular neural loop!");
        }
        return old;
    }

    @Override
    public boolean replaceInput(SignalProvider oldProvider, SignalProvider newProvider)
            throws IllegalArgumentException, NullPointerException {

        if (oldProvider == null || newProvider == null) throw new NullPointerException();

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
            if (this.traceConsumers().contains(newProvider)) {
                //undo everything... then throw exception
                for (int i = 0; i < replaced.length; i++) {
                    if (replaced[i]) {
                        this.inputs.set(i, oldProvider);
                    }
                }

                throw new IllegalArgumentException("Illegal circular neural loop!");
            }
        }

        return true;
    }

    @Override
    public void replaceInputs(Map<SignalProvider, SignalProvider> neuronMap) {
        for (ListIterator<SignalProvider> iterator = this.inputs.listIterator();
             iterator.hasNext();) {

            SignalProvider orig = iterator.next();
            SignalProvider replacement = neuronMap.get(orig);
            if (replacement == null) {
                replacement = orig.clone();
                if (replacement == null) throw new IllegalStateException();
                neuronMap.put(orig, replacement);
            }
            iterator.set(replacement);
        }
    }
}
