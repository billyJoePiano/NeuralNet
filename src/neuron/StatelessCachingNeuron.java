package neuron;

import java.util.*;

public class StatelessCachingNeuron extends CachingNeuron {
    public final StatelessFunction outputFunction;

    public StatelessCachingNeuron(StatelessCachingNeuron cloneFrom) {
        super(cloneFrom);
        this.outputFunction = cloneFrom.outputFunction;
    }

    protected StatelessCachingNeuron(StatelessCachingNeuron cloneFrom, StatelessFunction outputFunction) {
        super(cloneFrom);
        this.outputFunction = outputFunction;
    }

    public StatelessCachingNeuron(StatelessFunction outputFunction)
            throws NullPointerException {

        super();
        if (outputFunction == null) throw new NullPointerException();
        this.outputFunction = outputFunction;
    }

    public StatelessCachingNeuron(List<SignalProvider> inputs, StatelessFunction outputFunction)
            throws NullPointerException {

        super();
        if (outputFunction == null) throw new NullPointerException();
        this.outputFunction = outputFunction;

        //Needs to be done AFTER this.outputFunction is set, so that getMinInputs and getMaxInputs don't throw null pointer
        this.setInputs(inputs);
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
    protected short calcOutput(List<SignalProvider> inputs) {
        return this.outputFunction.calcOutput(inputs);
    }

    @Override
    public StatelessCachingNeuron clone() {
        return new StatelessCachingNeuron(this);
    }

    public String toString() {
         return /*this.getClass().getSimpleName() + "(" + */
                 this.outputFunction.getClass().getSimpleName() + " : " + this.outputFunction /*+ ")"*/;
    }
}
