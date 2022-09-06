package neuralNet.neuron;

import neuralNet.function.*;

import java.util.*;

public class CachingNeuronUsingFunction extends CachingNeuron {
    public static final long serialVersionUID = -3240982009517495015L;

    public final NeuralFunction outputFunction;

    /*
    protected Object readResolve() throws ObjectStreamException {
        return new CachingNeuronUsingFunction(this, (Void)null);
    }

    protected CachingNeuronUsingFunction(CachingNeuronUsingFunction deserializedFrom, Void v) {
        super(deserializedFrom, null);
        this.outputFunction = deserializedFrom.outputFunction;
    }
     */

    public CachingNeuronUsingFunction(CachingNeuronUsingFunction cloneFrom) {
        super(cloneFrom);
        this.outputFunction = cloneFrom.outputFunction;
    }

    protected CachingNeuronUsingFunction(CachingNeuronUsingFunction cloneFrom, NeuralFunction outputFunction) {
        super(cloneFrom);
        this.outputFunction = outputFunction;
    }

    public CachingNeuronUsingFunction(NeuralFunction outputFunction)
            throws NullPointerException {

        super();
        if (outputFunction == null) throw new NullPointerException();
        this.outputFunction = outputFunction;
    }

    public CachingNeuronUsingFunction(NeuralFunction outputFunction, List<SignalProvider> inputs)
            throws NullPointerException {

        super();
        if (outputFunction == null) throw new NullPointerException();
        this.outputFunction = outputFunction;

        //Needs to be done AFTER this.outputFunction is set, so that getMinInputs and getMaxInputs don't throw null pointer
        this.setInputs(inputs);
    }

    public CachingNeuronUsingFunction(NeuralFunction outputFunction, SignalProvider ... inputs) {
        this(outputFunction, Arrays.asList(inputs));
    }

    @Override
    public int getMinInputs() {
        return this.outputFunction.getMinInputs();
    }

    @Override
    public int getMaxInputs() {
        return this.outputFunction.getMaxInputs();
    }

    @Override
    public boolean inputOrderMatters() {
        return this.outputFunction.inputOrderMatters();
    }

    @Override
    public boolean pairedInputs() {
        return this.outputFunction.pairedInputs();
    }

    @Override
    protected short calcOutput(List<SignalProvider> inputs) {
        return this.outputFunction.calcOutput(inputs);
    }

    @Override
    public long getNeuralHash() {
        long hash = this.outputFunction.getNeuralHash() ^ Long.rotateLeft((long)this.inputs.size(), 27);
        if (this.outputFunction.inputOrderMatters()) {
            int i = 0;
            for (SignalProvider provider: this.inputs) {
                hash ^= Long.rotateRight(provider.getNeuralHash(), i += 17);
            }

        } else {
            for (SignalProvider provider : this.inputs) {
                hash ^= Long.rotateRight(provider.getNeuralHash(), 17);
            }
        }
        return hash;
    }

    @Override
    public CachingNeuronUsingFunction clone() {
        return new CachingNeuronUsingFunction(this);
    }

    public String toString() {
        return this.outputFunction.getClass().getSimpleName();
    }
}
