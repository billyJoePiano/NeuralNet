package neuralNet.neuron;

import neuralNet.function.*;

import java.util.*;

public class CachingNeuronUsingFunction extends CachingNeuron {
    public final FunctionWithInputs outputFunction;

    public CachingNeuronUsingFunction(CachingNeuronUsingFunction cloneFrom) {
        super(cloneFrom);
        this.outputFunction = cloneFrom.outputFunction;
    }

    protected CachingNeuronUsingFunction(CachingNeuronUsingFunction cloneFrom, FunctionWithInputs outputFunction) {
        super(cloneFrom);
        this.outputFunction = outputFunction;
    }

    public CachingNeuronUsingFunction(FunctionWithInputs outputFunction)
            throws NullPointerException {

        super();
        if (outputFunction == null) throw new NullPointerException();
        this.outputFunction = outputFunction;
    }

    public CachingNeuronUsingFunction(FunctionWithInputs outputFunction, List<SignalProvider> inputs)
            throws NullPointerException {

        super();
        if (outputFunction == null) throw new NullPointerException();
        this.outputFunction = outputFunction;

        //Needs to be done AFTER this.outputFunction is set, so that getMinInputs and getMaxInputs don't throw null pointer
        this.setInputs(inputs);
    }

    public CachingNeuronUsingFunction(FunctionWithInputs outputFunction, SignalProvider ... inputs) {
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
    protected short calcOutput(List<SignalProvider> inputs) {
        return this.outputFunction.calcOutput(inputs);
    }

    @Override
    public CachingNeuronUsingFunction clone() {
        return new CachingNeuronUsingFunction(this);
    }

    public String toString() {
         return /*this.getClass().getSimpleName() + "(" + */
                 this.outputFunction.getClass().getSimpleName() + " : " + this.outputFunction /*+ ")"*/;
    }
}
