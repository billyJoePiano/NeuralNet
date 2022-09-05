package neuralNet.evolve;

import neuralNet.network.*;
import neuralNet.neuron.*;
import neuralNet.util.*;

import java.util.*;
import java.util.concurrent.*;

import static neuralNet.evolve.NeuronSelector.*;

public class AddNeurons<N extends NeuralNet<?, N, ?>> implements Mutator<N> {

    public static final int MAX_NEW_INPUTS = 10;
    public static final int MAX_NEW_CONSUMERS = 5;
    public static final int MAX_NEW_NEURONS = 8;

    private N net;
    private Set<SignalConsumer> needsRepopulation;
    private List<ComplexNeuronMember> complexNeurons;

    //private final Map<Set<SignalProvider>, N> clonedNets;

    public AddNeurons() { }

    public AddNeurons(N net) {
        this.net = net;
    }

    @Override
    public N getDecisionProvider() {
        return this.net;
    }

    @Override
    public void setDecisionProvider(N decisionProvider) {
        if (this.net == null) this.net = decisionProvider;
        else if (this.net != decisionProvider) throw new UnsupportedOperationException();
    }

    public List<N> mutate(int count) {
        if (count < 1) throw new IllegalArgumentException();

        this.complexNeurons = new LinkedList<>();
        for (SignalProvider neuron : this.net.getProviders()) {
            if (neuron instanceof ComplexNeuronMember complex && complex.getPrimaryNeuron() == complex) {
                this.complexNeurons.add(complex);
            }
        }

        List<N> list = new ArrayList<>(count);
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        while(count-- > 0) {
            list.add(makeMutation(rand.nextInt(MAX_NEW_NEURONS) + 1));
        }
        return list;
    }

    public N makeMutation(int count) {
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        N clone = this.net.clone();
        List<SignalProvider> allProviders = new ArrayList<>(clone.getProviders().size() + count);
        List<SignalConsumer> allConsumers = new ArrayList<>(clone.getDecisionNodes().size() + allProviders.size() + count);

        // add existing neurons to the lists
        allProviders.addAll(clone.getProviders());
        allConsumers.addAll(clone.getDecisionNodes());
        for (int i = count; i < allProviders.size(); i++) {
            // skip over new neurons, by starting at 'count' index
            SignalProvider neuron = allProviders.get(i);
            if (neuron instanceof SignalConsumer consumer) allConsumers.add(consumer);
        }

        for (int i = 0; i < count; i++) {
            SignalProvider provider = makeRandom();

            if (provider instanceof SignalConsumer consumer) {
                if (!(consumer instanceof Neuron neuron)) throw new IllegalStateException();
                int min = consumer.getMinInputs();
                switch (rand.nextInt(min + Math.min(consumer.getMaxInputs() - min + 2, 4))) {
                    case 0:
                        this.placeInlineBefore(neuron, allConsumers.get(rand.nextInt(allConsumers.size())), allProviders, allConsumers);
                        break;

                    case 1:
                        this.placeInlineAfter(neuron, allProviders.get(rand.nextInt(allProviders.size())), allProviders, allConsumers);
                        break;

                    default:
                        this.populateInputs(consumer, allProviders);
                        this.populateConsumers(provider, allConsumers);
                }
                allConsumers.add(consumer);

            } else {
                this.populateConsumers(provider, allConsumers);
            }

            allProviders.add(provider);
        }

        int iterations = 0;
        while (this.needsRepopulation != null) {
            if (iterations++ > Math.max(10, allProviders.size() + allConsumers.size())) {
                throw new RuntimeException("Can't resolve neural pathways for consumers that need paired inputs or extra inputs, without creating illegal loops");
            }

            Set<SignalConsumer> temp = this.needsRepopulation;
            this.needsRepopulation = null;

            for (SignalConsumer neuron : temp) {
                this.repopulateInputs(neuron, allProviders);
            }
        }

        return clone.traceNeuronsSet();
    }

    private void placeInlineBefore(Neuron neuron, SignalConsumer downstreamConsumer,
                                               List<SignalProvider> allProviders,
                                               List<SignalConsumer> allConsumers) {

        ThreadLocalRandom rand = ThreadLocalRandom.current();

        SignalProvider displacedProvider = downstreamConsumer.replaceInput(rand.nextInt(downstreamConsumer.inputsSize()), neuron);
        if (displacedProvider == null) throw new IllegalStateException();

        // check to see if the displaced provider occurs more than once in the inputs list for the downstream downstreamConsumer
        List<SignalProvider> allInputs = downstreamConsumer.getInputs();
        for (int i = 0; i < allInputs.size(); i++) {
            if (allInputs.get(i) == displacedProvider && rand.nextInt(3) != 2) {
                // 2/3rds chance of also replacing this input
                downstreamConsumer.replaceInput(i, neuron);
            }
        }

        // 1/3 chance we may add more than the minimum required
        int numInputs = rand.nextInt(3) == 2 ? generateNumInputs(neuron, allProviders.size()) : neuron.getMinInputs();

        this.finishPopulatingInline(neuron, displacedProvider, numInputs, rand.nextInt(numInputs),
                                    allProviders, allConsumers, rand.nextBoolean());
    }

    private void placeInlineAfter(Neuron neuron, SignalProvider upstreamProvider,
                                 List<SignalProvider> allProviders,
                                 List<SignalConsumer> allConsumers) {

        ThreadLocalRandom rand = ThreadLocalRandom.current();

        // 1/3 chance we may add more than the minimum required
        int numInputs = rand.nextInt(3) == 2 ? generateNumInputs(neuron, allProviders.size()) : neuron.getMinInputs();

        int providerInputIndex;

        if (numInputs == 1) {
            neuron.addInput(upstreamProvider);
            providerInputIndex = 0;

        } else {
            neuron.setInputs(Collections.nCopies(numInputs, FixedValueProvider.makeZero()));
            providerInputIndex = rand.nextInt(numInputs);
            neuron.replaceInput(providerInputIndex, upstreamProvider);
        }


        Set<SignalConsumer> downstreamConsumers = upstreamProvider.getConsumers();
        Map<SignalConsumer, List<Integer>> consumerInputsMap = new HashMap<>(downstreamConsumers.size());
        int consumerInputsCount = 0;

        for (SignalConsumer consumer : downstreamConsumers) {
            if (consumer == neuron) continue;
            List<Integer> indexes = new ArrayList<>();
            consumerInputsMap.put(consumer, indexes);

            int i = 0;
            for (SignalProvider input : consumer.getInputs()) {
                if (input == upstreamProvider) {
                    indexes.add(i);
                    consumerInputsCount++;
                }
                i++;
            }
        }


        List<Map.Entry<SignalConsumer, List<Integer>>> consumerInputsList = new ArrayList<>(consumerInputsMap.entrySet());

        if (consumerInputsCount == 0) {
            this.finishPopulatingInline(neuron, upstreamProvider, numInputs, providerInputIndex, allProviders, null, false); //allConsumers not needed when not adding extra

            // we're going to add the extra consumers using the normal method (larger number of consumers added)
            // instead of the call to populateConsumers() from finishPopulatingInline
            this.populateConsumers(neuron, allConsumers);
            return;
        }


        int omit = (int)Math.round(Math.abs(rand.nextGaussian()) * (double)consumerInputsCount / 4.0); // use std = 1/4th instead of 1/3rd, to favor fewer omitted downstreamConsumers
        if (omit >= consumerInputsCount) omit = consumerInputsCount - 1; //make sure there is at least one consumer

        for (int i = omit; i < consumerInputsCount; i++) {
            int entryIndex = rand.nextInt(consumerInputsList.size());
            Map.Entry<SignalConsumer, List<Integer>> entry = consumerInputsList.get(entryIndex);

            SignalConsumer consumer = entry.getKey();
            List<Integer> inputIndexes = entry.getValue();

            int indexesRemaining = inputIndexes.size();

            if (indexesRemaining == 1) {
                int inputIndex = inputIndexes.get(0);
                consumer.replaceInput(inputIndex, neuron);
                consumerInputsList.remove(entryIndex);

            } else {
                int inputIndex = inputIndexes.remove(rand.nextInt(indexesRemaining));
                consumer.replaceInput(inputIndex, neuron);
            }
        }

        this.finishPopulatingInline(neuron, upstreamProvider, numInputs, providerInputIndex,
                                    allProviders, allConsumers, rand.nextBoolean());
    }

    /**
     * Common code shared between placeInlineAfter() and placeInlineBefore() was put here
     * @param neuron
     * @param provider
     * @param numInputs
     * @param providerInputIndex
     * @param allProviders
     * @param allConsumers
     * @param addExtraConsumers
     */
    private void finishPopulatingInline(Neuron neuron, SignalProvider provider, int numInputs, int providerInputIndex,
                                        List<SignalProvider> allProviders, List<SignalConsumer> allConsumers,
                                        boolean addExtraConsumers) {

        List<SignalProvider> candidateInputs = this.findCandidateInputs(neuron, allProviders);
        neuron.clearInputs();
        if (neuron.pairedInputs() && (numInputs & 0b1) == 1) numInputs++;

        if (neuron.inputOrderMatters()) {
            //insert the displacedProvider into the supplied index position in the inputs list, after the list is populated
            this.populateInputs(neuron, numInputs - 1, candidateInputs);
            neuron.addInput(providerInputIndex, provider);

        } else {
            //order doesn't matter... just add it first
            neuron.addInput(provider);
            this.populateInputs(neuron, numInputs - 1, candidateInputs);
        }

        if (addExtraConsumers) {
            List<SignalConsumer> candidateConsumers = findCandidateConsumers(neuron, allConsumers);
            int extraConsumers = (int) Math.round((double) this.generateNumConsumers(candidateConsumers.size()) / 2.0);
            this.populateConsumers(neuron, extraConsumers, candidateConsumers);
        }
    }

    public int generateNumInputs(SignalConsumer consumer, int allProvidersSize) {
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        int min = consumer.getMinInputs();
        int max = Math.max(min, Util.min(consumer.getMaxInputs(), allProvidersSize, MAX_NEW_INPUTS));

        int numInputs = rand.nextInt(min, max + 1);
        if (consumer.pairedInputs() && (numInputs & 0b1) == 1) { // numInputs & 0b1 == numInputs % 2
            if (numInputs == min) numInputs++;
            else numInputs--;
        }
        return numInputs;
    }

    public void populateInputs(SignalConsumer consumer, List<SignalProvider> allProviders) {
        this.populateInputs(consumer, this.generateNumInputs(consumer, allProviders.size()), allProviders);
    }


    public void populateInputs(SignalConsumer consumer, int numInputs, List<SignalProvider> candidates) {
        //populate consumers of new SignalProviders and inputs (if it is also a consumer)
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        for (int i = 0; i < numInputs; i++) {
            SignalProvider input = candidates.get(rand.nextInt(candidates.size()));

            if (input == consumer && consumer instanceof Neuron n && n.traceProviders().contains(consumer)) {
                i--; //can't add the neuron as an input to itself

            } else if (consumer.containsInput(input) && rand.nextBoolean()) {
                i--; //50/50 chance of trying for a different input if it's a repeat

            } else {
                consumer.addInput(input);
            }
        }
    }

    public List<SignalConsumer> findCandidateConsumers(SignalProvider neuron, List<SignalConsumer> allConsumers) {
        if (neuron instanceof Neuron n) {
            Set<SignalProvider> exclude = n.traceProviders();
            if (exclude != null && exclude.size() > 0) {
                List<SignalConsumer> candidates = new ArrayList<>(allConsumers);
                candidates.removeAll(exclude);
                return candidates;
            }
        }
        return allConsumers;
    }
    
    public List<SignalProvider> findCandidateInputs(SignalConsumer neuron, List<SignalProvider> allProviders) {
        if (neuron instanceof Neuron n) {
            Set<SignalConsumer> exclude = n.traceConsumers();
            if (exclude != null && exclude.size() > 0) {
                List<SignalProvider> candidates = new ArrayList<>(allProviders);
                candidates.removeAll(exclude);
                return candidates;
            }
        }
        return allProviders;
    }

    public int generateNumConsumers(int candidatesSize) {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        return rand.nextInt(1, Math.max(Math.min(candidatesSize, MAX_NEW_INPUTS), 2));
    }

    public void populateConsumers(SignalProvider neuron, List<SignalConsumer> allConsumers) {
        List<SignalConsumer> candidates = findCandidateConsumers(neuron, allConsumers);
        this.populateConsumers(neuron, generateNumConsumers(candidates.size()), candidates);
    }

    public void populateConsumers(SignalProvider neuron, int numConsumers, List<SignalConsumer> candidates) {
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        if (candidates.size() < 1) throw new IllegalStateException();

        for (int i = 0; i < numConsumers; i++) {
            SignalConsumer candidate = candidates.get(rand.nextInt(0, candidates.size()));
            List<SignalProvider> currentInputs = candidate.getInputs();
            int max = candidate.getMaxInputs();
            if (currentInputs != null && currentInputs.size() >= max) {
                int replaceIndex = rand.nextInt(max);
                SignalProvider replace = currentInputs.get(replaceIndex);
                Set<SignalConsumer> currentConsumers = replace.getConsumers();
                if (currentConsumers == null || currentConsumers.size() > 1
                        && currentInputs.indexOf(replace) == currentInputs.lastIndexOf(replace) //checking if it occurs more than once in the list
                        && rand.nextBoolean()) {
                    // if this would leave the provider with no consumers...
                    // there is a 50% chance of replacement, otherwise we attempt to find a different one
                    i--;

                } else {
                    candidate.replaceInput(replaceIndex, neuron);
                }

            } else {
                if (candidate.pairedInputs()) {
                    if (this.needsRepopulation == null) this.needsRepopulation = new HashSet<>();
                    this.needsRepopulation.add(candidate);
                }
                candidate.addInput(neuron);
            }
        }
    }

    public void repopulateInputs(SignalConsumer neuron, List<SignalProvider> allProviders) {
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        // add paired input or additional input(s) if needed

        int size = neuron.inputsSize();

        if ((!neuron.pairedInputs() || (size & 0b1) == 0)
                && size >= neuron.getMinInputs()) {

            return;
        }

        List<SignalProvider> candidates;
        if (neuron instanceof Neuron n) {
            Set<SignalConsumer> exclude = n.traceConsumers();
            if (exclude.size() > 0) {
                candidates = new ArrayList<>(allProviders);
                candidates.removeAll(exclude);

            } else candidates = allProviders;
        } else candidates = allProviders;

        if (candidates.size() < 1) {
            if (size > neuron.getMinInputs()) {
                //remove instead of add
                neuron.removeInput(rand.nextInt(size));
                return;

            } else if (allProviders.size() < 1 || !(neuron instanceof Neuron n)) {
                throw new IllegalStateException();

            } else do {
                //else... we need to cull some of the consumers until there are candidates
                Set<SignalConsumer> set = n.getConsumers();
                if (set == null || set.size() < 1)
                    throw new RuntimeException("Can't resolve neural pathways for inputs that need pairing without creating illegal loops");
                List<SignalConsumer> cullCandidates = new ArrayList<>(set);
                SignalConsumer cull = cullCandidates.get(rand.nextInt(cullCandidates.size()));

                cull.removeInput(n);
                int cullsSize = cull.inputsSize();
                if (cullsSize < cull.getMinInputs() || (cull.pairedInputs() && (cullsSize & 0b1) != 0)) {
                    if (this.needsRepopulation == null) this.needsRepopulation = new HashSet<>();
                    this.needsRepopulation.add(cull);
                }

                Set<SignalConsumer> exclude = n.traceConsumers();
                if (exclude.size() > 0) {
                    candidates = new ArrayList<>(allProviders);
                    candidates.removeAll(exclude);

                } else candidates = allProviders;

            } while (candidates.size() < 1);
        }

        //populate the inputs using candidates
        while (true) {
            int index = rand.nextInt(candidates.size());
            neuron.addInput(candidates.get(index));

            if (neuron.inputsSize() >= neuron.getMinInputs()) break;
            else if (rand.nextBoolean()) {
                //50/50 chance of removing the candidate from the pool (on this iteration of the outermost loop)
                candidates.remove(index);
                if (candidates.size() < 1) {
                    if (this.needsRepopulation == null) this.needsRepopulation = new HashSet<>();
                    this.needsRepopulation.add(neuron);
                    break;
                }
            }
        }
    }
}
