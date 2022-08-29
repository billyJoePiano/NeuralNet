package neuralNet.evolve;

import neuralNet.network.*;

public class TweakTrial<N extends NeuralNet<?, N, ?>> {
/*
    public static final int MAX_NEW_INPUTS = 10;
    public static final int MAX_NEW_CONSUMERS = 5;

    private final N net;
    private final Map<SignalProvider, List<SignalProvider>> replacements = new LinkedHashMap<>();
    //private final Map<Set<SignalProvider>, N> clonedNets;

    public TweakTrial(N net) {
        this.net = net;
    }

    public List<N> addNeurons(int count) {
        if (count < 1) throw new IllegalArgumentException();

        N clone = this.net.clone();
        List<SignalProvider> allNeurons = new ArrayList<>(clone.getNeurons().size() + count);
        allNeurons.addAll(clone.getNeurons());


        List<SignalProvider> newNeurons = new ArrayList<>(count);
        List<List<Param>> paramsLists = new ArrayList<>(count);
        List<SignalConsumer> consumers = new ArrayList<>(clone.getDecisionNodes().size() + allNeurons.size() + count);
        //create new neurons

        for (int i = 0; i < count; i++) {
            SignalProvider neuron = getRandom();
            newNeurons.add(neuron);
            allNeurons.add(neuron);
            if (neuron instanceof SignalConsumer consumer) consumers.add(consumer);

            if (neuron instanceof SignalProvider.Tweakable tweakable) {
                paramsLists.add(tweakable.getTweakingParams());

            } else {
                paramsLists.add(null);
            }
        }

        //populate consumers of new SignalProviders and inputs (if it is also a consumer)
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        int newConsumers = consumers.size();
        consumers.addAll(clone.getDecisionNodes());
        for (SignalProvider neuron : allNeurons) {
            if (neuron instanceof SignalConsumer consumer) consumers.add(consumer);
        }

        //populate inputs
        for (int c = 0; c < newConsumers; c++) {
            SignalConsumer consumer = consumers.get(c);
            //SignalProvider p = (SignalProvider)consumer;
            Neuron n = consumer instanceof Neuron ? (Neuron) consumer : null;

            int min = consumer.getMinInputs();
            int max = Math.max(min, Util.min(consumer.getMaxInputs(), allNeurons.size(), MAX_NEW_INPUTS));
            int numInputs = rand.nextInt(min, max + 1);

            if (consumer.pairedInputs() && (numInputs & 0b1) == 1) { // numInputs & 0b1 == numInputs % 2
                if (numInputs == min) numInputs++;
                else numInputs--;
            }

            consumer.setInputs(Collections.nCopies(numInputs, FixedValueProvider.ZERO)); //placeholder

            for (int i = 0; i < numInputs; i++) {
                SignalProvider input = allNeurons.get(rand.nextInt(allNeurons.size()));
                if (input == consumer && n != null && n.traceProviders().contains(consumer)) {
                    i--; //can't add the neuron as an input to itself
                    continue;
                }
                consumer.replaceInput(i, input);
            }

            if (consumer instanceof SignalProvider.Tweakable tweakable && tweakable.paramsPerInput()) {
                List<Param> params = new ArrayList<>(tweakable.getTweakingParams());
                if (numInputs < consumer.getMaxInputs()) params.remove(params.size() - 1);
                paramsLists.set(newNeurons.indexOf(consumer), params);
            }
        }

        //populate consumers
        Set<SignalConsumer> needsAdditional = null; // for cases where a paired input needs to be populated
        for (SignalProvider neuron : newNeurons) {
            List<SignalConsumer> candidates;
            if (neuron instanceof Neuron n) {
                Set<SignalProvider> exclude = n.traceProviders();
                if (exclude.size() > 0) {
                    candidates = new ArrayList<>(consumers);
                    candidates.removeAll(exclude);

                } else candidates = consumers;
            } else candidates = consumers;

            if (candidates.size() < 1) throw new IllegalStateException();
            int numConsumers = rand.nextInt(1, Math.max(Math.min(candidates.size(), MAX_NEW_INPUTS), 2));

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
                        if (needsAdditional == null) needsAdditional = new HashSet<>();
                        needsAdditional.add(candidate);
                    }
                    candidate.addInput(neuron);
                }
            }
        }

        // add paired input if needed
        int iterations = 0;
        while (needsAdditional != null) {
            Set<SignalConsumer> temp = needsAdditional;
            needsAdditional = null;

            if (iterations++ > Math.max(10, allNeurons.size() * 2)) {
                throw new RuntimeException("Can't resolve neural pathways for inputs that need pairing without creating illegal loops");
            }

            for (SignalConsumer neuron : temp) {
                int size = neuron.inputsSize();

                if ((!neuron.pairedInputs() || (size & 0b1) == 0)
                        && size >= neuron.getMinInputs()) {

                    continue;
                }

                List<SignalProvider> candidates;
                if (neuron instanceof Neuron n) {
                    Set<SignalConsumer> exclude = n.traceConsumers();
                    if (exclude.size() > 0) {
                        candidates = new ArrayList<>(allNeurons);
                        candidates.removeAll(exclude);

                    } else candidates = allNeurons;
                } else candidates = allNeurons;

                if (candidates.size() < 1) {
                    if (size > neuron.getMinInputs()) {
                        //remove instead of add
                        neuron.removeInput(rand.nextInt(size));
                        continue;

                    } else if (allNeurons.size() < 1 || !(neuron instanceof Neuron n)) {
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
                            if (needsAdditional == null) needsAdditional = new HashSet<>();
                            needsAdditional.add(cull);
                        }

                        Set<SignalConsumer> exclude = n.traceConsumers();
                        if (exclude.size() > 0) {
                            candidates = new ArrayList<>(allNeurons);
                            candidates.removeAll(exclude);

                        } else candidates = allNeurons;

                    } while (candidates.size() < 1);
                }

                //populate the inputs using candidates
                while(true) {
                    int index = rand.nextInt(candidates.size()))
                    neuron.addInput(candidates.get(index));

                    if (neuron.inputsSize() >= neuron.getMinInputs()) break;
                    else if (rand.nextBoolean()) {
                        //50/50 chance of removing the candidate from the pool (on this iteration of the outermost loop)
                        candidates.remove(index);
                        if (candidates.size() < 1) {
                            if (needsAdditional == null) needsAdditional = new HashSet<>();
                            needsAdditional.add(neuron);
                            break;
                        }
                    }
                }
            }
        }


        // generate tweaks
        long totalComplexity = 1;
        List<List<SignalProvider.Tweakable>> tweaksLists = new ArrayList<>(count);
        int i = -1;

        for (List<Param> params : paramsLists) {
            i++;
            if (params == null) {
                tweaksLists.add(null);
                continue;
            }

            List<Short>[] tweaks = new List<>[params.size()];

            int neuronComplexity = 1;
            for (Param param : params) {
                List<Short> permutations = new ArrayList<>();
                tweaks[i++] = permutations;

                if (param.min < 0) {
                    permutations.add(param.min);
                    if (param.min < -1) {
                        permutations.add((short) -Math.round(-(double) param.min / 2.0));
                        if (param.min < -2) {
                            permutations.add((short) -Math.round(-(double) param.min / 4.0));

                            if (param.min < -5) permutations.add((short) -Math.round(-(double) param.min / 8.0));
                            else if (param.min == -5) permutations.add(2, (short)-2);
                            else if (param.min == -4) permutations.add(1, (short)-3);
                        }
                    }
                }

                if (param.min <= 0 && param.max >= 0) permutations.add((short)0);
                else throw new IllegalStateException();

                if (param.max > 0) {
                    if (param.max > 1) {
                        if (param.max > 2) {
                            if (param.max > 5) permutations.add((short) Math.round((double) param.max / 8.0));

                            permutations.add((short) Math.round((double) param.max / 4.0));
                            if (param.max == 5) permutations.add((short)2);
                        }
                        permutations.add((short) Math.round((double) param.max / 2.0));
                        if (param.max == 4) permutations.add((short)3);
                    }

                    if (!(param.circular && param.max == Short.MAX_VALUE)) {
                        permutations.add(param.max);
                    }
                }

                neuronComplexity *= permutations.size();
            }
            tweaksLists.add(makeTweaks((SignalProvider.Tweakable) newNeurons.get(i), tweaks,
                                new short[tweaks.length], 0, true, new ArrayList<>(neuronComplexity)));

            totalComplexity *= neuronComplexity;
        }


    }

    private List<SignalProvider.Tweakable> makeTweaks(SignalProvider.Tweakable<? extends SignalProvider.Tweakable> origNeuron,
                                                      List<Short>[] tweaks, short[] currentParams, int index,
                                                      boolean allZeros, List<SignalProvider> neurons) {

        for (short param : tweaks[index]) {
            currentParams[index] = param;
            boolean currentAllZeros = allZeros && param == 0;
            if (index == tweaks.length - 1) {
                if (currentAllZeros) neurons.add(origNeuron);
                else neurons.add(origNeuron.tweak(currentParams));


            } else makeTweaks(origNeuron, tweaks, currentParams, index + 1, currentAllZeros, neurons);
        }
    }

*/
}
