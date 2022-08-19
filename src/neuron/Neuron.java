package neuron;

import java.util.*;

public interface Neuron extends SignalProvider, SignalConsumer {
    /**
     * Invoked by the neural net at the start of each round, before any outputs are requested,
     * and AFTER any action resulting from the decision of the previous round is already carried out.
     *
     * NOTE: implementations should take care to NOT invoke neuron.getOutput() of other neurons
     * at this phase, because it is assumed this is where any cached outputs would either be outdated,
     * or in the process of being cleaned up
     */
    default public void before() { }

    /**
     * Invoked by the neural net at the end of each round, after all needed outputs have already
     * been requested and any decision signals have been read by the DecisionConsumer, but BEFORE
     * any decisions are actually carried out.
     */
    default public void after() { }

    public void reset();

    @Override //override from SignalProvider, to make the return type Neuron
    public Neuron clone();


    /**
     * Convenience method to be invoked by setInputs() to validate the list.  Should be followed
     * by a call to populateConsumers() after the new list/view have been assigned to the instance variables
     *
     * Throws IllegalArgumentException if the list doesn't check out.
     * Otherwise returns copied list with an unmodifiable view, wrapped by InputsWithView
     */
    default public ProvidersWithView validateInputs(List<SignalProvider> inputs)
            throws IllegalArgumentException, NullPointerException {

        List<SignalProvider> old = this.getInputs();
        if (inputs == null) {
            if (this.getMinInputs() != 0) {
                throw new IllegalArgumentException();
            }

            return new ProvidersWithView(new ArrayList<>());
        }

        ProvidersWithView iwv = new ProvidersWithView(new ArrayList<>(inputs));
        int size = iwv.inputs.size();
        int min = this.getMinInputs();
        int max = this.getMaxInputs();
        if (size < min || size > max) {
            throw new IllegalArgumentException("Input size must be " + min + " - " + max + ".  Actual size: " + size);
        }

        return iwv;
    }

    /**
     * To be invoked after validateInputs returns, and the new inputs list/view has already been
     * assigned to the instance variable(s).
     *
     * @param newInputs
     * @param oldInputs
     */
    default public void populateConsumers(List<SignalProvider> newInputs, List<SignalProvider> oldInputs) {
        if (newInputs != null) {
            if (oldInputs != null) {
                for (SignalProvider neuron : oldInputs) {
                    neuron.removeConsumer(this);
                }
            }

            for (SignalProvider neuron : newInputs) {
                neuron.addConsumer(this);
            }

        } else if (oldInputs != null) {
            for (SignalProvider neuron : oldInputs) {
                if (!newInputs.contains(neuron)) {
                    neuron.removeConsumer(this);
                }
            }
        }
    }

    default public boolean checkForCircularReferences() {
        List<SignalProvider> providers = this.getInputs();
        if (providers == null || providers.size() == 0) return false;

        Set<SignalConsumer> consumers = this.traceConsumers();
        if (consumers == null || consumers.size() == 0) return false;

        for (SignalProvider neuron : this.getInputs()) {
            if (neuron == null) throw new NullPointerException();
            if (neuron instanceof SignalConsumer && consumers.contains(neuron)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Recursively obtains the set of SignalConsumers which this neuron provides outputs for,
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
     * Convenience method for passing a single set recursively through the neural network, and
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

        for (SignalConsumer neuron : this.getConsumers()) {
            if (neuron instanceof Neuron) {
                ((Neuron)neuron).traceConsumers(addToExistingSet);
            }
        }
    }


    /**
     * Recursively obtains the set of SignalProviders which this neuron depends upon
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
     * Convenience method for passing a single set recursively through the neural network, and
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

    class ProvidersWithView {
        public final List<SignalProvider> inputs;
        public final List<SignalProvider> view;

        public ProvidersWithView(final List<SignalProvider> inputs) {
            this.inputs = inputs;
            this.view = Collections.unmodifiableList(inputs);
        }

        public ProvidersWithView(final List<SignalProvider> inputs, final List<SignalProvider> view) {
            this.inputs = inputs;
            this.view = view;
        }
    }
}
