package neuralNet.neuron;

import java.util.*;

public interface Neuron extends SignalProvider, SignalConsumer {
    @Override //override from SignalProvider, to make the return type Neuron
    public Neuron clone();

    default public boolean checkForCircularReferences() {
        List<? extends SignalProvider> providers = this.getInputs();
        if (providers == null || providers.size() == 0) return false;

        Set<? extends SignalConsumer> consumers = this.traceConsumers();
        if (consumers == null || consumers.size() == 0) return false;

        for (SignalProvider neuron : providers) {
            if (neuron == null) throw new NullPointerException();
            if (neuron instanceof SignalConsumer && consumers.contains(neuron)) {
                return true;
            }
        }
        return false;
    }

    default public boolean traceConsumers(SignalConsumer contains) {
        Set<SignalConsumer> consumers = this.traceConsumers();
        if (consumers == null) return false;
        else return consumers.contains(contains);
    }

    /**
     * Recursively obtains the set of SignalConsumers which this neuralNet.neuron provides outputs for,
     * and therefore which it cannot consume the signal from itself, typically including
     * itself.  The purpose of this method is to prevent infinite loops when calculating
     * outputs, to ensure that signals only move in one direction.
     *
     * Signal-consuming neurons which don't rely upon the *CURRENT* output of other neurons
     * to produce their own outputs (i.e. memory neurons) should return null or an empty set
     *
     * @return
     */
    default public Set<SignalConsumer> traceConsumers() {
        Set<SignalConsumer> consumers = new HashSet<>();
        this.traceConsumers(consumers);
        return consumers;
    }

    /**
     * Convenience method for passing a single set recursively through the neural neuralNet.network, and
     * preventing infinite recursion. NOTE that neurons should only add themselves to the set
     * (if appropriate) and then invoke this method on the other neurons in their consumers set.
     *
     * Signal-consuming neurons which don't rely upon the *CURRENT* output of other neurons
     * to produce their own current output (i.e. memory neurons) should override this default
     * implementation.  They should NOT add themselves to the set and should NOT recursively
     * invoke this method on their consumers ... they should simply return without any action
     *
     * @param addToExistingSet
     */
    default public void traceConsumers(Set<SignalConsumer> addToExistingSet) {
        if (addToExistingSet.contains(this)) return;
        addToExistingSet.add(this);
        Set<SignalConsumer> consumers = this.getConsumers();
        if (consumers == null) return;

        for (SignalConsumer neuron : consumers) {
            if (neuron instanceof Neuron) {
                ((Neuron)neuron).traceConsumers(addToExistingSet);
            }
        }
    }


    /**
     * Recursively obtains the set of SignalProviders which this neuralNet.neuron depends upon
     * and therefore cannot provide a signal to if they are consumers, typically including
     * itself.  The purpose of this method is to prevent infinite loops when calculating
     * outputs, to ensure that signals only move in one direction.
     *
     * Signal-providing neurons which don't rely upon the *CURRENT* output of other neurons (e.g.
     * memory neurons, fixed-value neurons, sensor-neurons) should return null or an empty set
     *
     * @return
     */
    default public Set<SignalProvider> traceProviders() {
        Set<SignalProvider> providers = new HashSet<>();
        this.traceProviders(providers);
        return providers;
    }

    /**
     * Convenience method for passing a single set recursively through the neural neuralNet.network, and
     * preventing infinite recursion. NOTE that neurons should only add themselves to the set
     * (if appropriate) and then invoke this method on the other neurons in their providers set.
     *
     * Signal-providing neurons which don't rely upon the *CURRENT* output of other neurons
     * to produce their own current output (i.e. memory neurons) should override this default
     * implementation.  They should NOT add themselves to the set and should NOT recursively
     * invoke this method on their providers ... they should simply return without any action
     */
    default public void traceProviders(Set<SignalProvider> addToExistingSet) {
        if (addToExistingSet.contains(this)) return;
        addToExistingSet.add(this);

        for (SignalProvider neuron : this.getInputs()) {
            if (neuron instanceof Neuron) {
                ((Neuron)neuron).traceProviders(addToExistingSet);
            }
        }
    }
}
