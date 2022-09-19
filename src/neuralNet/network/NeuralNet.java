package neuralNet.network;

import neuralNet.evolve.*;
import neuralNet.neuron.*;
import neuralNet.util.*;

import java.io.*;
import java.util.*;
import java.util.function.*;

public abstract class NeuralNet<S extends Sensable<S>,
                            N extends NeuralNet<S, N, C>,
                            C extends DecisionConsumer<S, C, ?>>
        implements DecisionProvider<S, N, C> {

    public static final long serialVersionUID = -1026481800510998039L;

    private enum Generation {
        CURRENT;
        private long generation = 0;
    }

    public static long getCurrentGeneration() { return Generation.CURRENT.generation; }
    public static long nextGeneration() { return ++Generation.CURRENT.generation; }

    public static class SavedCollection<N extends NeuralNet<?, N, ?>> {
        public final long generation = CURRENT.generation;
        public final Collection<N> nets;

        public SavedCollection(Collection<N> nets) {
            this.nets = nets;
        }
    }

    private static final Generation CURRENT = Generation.CURRENT;

    public final long generation;
    public long[] lineageLegacy;

    private transient Supplier<Lineage> makeLineage;
    private Lineage lineage = null;

    private final SerializableWeakHashSet<SignalProvider> providers;
    private final SerializableWeakHashSet<SignalConsumer> consumers;

    private SerializableWeakRef<NoOp> noOp;

    private transient Set<SignalProvider> providersView;
    private transient Set<SignalConsumer> consumersView;
    private transient S sensedObjected;
    private transient long round = 0;

    private transient Long hashCache = null;

    private Object writeReplace() throws ObjectStreamException {
        this.getLineage();
        return this;
    }

    private static Map<Long, Lineage> lineagesMap;
    private static Map<Long, long[]> legacyComparison;

    public static void clearLegacyLineagesMap() {
        lineagesMap = null;
    }

    protected Object readResolve() throws ObjectStreamException {
        this.providersView = this.providers.getView();
        this.consumersView = this.consumers.getView();
        if (this.generation > CURRENT.generation) CURRENT.generation = this.generation;

        if (this.lineageLegacy != null) {
            if (lineagesMap == null) lineagesMap = new TreeMap<>();
            this.lineage = SingleLineage.fromLegacyArray(this.lineageLegacy, this.getNeuralHash(), lineagesMap);

            for (Map.Entry<Long, long[]> entry : legacyComparison.entrySet()) {
                Lineage otherNew = lineagesMap.get(entry.getKey());

                double newMethod = this.lineage.getKinshipScore(otherNew);
                double oldMethod = NeuralHash.collisionKinship(this.getNeuralHash(), this.lineageLegacy, entry.getValue());
                if (newMethod != oldMethod) throw new IllegalStateException();
                if (newMethod != otherNew.getKinshipScore(this.lineage)) throw new IllegalStateException();
            }

            legacyComparison.put(this.getNeuralHash(), this.lineageLegacy);
            this.lineageLegacy = null;

        } else if (this.lineage == null) throw new IllegalStateException();

        return this;
    }

    /*
    protected NeuralNet(N deserializedFrom, Void v) {
        NeuralNet<S, N, C> df = deserializedFrom;
        this.lineageLegacy = df.lineageLegacy;
        this.generation = df.generation;
        this.providers = df.providers;
        this.consumers = df.consumers;
        this.providersView = this.providers.getView();
        this.consumersView = this.consumers.getView();
    }
     */

    protected NeuralNet() {
        this.generation = CURRENT.generation;
        //this.lineageLegacy = new long[0];
        this.providers = new SerializableWeakHashSet<>();
        this.consumers = new SerializableWeakHashSet<>();
        this.providersView = this.providers.getView();
        this.consumersView = this.consumers.getView();

        this.makeLineage = () -> new RootLineage(this.getNeuralHash());
    }

    protected NeuralNet(NeuralNet<?, ?, ?> cloneFrom) {
        this.generation = CURRENT.generation;
        this.providers = new SerializableWeakHashSet<>();
        this.consumers = new SerializableWeakHashSet<>();
        this.providersView = this.providers.getView();
        this.consumersView = this.consumers.getView();


        /*
        long parentHash = cloneFrom.getNeuralHash();
        long[] lineage = cloneFrom.lineageLegacy;
        if (lineage.length == 0 || lineage[0] != parentHash) {
            this.lineageLegacy = new long[lineage.length + 1];
            this.lineageLegacy[0] = parentHash;
            for (int i = 1; i < this.lineageLegacy.length; i++) {
                this.lineageLegacy[i] = lineage[i - 1];
            }

        } else {
            this.lineageLegacy = lineage;
        }
         */

        this.lineageLegacy = null;
        this.makeLineage = () -> new SingleLineage(cloneFrom.getLineage(), this.getNeuralHash());
    }

    protected NeuralNet(NeuralNet<?, ?, ?> parent1, NeuralNet<?, ?, ?> parent2) {
        this.generation = CURRENT.generation;
        this.providers = new SerializableWeakHashSet<>();
        this.consumers = new SerializableWeakHashSet<>();
        this.providersView = this.providers.getView();
        this.consumersView = this.consumers.getView();

        this.lineageLegacy = null;
        this.makeLineage = () -> new DualLineage(parent1.getLineage(), parent2.getLineage(), this.getNeuralHash());
    }

    protected NeuralNet(Map<NeuralNet<?, ?, ?>, Double> parents) {
        this.generation = CURRENT.generation;
        this.providers = new SerializableWeakHashSet<>();
        this.consumers = new SerializableWeakHashSet<>();
        this.providersView = this.providers.getView();
        this.consumersView = this.consumers.getView();

        this.lineageLegacy = null;
        this.makeLineage = () -> new MultiLineage(MultiLineage.convert(parents), this.getNeuralHash());
    }

    public Lineage getLineage() {
        if (this.lineage == null) {
            if (this.makeLineage == null) throw new IllegalStateException();
            this.lineage = this.makeLineage.get();
            this.makeLineage = null;
        }

        return this.lineage;
    }

    /**
     * Makes clones of all the neurons from the cloned NeuralNet, mapping the old neurons to the new ones,
     * and then replaces the inputs and consumers of each copied neuron using the map of old to new.
     *
     * IMPORTANT: It is CRITICAL to ensure that the NeuralNet being cloned has valid neuron sets
     * (providers and consumers) BEFORE undertaking the cloneNeurons operation.  See neuralNet.traceNeurons().
     * If there are unaccounted neurons that are reachable from accounted neurons (including decisionNodes
     * and sensorNodes), it could cause undefined behavior when the cloned net is being used, which could
     * also impact the net from which it was cloned because they will both share these unaccounted neurons.
     *
     * Note that this neuron cloning operation can't be done immediately in a NeuralNet clone constructor,
     * because the subclass' constructor may need to perform some initialization tasks before the full cloning
     * operation is ready to commence (e.g. populate sensor and decision nodes). This method should be called
     * from the subclass's constructor after it has finished those initializations.
     *
     *
     * @param cloneFrom the neural net we are cloning
     * @param providersMap any special signalProviders substitutions to use.  NOTE THAT ANY PASSED MAP WILL BE MUTATED.
     *                      If null, an empty HashMap will be constructed and used to map neurons from the old
     *                      net to cloned copies for the new one
     *
     * @param consumersMap any special signalConsumers substitutions to use.  NOTE THAT ANY PASSED MAP WILL BE MUTATED.
     *                      If null, an empty HashMap will be constructed and used to map neurons from the old
     *                      net to cloned copies for the new one
     */
    protected void cloneNeurons(NeuralNet cloneFrom,
                                Map<SignalProvider, SignalProvider> providersMap,
                                Map<SignalConsumer, SignalConsumer> consumersMap) {
        //cloneFrom.validateNeuronsSet();
        if (providersMap != null) {
            if (consumersMap != null) Util.syncMaps(providersMap, consumersMap);
            else consumersMap = Util.convertProvidersMap(providersMap);

        } else if (consumersMap != null) providersMap = Util.convertConsumersMap(consumersMap);

        Map<SignalProvider, SignalProvider> providers = this.populateSensorCloneMap(cloneFrom, providersMap);
        Map<SignalConsumer, SignalConsumer> consumers = this.populateDecisionCloneMap(cloneFrom, consumersMap);

        // clone allProviders from the old instance, and put into the providersMap
        for (SignalProvider p : (Set<SignalProvider>)cloneFrom.providers) {
            providers.computeIfAbsent(p, old -> {
                SignalProvider mine = old.clone();

                if (old instanceof SignalConsumer oldConsumer) {
                    if (mine instanceof SignalConsumer myConsumer) {
                        consumers.put(oldConsumer, myConsumer);

                    } else throw new IllegalStateException();
                } else if (mine instanceof SignalConsumer) throw new IllegalStateException();

                return mine;
            });
        }

        for (SignalConsumer c : (Set<SignalConsumer>)cloneFrom.consumers) {
            consumers.computeIfAbsent(c, old -> {
                SignalConsumer mine = old.clone();
                if (old instanceof SignalProvider || mine instanceof SignalProvider) {
                    throw new IllegalStateException();
                }
                return mine;
            });
        }

        this.providers.addAll(providers.values());
        this.consumers.addAll(consumers.values());

        for (SignalProvider provider : this.providers) {
            provider.replaceConsumers(consumers);
            if (provider instanceof LoopingNeuron loopingNeuron) {
                loopingNeuron.clearHashCache();
            }
        }

        for (SignalConsumer consumer : this.consumers) {
            consumer.replaceInputs(providers);
        }
    }

    @Override
    public abstract List<? extends SensorNode<S, N>> getSensors();

    @Override
    public abstract List<? extends DecisionNode<N, C>> getDecisionNodes();

    @Override
    public abstract N clone();

    public abstract N cloneWith(Map<SignalProvider, SignalProvider> providerSubs,
                                Map<SignalConsumer, SignalConsumer> consumerSubs);

    public Set<SignalProvider> getProviders() {
        return this.providersView;
    }

    @Override
    public Set<SignalConsumer> getConsumers() {
        return this.consumersView;
    }

    public long getRound() {
        return this.round;
    }

    public void before() {
        for (SignalProvider neuron : this.providers) {
            neuron.before();
        }
    }

    public void after() {
        for (SignalProvider neuron : this.providers) {
            neuron.after();
        }
        this.round++;
    }

    public void reset() {
        this.round = 0;
        for (SignalProvider neuron : this.providers) {
            neuron.reset();
        }
    }

    /**
     * IMPORTANT: MUST BE RUN AFTER MUTATING A NETWORK, BEFORE THE NETWORK IS USED OR CLONED --
     * Clears the set of neurons and repopulates it with all SignalProviders which are reachable from
     * the sensors and decision nodes, by tracing inputs and consumers recursively.
     *
     * @return 'this', for chaining
     *
     */
    public synchronized N traceNeuronsSet() {
        this.hashCache = null;
        this.providers.clear();
        this.consumers.clear();

        Set<SignalProvider> knownProviders = new HashSet<>(this.getSensors());
        Set<SignalConsumer> knownConsumers = new HashSet<>(this.getDecisionNodes());

        knownProviders.forEach(node -> knownConsumers.addAll(node.getConsumers()));
        knownConsumers.forEach(node -> knownProviders.addAll(node.getInputs()));

        Set<SignalProvider> providersInvoked = new HashSet<>();
        Set<SignalConsumer> consumersInvoked = new HashSet<>();

        while (runTracingLoops(knownProviders, knownConsumers, providersInvoked, consumersInvoked)) {
            knownConsumers.addAll(consumersInvoked);
            knownProviders.addAll(providersInvoked);

            providersInvoked.retainAll(this.providers);
            consumersInvoked.retainAll(this.consumers);
        }

        if (!(Objects.equals(providersInvoked, this.providers) && Objects.equals(consumersInvoked, this.consumers))) {
            throw new IllegalStateException();
        }

        return (N) this;
    }

    private boolean runTracingLoops(Set<SignalProvider> knownProviders,
                                    Set<SignalConsumer> knownConsumers,
                                    Set<SignalProvider> providersInvoked,
                                    Set<SignalConsumer> consumersInvoked) {

        boolean failed = false;

        for (SignalProvider provider : knownProviders) try {
            this.traceProvidersConsumers(provider, providersInvoked, consumersInvoked);

        } catch (StackOverflowError e) {
            failed = true;
            System.err.println("\tRecoverable error:"); // ...see explanation in cloneNeurons()
            System.err.println(e);
            // if this happens, just continue iterating on knownProviders/knownConsumers, and then return false so
            // repeat the entire process will be repeated.  As the 'known' and 'invoked' sets are populated,
            // the two tracing functions will not need to recurse as deeply the next time around
        }

        for (SignalConsumer consumer : knownConsumers) try {
            this.traceConsumersInputs(consumer, providersInvoked, consumersInvoked);

        } catch (StackOverflowError e) {
            failed = true;
            System.err.println("\tRecoverable error:"); // ...see explanation in cloneNeurons()
            System.err.println(e);
        }

        return failed;
    }

    private void traceConsumersInputs(SignalConsumer neuron,
                                      Set<SignalProvider> providersInvoked,
                                      Set<SignalConsumer> consumersInvoked)
            throws StackOverflowError {

        if (consumersInvoked.contains(neuron)) return;
        if (neuron instanceof DecisionNode decision && decision.getDecisionProvider() != this)
            throw new IllegalStateException();

        // We assume that all members of a complex neuron share the same inputs list, and therefore
        // this method only needs to be run once for the entire complex.
        // Note that this method also ensures the other method is invoked on every member (see the end)
        if (neuron instanceof ComplexNeuronMember complex) consumersInvoked.addAll(complex.getMembers());
        else consumersInvoked.add(neuron);

        for (SignalProvider provider : neuron.getInputs()) {
            if (provider instanceof SignalConsumer consumer) {
                this.traceConsumersInputs(consumer, providersInvoked, consumersInvoked);
            }

            this.traceProvidersConsumers(provider, providersInvoked, consumersInvoked);
        }

        if (neuron instanceof ComplexNeuronMember complex) {
            for (ComplexNeuronMember member : complex.getMembers()) {
                traceProvidersConsumers(member, providersInvoked, consumersInvoked);
            }
            this.consumers.addAll(complex.getMembers());

        } else {
            this.consumers.add(neuron);
        }
    }

    private void traceProvidersConsumers(SignalProvider neuron,
                                         Set<SignalProvider> providersInvoked,
                                         Set<SignalConsumer> consumersInvoked)
            throws StackOverflowError {

        if (providersInvoked.contains(neuron)) return;
        if (neuron instanceof SensorNode sensor && sensor.getDecisionProvider() != this)
            throw new IllegalStateException();


        providersInvoked.add(neuron);

        if (neuron instanceof ComplexNeuronMember complex) {
            this.traceConsumersInputs(complex, providersInvoked, consumersInvoked);
            // traceConsumersInputs (other method) ensures that each member has this method invoked on it
        }

        for (SignalConsumer consumer : neuron.getConsumers()) {
            this.traceConsumersInputs(consumer, providersInvoked, consumersInvoked);

            if (consumer instanceof SignalProvider provider) {
                this.traceProvidersConsumers(provider, providersInvoked, consumersInvoked);
            }
        }

        this.providers.add(neuron);
    }


    /**
     * Default implementation assumes the Lists of sensor nodes are identical size, and maps
     * the two lists directly to each other by index.  If the list from the cloned NeuralNet is smaller
     * than the list from this, the method will throw a NullPointerException UNLESS the providersMap was
     * pre-populated with substitutions for the additional clonedFrom sensors.
     *
     * Custom implementations can override this.
     *
     * @param clonedFrom NeuralNet which this is cloned from
     * @param usingMap The pre-existing map to populate with the sensor mappings, or null for a new HashMap
     * @return Same map passed in, modified with the old sensors (from clonedFrom) added as keys,
     *          and the new sensors (from this) as their values
     *
     * @throws NoSuchElementException if the SensorNode list from the cloned NeuralNet is smaller than
     * the list from this
     */
    private Map<SignalProvider, SignalProvider> populateSensorCloneMap(NeuralNet clonedFrom,
                                                                    Map<SignalProvider, SignalProvider> usingMap) {

        if (usingMap == null) usingMap = new HashMap<>(((NeuralNet<S, N, C>)clonedFrom).providers.size());

        for (Iterator<? extends SensorNode<S, N>>
                origIt = clonedFrom.getSensors().listIterator(),
                myIt = this.getSensors().listIterator();
                origIt.hasNext();) {

            SensorNode orig = origIt.next(),
                       mine = myIt.hasNext() ? myIt.next() : null;

            usingMap.computeIfAbsent(orig, o -> mine).addConsumers(orig.getConsumers());
        }

        return usingMap;
    }


    /**
     * Default implementation assumes the Lists of decision nodes are identical size, and maps
     * the two lists directly to each other by index.  If the list from the cloned NeuralNet is smaller
     * than the list from this, the method will throw a NullPointerException UNLESS the consumersMap was
     * pre-populated with substitutions for the additional clonedFrom decisionNodes.
     *
     * Custom implementations can override this.
     *
     * @param clonedFrom NeuralNet which this is cloned from
     * @return Map with the old decision node (from clonedFrom) as key, and the new decision node (from this) as value
     * @throws NoSuchElementException if the DecisionNode list from the cloned NeuralNet is smaller than
     * the list from this
     */
    private Map<SignalConsumer, SignalConsumer> populateDecisionCloneMap(NeuralNet clonedFrom,
                                                                     Map<SignalConsumer, SignalConsumer> usingMap) {
        if (usingMap == null) usingMap = new HashMap<>();

        for (ListIterator<? extends DecisionNode>
                origIt = clonedFrom.getDecisionNodes().listIterator(),
                myIt = this.getDecisionNodes().listIterator();
                origIt.hasNext();) {

            DecisionNode orig = origIt.next(),
                         mine = myIt.hasNext() ? myIt.next() : null;

            //if this net has fewer sensor

            usingMap.computeIfAbsent(orig, o -> mine).setInputs(orig.getInputs());
        }

        return usingMap;
    }

    public void setSensedObject(S sensedObject) {
        this.sensedObjected = sensedObject;
    }

    public S getSensedObject() {
        return this.sensedObjected;
    }

    public abstract class Sensor extends CachingProvider implements SensorNode<S, N> {
        protected Sensor() { }

        @Override
        public S getSensedObject() {
            return NeuralNet.this.getSensedObject();
        }

        @Override
        public N getDecisionProvider() {
            return (N)NeuralNet.this;
        }

        @Override
        public abstract short sense();

        @Override
        protected short calcOutput() {
            return this.sense();
        }

        @Override
        public CachingProvider clone() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        public static final long HASH_HEADER = NeuralHash.HEADERS.get(SensorNode.class);

        @Override
        public long calcNeuralHash() {
            return HASH_HEADER ^ Long.rotateLeft(this.getSensorId(), 29);
        }
    }

    public abstract class Decision implements DecisionNode<N, C> {
        private List<SignalProvider> inputs = new ArrayList<>(1);
        private List<SignalProvider> inputsView = Collections.unmodifiableList(inputs);

        protected Decision() { }

        @Override
        public int getMinInputs() {
            return 1;
        }

        @Override
        public int getMaxInputs() {
            return 1;
        }

        @Override
        public N getDecisionProvider() {
            return (N)NeuralNet.this;
        }

        @Override
        public abstract int getDecisionId();

        @Override
        public List<SignalProvider> getInputs() {
            return this.inputsView;
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
        public void setInputs(List<SignalProvider> inputs) {
            switch (inputs.size()) {
                case 0: this.inputs.clear(); return;
                case 1: break;
                default: throw new IllegalArgumentException();
            }

            SignalProvider newProvider = inputs.get(0);
            if (newProvider == null) throw new NullPointerException();

            if (this.inputs.size() == 0) this.inputs.add(newProvider);
            else this.inputs.set(0, newProvider);
        }

        @Override
        public void addInput(SignalProvider newProvider) {
            if (this.inputs.size() != 0) throw new IndexOutOfBoundsException();
            if (newProvider == null) throw new NullPointerException();
            this.inputs.add(newProvider);
        }

        @Override
        public void addInput(int index, SignalProvider newProvider) {
            this.inputs.add(index, newProvider);
        }

        @Override
        public boolean replaceInput(SignalProvider oldProvider, SignalProvider newProvider) {
            if (this.inputs.size() == 0 || inputs.get(0) != oldProvider) return false;
            if (newProvider == null) throw new NullPointerException();
            inputs.set(0, newProvider);
            return true;
        }

        @Override
        public SignalProvider replaceInput(int index, SignalProvider newProvider) {
            if (index != 0) throw new IndexOutOfBoundsException();
            if (newProvider == null) throw new NullPointerException();

            if (this.inputs.size() == 0) {
                this.inputs.add(newProvider);
                return null;
            }

            return this.inputs.set(0, newProvider);
        }

        @Override
        public void replaceInputs(Map<SignalProvider, SignalProvider> replacements) throws IllegalStateException {
            if (this.inputs.size() == 0) return;
            SignalProvider orig = this.inputs.get(0);
            SignalProvider replacement = replacements.get(orig);

            if (replacement == null) throw new IllegalStateException();

            replacements.put(orig, replacement);
            this.inputs.set(0, replacement);
        }

        @Override
        public SignalProvider removeInput(int index) {
            return this.inputs.remove(index);
        }

        @Override
        public boolean removeInput(SignalProvider removeAll) {
            return this.inputs.remove(removeAll);
        }

        @Override
        public void clearInputs() {
            this.inputs.clear();
        }

        @Override
        public SignalConsumer clone() {
            throw new UnsupportedOperationException();
        }
    }

    protected NoOp getNoOp() {
        NoOp noOp;
        if (this.noOp != null) noOp = this.noOp.get();
        else noOp = null;

        if (noOp == null) {
            noOp = new NoOp();
            this.noOp = new SerializableWeakRef<>(noOp);
        }

        return noOp;
    }

    protected class NoOp extends Decision {
        private NoOp() {
            this.replaceInput(0, FixedValueProvider.makeMin());
        }

        @Override
        public int getDecisionId() {
            return -1;
        }

        @Override
        public SignalConsumer clone() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public synchronized long getNeuralHash() {
        if (this.hashCache != null) return hashCache;

        int i = 0;
        long hash = 0;
        for (DecisionNode<N, C> node : this.getDecisionNodes()) {
            hash ^= Long.rotateRight(node.getNeuralHash(), i);
            i += 19;
        }
        return this.hashCache = hash;
    }

    public String toString() {
        return this.getClass().getSimpleName()
                + "(generation: " + this.generation
                + ", providers: " + this.getProviders().size()
                + ", consumers: " + this.getConsumers().size()
                + ", hash: " + NeuralHash.toHex(this.getNeuralHash())
                + ", lineage(" + this.lineageLegacy.length + "): " + NeuralHash.toHex(this.lineageLegacy) + "  )";
    }
}