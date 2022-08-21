package neuralNet.neuron;

import neuralNet.util.*;
import java.util.*;

public abstract class CachingNeuron implements Neuron {
    public static final double RANGE = (double)Short.MAX_VALUE - (double)Short.MIN_VALUE;
    public static final int RANGE_INT = (int)Short.MAX_VALUE - (int)Short.MIN_VALUE + 1;
    public static final double RANGE_INV = 1 / RANGE;

    public static final double NORMALIZE = RANGE + 1;
    public static final int NORMALIZE_INT = RANGE_INT + 1;
    public static final double NORMALIZE_INV = 1 / NORMALIZE;

    public static final double ZEROIZE = -((double)Short.MIN_VALUE);
    public static final int ZEROIZE_INT = -((int)Short.MIN_VALUE);


    public static final double MAX_PLUS_ONE = (double)Short.MAX_VALUE + 1;

    public static final double HALF_MAX = (double)Short.MAX_VALUE / 2;
    public static final double HALF_MIN = (double)Short.MIN_VALUE / 2;

    public static final double PI = Math.PI;
    public static final double TWO_PI = Math.PI * 2;


    private List<SignalProvider> inputs;
    private List<SignalProvider> inputsView;

    private final Set<SignalConsumer> consumers = Collections.newSetFromMap(new WeakHashMap<>());
    private final Set<SignalConsumer> consumersView = Collections.unmodifiableSet(this.consumers);


    private Short output;

    /**
     * SHOULD ONLY BE INVOKED BY IMPLEMENTATIONS WHICH HAVE getMinInputs() == 0 !!!!
     */
    protected CachingNeuron() throws IllegalStateException {
        //if (this.getMinInputs() != 0) throw new IllegalStateException();
    }

    protected CachingNeuron(CachingNeuron cloneFrom) {
        this(cloneFrom, false);
    }

    protected CachingNeuron(CachingNeuron cloneFrom, boolean cloneConsumersAndOutput) {
        if (cloneFrom.inputs != null) {
            this.inputs = new ArrayList<>(cloneFrom.inputs);
            this.inputsView = Collections.unmodifiableList(this.inputs);
        }

        if (cloneConsumersAndOutput && cloneFrom.consumers != null) {
            this.consumers.addAll(cloneFrom.consumers);
            this.output = cloneFrom.output;
        }
    }

    protected CachingNeuron(List<SignalProvider> inputs) {
        this.setInputs(inputs);
    }

    public List<SignalProvider> getInputs() {
        return this.inputsView;
    }

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
    public void replaceAllInputs(Map<SignalProvider, SignalProvider> replacements)
        throws IllegalStateException {

        for (ListIterator<SignalProvider> iterator = this.inputs.listIterator();
                iterator.hasNext();) {

            SignalProvider old = iterator.next();
            SignalProvider newProvider = replacements.get(old);
            if (newProvider == null || newProvider == old) {
                throw new IllegalStateException();
            }
            iterator.set(newProvider);
        }
    }

    protected abstract short calcOutput(List<SignalProvider> inputs);

    @Override
    public void before() {
        this.output = null;
    }

    public void reset() {
        this.output = null;
    }

    protected final void setCache(short value) {
        this.output = value;
    }

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
    public void clearConsumers() {
        this.consumers.clear();
    }

    public abstract CachingNeuron clone();

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
