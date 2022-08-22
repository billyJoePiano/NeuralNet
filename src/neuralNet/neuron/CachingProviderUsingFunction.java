package neuralNet.neuron;

import neuralNet.function.*;

import java.util.*;

public class CachingProviderUsingFunction extends CachingProvider {
    public final FunctionNoInputs outputFunction;

    public CachingProviderUsingFunction(CachingProviderUsingFunction cloneFrom) {
        super(cloneFrom);
        this.outputFunction = cloneFrom.outputFunction;
    }

    protected CachingProviderUsingFunction(CachingProviderUsingFunction cloneFrom, FunctionNoInputs outputFunction)
            throws NullPointerException {

        super(cloneFrom);
        if (outputFunction == null) throw new NullPointerException();
        this.outputFunction = outputFunction;
    }

    public CachingProviderUsingFunction(FunctionNoInputs outputFunction)
            throws NullPointerException {

        super();
        if (outputFunction == null) throw new NullPointerException();
        this.outputFunction = outputFunction;
    }



    @Override
    protected short calcOutput(List<SignalProvider> inputs) {
        return this.outputFunction.calcOutput();
    }

    @Override
    public CachingProviderUsingFunction clone() {
        return new CachingProviderUsingFunction(this);
    }

    public String toString() {
         return /*this.getClass().getSimpleName() + "(" + */
                 this.outputFunction.getClass().getSimpleName() + " : " + this.outputFunction /*+ ")"*/;
    }
}
