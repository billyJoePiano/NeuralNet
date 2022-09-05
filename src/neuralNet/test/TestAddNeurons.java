package neuralNet.test;

import game2048.*;
import game2048.BoardInterface.*;
import neuralNet.evolve.*;
import neuralNet.network.*;
import neuralNet.neuron.*;
import neuralNet.util.*;

import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

public class TestAddNeurons extends Thread {
    public static final int GENERATIONS = 2048;
    public static final int NETS_PER_GENERATION = 32;
    public static final int RETAIN_BEST = 8;
    public static final int THREADS = 6;
    public static final int MIN_RAND_LINEAGE_RETAINED = 4;

    private static final BoardNet EDGE_NET = TestBoardNet.makeEdgeNet(),
            RAND_NET = TestBoardNet.makeRandomNet();

    public static final long RAND_HASH = RAND_NET.getNeuralHash();

    public static final String FILENAME = "TestAddNeurons "
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH_mm_ss"));

    private static final List<TestAddNeurons> threads = new ArrayList<>(THREADS);

    private static Set<BoardNet> currentNets = Set.of(EDGE_NET, RAND_NET);
    private static List<BoardNet> fittest;
    private static final Var<Iterator<BoardNet>> currentIterator = new Var<>();
    private static final Set<Thread> threadsIdle = new HashSet<>();
    private static boolean finished = false;
    private static List<BoardNetFitness> fitnesses;
    private static final Map<Long, BoardNet> hashes = new TreeMap<>(Map.of(EDGE_NET.getNeuralHash(), EDGE_NET, RAND_NET.getNeuralHash(), RAND_NET));
    private static final Map<Long, Set<BoardNet>> hashCollisions = new TreeMap<>();
    private static final Map<BoardNet, Long> legacy = new LinkedHashMap<>();
    private static TeePrintStream System_all;

    private static final Thread SHUTDOWN_HOOK = new Thread(() -> {
        String filename = FILENAME + " (gen " + NeuralNet.getCurrentGeneration() + ").nnt";

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            if (!finished) waitForThreadsToExit(reader);
            if (!askToSave(filename, reader)) {
                System_all.println("Not saving...\nBYE");
                return;
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
            System.err.println("\nProceeding with save due to error receiving user input\n");
        }

        System.out.println("SAVING AS " + filename);

        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename))) {
            out.writeObject(new SavedNets());

        } catch (IOException e) {
            e.printStackTrace(System.err);
        }

        System_all.print("BYE");
    });

    public static class SavedNets implements Serializable {
        public static final long serialVersionUID = 5228078204316649402L;

        public final List<BoardNet> fittest = TestAddNeurons.fittest;
        public final Map<Long, BoardNet> hashes = TestAddNeurons.hashes;
        public final Map<Long, Set<BoardNet>> hashCollisions = TestAddNeurons.hashCollisions;
        public final Map<BoardNet, Long> legacy = TestAddNeurons.legacy;

        private SavedNets() { }

        public void restoreState() {
            long edgeHash = EDGE_NET.getNeuralHash();
            long randHash = RAND_NET.getNeuralHash();
            BoardNet edge = hashes.get(edgeHash);
            BoardNet rand = hashes.get(randHash);

            // re-calculate the hashes
            for (Map.Entry<Long, BoardNet> entry : this.hashes.entrySet()) {
                BoardNet net = entry.getValue();
                long hash = net.getNeuralHash();
                long oldHash = entry.getKey();
                if (hash != oldHash) {
                    System.err.println("Old hash no longer matches current algorithm: "
                            + oldHash + "\n" + net);
                }

                if (hash == edgeHash && net.generation == 0) edge = net;
                else if (hash == randHash && net.generation == 0) rand = net;
                else TestAddNeurons.hashes.put(hash, net);
            }

            if (edge == null || rand == null) {
                for (BoardNet net : this.fittest) {
                    if (net.generation != 0 || net.getLineage().length != 0) continue;
                    long hash = net.getNeuralHash();
                    if (edge == null && hash == edgeHash) edge = net;
                    else if (rand == null && hash == randHash) rand = net;
                }
            }


            if (edge != null && edge.generation == 0) this.fittest.remove(edge);
            else throw new IllegalStateException();


            if (rand != null && rand.generation == 0) this.fittest.remove(rand);
            else throw new IllegalStateException();

            this.fittest.add(EDGE_NET);
            this.fittest.add(RAND_NET);
            currentNets = new HashSet<>(this.fittest);

            for (Map.Entry<Long, Set<BoardNet>> entry : hashCollisions.entrySet()) {
                Set<BoardNet> displaced = TestAddNeurons.hashCollisions.put(entry.getKey(), entry.getValue());
                if (displaced != null) entry.getValue().addAll(displaced);
            }

            Map<BoardNet, Long> legacy = this.legacy;

            if (legacy == null) {
                legacy = new LinkedHashMap<>();

                long current = NeuralNet.getCurrentGeneration();

                for (BoardNet net : this.fittest) {
                    if (net == EDGE_NET || net == RAND_NET) continue;
                    if (net.generation >= current) continue;

                    long diff = current - net.generation;
                    long useInterned = current - (long) Math.floor((double) diff / 3.0) + 3;
                    if (useInterned < current) legacy.put(net, useInterned);
                }

                System.err.println("Legacy map was missing... filled in estimated legacy values");
            }

            // sort the legacies to go from oldest to newest, and add to the actual legacy map
            List<Map.Entry<BoardNet, Long>> entryList = new ArrayList<>(legacy.entrySet());
            Collections.sort(entryList, Comparator.comparing(Map.Entry::getValue));

            for (Map.Entry<BoardNet, Long> entry : entryList) {
                TestAddNeurons.legacy.put(entry.getKey(), entry.getValue());
            }
            System.out.println();
        }
    }

    public static void main(String[] args) throws FileNotFoundException {
        PrintStream outFile = new PrintStream(new FileOutputStream(FILENAME + ".out.txt")),
                errFile = new PrintStream(new FileOutputStream(FILENAME + ".err.txt")),
                outTee = new TeePrintStream(System.out, outFile),
                errTee = new TeePrintStream(System.err, errFile, outFile);

        System_all = new TeePrintStream(System.out, outFile, errFile);
        System.setOut(outTee);
        System.setErr(errTee);

        if (args.length > 0) {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(args[0]))) {
                SavedNets nets = (SavedNets) in.readObject();
                nets.restoreState();
                System.out.println("Successfully loaded saved file '" + args[0] + "'");

            } catch (Exception e) {
                e.printStackTrace(System.err);
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                    while (true) {
                        System.err.println("Error loading save file " + args[0] + "\n continue anyways? Enter 'y' or 'n'");
                        String output = reader.readLine().toLowerCase();
                        if ("y".equals(output)) break;
                        else if ("n".equals(output)) return;
                        System.err.println("Invalid response");
                    }

                } catch (Exception e2) {
                    throw new RuntimeException(e2);
                }
            }
        }

        Runtime.getRuntime().addShutdownHook(SHUTDOWN_HOOK);

        legacy.put(EDGE_NET, Long.MAX_VALUE);
        legacy.put(RAND_NET, Long.MAX_VALUE);

        for (int i = 0; i < THREADS; i++) {
            new TestAddNeurons().start();
        }

        Set<BoardNet> fittest = currentNets;
        boolean incrementGeneration = fittest.size() < NETS_PER_GENERATION;

        for (long i = NeuralNet.getCurrentGeneration(), end = i + GENERATIONS;
             i <= end && !finished; i++) {

            if (incrementGeneration) i = NeuralNet.nextGeneration(); //only increment to nextGeneration if i < end, so the Shutdown hook has correct generations
            else incrementGeneration = true; //for the first iteration only

            fittest = iteration(fittest, i);
        }

        checkLegacy(fittest, currentNets, NeuralNet.getCurrentGeneration());
        System.out.println("MAIN THREAD HAS FINISHED CRITICAL SHUTDOWN TASKS... cleaning up");

        synchronized (currentIterator) {
            currentNets = fittest;
            finished = true;
            currentIterator.notifyAll();
        }

        filterHashes();
    }

    private static void iteration() {
        Set<BoardNet> previousNets = currentNets;
        currentNets = makeMutations(fittest);

        currentNets.addAll(legacy.keySet());

        dispatchWorkerThreads();
        doWaitingTasks(previousNets);

        Collections.sort(fitnesses);

        fittest = new ArrayList<>(RETAIN_BEST);
        for (BoardNetFitness fitness : fitnesses) {
            fittest.add(fitness.net);
            if (fittest.size() >= RETAIN_BEST) break;
        }
    }

    public static int[] mutationCounts(int fittestCount, int netsToMake) {
        int[] makeMutations = new int[fittestCount];

        if (netsToMake <= fittestCount) {
            if (netsToMake > 0) {
                Arrays.fill(makeMutations, 0, netsToMake, 1);

            } else {
                netsToMake = 0;
            }

        } else {

            int evenlyDistribute = Math.min(netsToMake, Math.max(fittestCount, (int) Math.ceil((double) netsToMake * 2.0 / 3.0)));
            int baseline = (int) Math.floor((double) evenlyDistribute / (double) fittestCount);

            if (baseline * fittestCount != evenlyDistribute) {
                if (netsToMake - baseline * fittestCount > fittestCount) {
                    baseline++;
                }
                evenlyDistribute = baseline * fittestCount;
            }

            double[] mmFloat = new double[fittestCount];
            int geoDistribute = netsToMake - evenlyDistribute;

            double offset = baseline + 2.0 * (double)geoDistribute / (double) fittestCount;
            double slope = offset / fittestCount;
            offset += baseline;

            int sum = 0;

            for (int i = 0; i < fittestCount; i++) {
                double flt = offset - slope * i;
                assert flt >= 0;
                int intg = (int)Math.round(flt);
                mmFloat[i] = flt;
                makeMutations[i] = intg;
                sum += intg;
            }

            assert sum >= netsToMake;

            if (sum > netsToMake) {
                List<Map.Entry<Integer, Double>> diffs = new ArrayList<>(fittestCount);
                //key = index, value = difference between floating and actual

                for (int i = 0; i < fittestCount; i++) {
                    diffs.add(new AbstractMap.SimpleEntry<>(i, mmFloat[i] - makeMutations[i]));
                }

                while (sum > netsToMake) {
                    diffs.sort((e1, e2) -> {
                        double diff1 = e1.getValue(), diff2 =  e2.getValue();
                        if (diff1 < diff2) return -1;
                        if (diff2 < diff1) return 1;
                        if (diff1 == diff2) return 0;
                        throw new IllegalStateException(diff1 + "\n" + diff2);
                    });

                    for (Map.Entry<Integer, Double> entry : diffs) {
                        int index = entry.getKey();
                        if (makeMutations[index] == 1) continue;
                        makeMutations[index]--;
                        entry.setValue(mmFloat[index] - makeMutations[index]);
                        if (--sum <= netsToMake) break;
                    }
                }
            }
        }

        assert Arrays.stream(makeMutations).sum() == netsToMake;
        return makeMutations;
    }

    private static Set<BoardNet> makeMutations(List<BoardNet> fittest) {
        Set<BoardNet> newGen = new LinkedHashSet<>(NETS_PER_GENERATION + legacy.size());
        newGen.addAll(fittest);

        int[] counts = mutationCounts(fittest.size(), NETS_PER_GENERATION - fittest.size());

        Iterator<BoardNet> iterator = fittest.iterator();
        for (int n = 0; n < fittest.size(); n++) {
            int make = counts[n];
            if (make < 1) break;

            AddNeurons<BoardNet> mutator = new AddNeurons<>(iterator.next());
            List<BoardNet> mutations = mutator.mutate(make);
            newGen.addAll(mutations);
        }

        return newGen;
    }

    // only call with on a lock on currentIterator
    private static void dispatchWorkerThreads(long gen) {
        System.out.println();
        System_all.println("\n-----------------------------------------------------\nGENERATION " + gen);
        System.out.println("\n");

        fitnesses = new ArrayList<>(currentNets.size());

        synchronized (currentIterator) {
            currentIterator.value = currentNets.iterator();
            currentIterator.notifyAll();
        }
    }

    private static void doWaitingTasks(long gen, Set<BoardNet> previousNets) {
        // while the worker threads are going, calculate the hashes and add them to the hashes map.
        // Iterate in REVERSE order as the worker threads, so there is less chance
        // of the worker threads blocking main or vice-versa, while calculating the hashes
        LinkedList<BoardNet> netsCopy = new LinkedList<>(currentNets);
        for (Iterator<BoardNet> iterator = netsCopy.descendingIterator();
             iterator.hasNext();) {

            BoardNet net = iterator.next();
            if (net.generation < gen) continue; //only do this for new nets
            long hash = net.getNeuralHash();
            if (hashes.containsKey(hash)) checkForHashCollision(hash, net);
            hashes.put(hash, net);
        }

        if (fittest != previousNets) {
            // they are the same instance only on the first iteration, in which case we shouldn't process them
            // check legacy is processing from the *PREVIOUS* round
            checkLegacy(previousNets, gen - 1);
        }

        Set<BoardNet> legaciesToCheck = new LinkedHashSet<>(legacy.keySet());
        legaciesToCheck.retainAll(currentNets);

        if (legaciesToCheck.size() > 0) {
            //wait for the currentNets iterator to finish then swap out for the legaciesToCheck iterator.
            // The swapping out task is mostly waiting, so this can be delegated to a small extra thread
            // while the main thread focuses on filterHashes()
            Thread iteratorSwapper = new Thread(() -> {
                synchronized (currentIterator) {
                    while (currentIterator.value.hasNext()) {
                        try {
                            currentIterator.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace(System.err);
                        }
                    }
                    currentIterator.value = legaciesToCheck.iterator();
                    currentIterator.notifyAll();
                }
            });

            iteratorSwapper.start();
            filterHashes();
            boolean addedLegacies = false;

            while (runRound() && iteratorSwapper.isAlive()) { }

            while (iteratorSwapper.isAlive()) {
                try { iteratorSwapper.join(); }
                catch (InterruptedException e) { e.printStackTrace(System.err); }
            }

            currentNets.addAll(legaciesToCheck); //wait till after filterHashes is done AND the currentNets iterator
                                            // is finished so we don't get a ConcurrentModificationException

        } else {
            synchronized (threadsIdle) {
                if (threadsIdle.size() >= THREADS) return;
            }
            filterHashes(); //garbage collection, nice to do but not necessary every time
        }

        while(runRound()) { }
        waitForWorkerThreads();
    }

    private static final BoardInterface mainsBoard = new BoardInterface();

    private static boolean runRound() {
        if (finished) return false;
        BoardNet net;
        synchronized (currentIterator) {
            if (finished || !currentIterator.value.hasNext()) return false;
            net = currentIterator.value.next();;
            currentIterator.notifyAll();
        }

        if (net.generation == NeuralNet.getCurrentGeneration()) net.traceNeuronsSet();
        BoardNetFitness fitness = mainsBoard.testFitness(net, null);
        System.out.println(fitness + "\n");
        synchronized (threadsIdle) {
            fitnesses.add(fitness);
        }
        return true;
    }

    private static void waitForWorkerThreads() {
        synchronized (threadsIdle) {
            while (threadsIdle.size() < THREADS && !finished) {
                try {
                    threadsIdle.wait();

                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
            }
        }
    }

    /** This check is done on the _previous_ rounds' results while the current round is running,
     * in order to optimize concurrency usage. The invoker will pass gen - 1 as the gen arg, along
     * with last rounds' 'fittest' set and set of nets.  This method cannot use the currentNets
     * set as it is for the current round.
     *
     * @param fittest
     * @param gen
     */
    private static void checkLegacy(Set<BoardNet> previousNets, long gen) {
        // previousNets is immediately mutated to remove the fittest nets, and therefore is effectively
        // 'notFittest'

        long genMinus2 = gen - 2;
        fittest.forEach(net -> {
            previousNets.remove(net);
            if (net == EDGE_NET || net == RAND_NET) return;
            legacy.computeIfPresent(net, (n, genInterned) -> genInterned < genMinus2 ? genInterned + 2 : null);
            /** For legacies that have made it into the "fittest" set, add two to the generation interned
             (and remove if that brings it up to the current generation).
             This makes it so that very old legacies still need to be in the "fittest" Set 1/3rd of the time
             in order to be kept as legacy (two times not in the set, to one time in the set, in order to survive)
             */
        });

        for (BoardNet net : previousNets) {
            if (net.generation == gen) continue;
            long interned = legacy.computeIfAbsent(net, n -> gen);

            if (interned - net.generation < gen - interned) {
                legacy.remove(net);
            }
        }

        for (Iterator<Map.Entry<BoardNet, Long>> iterator = legacy.entrySet().iterator();
                iterator.hasNext();) {

            Map.Entry<BoardNet, Long> entry = iterator.next();
            BoardNet net = entry.getKey();
            long interned = entry.getValue();

            if (interned - net.generation < gen - interned) {
                System.err.println("Legacy map contained an unaccounted old net which had to be removed:\n\t" + net);
                iterator.remove();
            }
        }

        int foundRandLineage = 0;
        for (BoardNet net : fittest) {
            long[] lineage = net.getLineage();

            if (lineage.length == 0
                    ? net == RAND_NET
                    : lineage[lineage.length - 1] == RAND_HASH) {

                foundRandLineage++;
            }
        }
        if (foundRandLineage >= MIN_RAND_LINEAGE_RETAINED) return;

        for (BoardNetFitness fitness : fitnesses) {
            if (fittest.contains(fitness.net)) continue;

            long[] lineage = fitness.net.getLineage();

            if (lineage.length == 0
                    ? fitness.net == RAND_NET
                    : lineage[lineage.length - 1] == RAND_HASH) {

                legacy.computeIfAbsent(fitness.net, n -> gen);
                if (foundRandLineage++ == 0L) fittest.add(fitness.net); //allow the best rand lineage to mutate
                else if (foundRandLineage >= MIN_RAND_LINEAGE_RETAINED) return;
            }
        }
    }

    private static void checkForHashCollision(long hash, BoardNet newNet) {
        BoardNet oldNet = hashes.get(hash);
        if (oldNet == newNet) return;

        System.err.println("POSSIBLE HASH COLLISION: " + NeuralHash.toHex(hash)
                + "\n\t" + newNet + "\n\t" + oldNet);

        long[] newLin = newNet.getLineage(), oldLin = oldNet.getLineage();
        double kinship = NeuralHash.collisionKinship(hash, newLin, oldLin);
        System.err.print("Kinship score: " + kinship);
        double smallerSize = Math.max(1, Math.min(newLin.length, oldLin.length));
        if (kinship < (smallerSize - 1) / smallerSize) {
            System.err.println(" ... too small");
            System.err.println("Lineages appear too different.  Adding these networks to the hash collisions map");
            hashCollisions.computeIfAbsent(hash, h -> new HashSet<>()).addAll(List.of(newNet, oldNet));
            return;
        }

        System.err.println(" ... good.  Continuing to other checks");


        int decisionsEqual = 0;
        for (DualIterable.Pair<BoardNet.Decision> pair
                : new DualIterable<>(newNet.getDecisionNodes(), oldNet.getDecisionNodes())) {

            BoardNet.Decision d1 = pair.value1(), d2 = pair.value2();
            long hash1 = d1.getNeuralHash(), hash2 = d2.getNeuralHash();

            if (hash1 != hash2) {
                System.err.println("Decision nodes (" + d1.getDecisionId()
                        + "," + d2.getDecisionId()  + ") had different hashes: "
                        + NeuralHash.toHex(hash1) + "  " + NeuralHash.toHex(hash2));
                break;
            }

            SignalProvider input1 = pair.value1().getInputs().get(0);
            SignalProvider input2 = pair.value2().getInputs().get(0);

            hash1 = input1.getNeuralHash();
            hash2 = input2.getNeuralHash();

            if (input1.getClass() != input2.getClass() || hash1 != hash2) {
                System.err.println("Inputs for decision nodes (" + d1.getDecisionId()
                        + "," + d2.getDecisionId()  + ") don't match "
                        + input1 + "\n\t" + input2 + "\n\t" + hash1 + "\t" + hash2);
                break;
            }

            if (input1 instanceof CachingNeuronUsingFunction func1) {
                CachingNeuronUsingFunction func2 = (CachingNeuronUsingFunction) input2;
                hash1 = func1.outputFunction.getNeuralHash();
                hash2 = func2.outputFunction.getNeuralHash();

                if (func1.outputFunction.getClass() != func2.outputFunction.getClass()
                        || hash1 != hash2
                        || func1.outputFunction.hashHeader()    != func2.outputFunction.hashHeader()) {

                    System.err.println("Inputs' functions for decision nodes (" + d1.getDecisionId()
                            + "," + d2.getDecisionId()  + ") don't match "
                            + "\n\t" + func1.outputFunction + "\t" + func2.outputFunction
                            + "\n\t" + hash1 + "\t" + hash2
                            + "\n\t" + func1.outputFunction.hashHeader() + "\t" + func2.outputFunction.hashHeader());
                    break;
                }

                if (func1 instanceof CachingNeuronUsingTweakableFunction tweak1) {
                    CachingNeuronUsingTweakableFunction tweak2 = (CachingNeuronUsingTweakableFunction)input2;
                    short[] toAchieve = tweak1.getTweakingParams(tweak2);
                    for (short param : toAchieve) {
                        if (param != 0) {
                            System.err.println("toAchieve tweaks indicate the params are different\t"
                                    + Util.toString(toAchieve));
                            break;
                        }
                    }
                }
            }
            decisionsEqual++;
        }

        if (decisionsEqual == 5) {
            System.err.println("Decision nodes appear to have equivalent inputs");
            return;
        }

        System.err.println("Decision nodes appear to have different inputs.  Adding these networks to the hash collisions map");

        hashCollisions.computeIfAbsent(hash, h -> new HashSet<>()).addAll(List.of(newNet, oldNet));
    }

    private BoardNet getNext() {
        Thread current = Thread.currentThread(); //should be this
        assert current == this;

        boolean addedToThreadsIdle = false;
        try {
            synchronized (currentIterator) {
                if (finished) return null;
                while (currentIterator.value == null || !currentIterator.value.hasNext()) {
                    synchronized (threadsIdle) {
                        addedToThreadsIdle = true;
                        threadsIdle.add(current);
                        if (threadsIdle.size() >= THREADS) {
                            threadsIdle.notifyAll(); //tell the main thread that worker threads are done
                        }
                    }
                    try {
                        currentIterator.wait();

                    } catch (InterruptedException e) {
                        System.err.println("Worker tread, Id: " + current.getId());
                        e.printStackTrace(System.err);
                    }
                    if (finished) return null;
                }
                currentIterator.notifyAll();
                return currentIterator.value.next();

            }
        } finally {
            if (addedToThreadsIdle) {
                synchronized (threadsIdle) {
                    threadsIdle.remove(current);
                }
            }
        }
    }

    private BoardNet addResult(BoardNetFitness fitness) {
        synchronized (threadsIdle) {
            fitnesses.add(fitness);
        }

        return getNext();
    }

    private BoardInterface board = new BoardInterface();
    private TestAddNeurons() {
        threads.add(this);
    }

    public void run() {
        try (UniqueAffinityLock af = UniqueAffinityLock.obtain()) {
            runWithAffinity();

        } finally {
            synchronized (threadsIdle) {
                // in case a thread dies from uncaught exception, we don't want the main thread waiting for it
                threadsIdle.add(Thread.currentThread());
            }
        }
    }

    private void runWithAffinity() {
        BoardNet net = getNext();
        while (net != null) {
            if (net.generation == NeuralNet.getCurrentGeneration()) net.traceNeuronsSet();
            BoardNetFitness fitness = board.testFitness(net, null);
            System.out.println(fitness + "\n");
            net = addResult(fitness);
        }
    }


    /**
     * Shutdown hook methods below
     *
     */

    private static void waitForThreadsToExit(BufferedReader reader) {
        finished = true;
        System.err.println("\n*****************************************************************************\n\n"
                + "PROCESS INTERRUPTED ... waiting for threads to finish\n"
                + "Press Enter to continue without waiting...\n"
                + "\n*****************************************************************************\n");

        Thread current = Thread.currentThread();
        Var.Bool exit = new Var.Bool(false);

        Thread listener = new Thread(() -> {
            //listen for user input, to skip waiting for the other threads
            while (!exit.value) try {
                Thread.sleep(200);
                if (reader.ready()) {
                    if (!exit.value) {
                        exit.value = true;
                        current.interrupt();
                    }
                    return;
                }

            } catch (IOException e) {
                e.printStackTrace(System.err);

            } catch (InterruptedException e) {
                if (!exit.value) {
                    e.printStackTrace(System.err);
                }
            }

        });
        listener.start();

        //send notifications to the two monitor objects, but don't block this thread waiting to obtain
        // the lock on them
        new Thread(() -> {
            synchronized (currentIterator) { currentIterator.notifyAll(); }
        }).start();

        new Thread(() -> {
            synchronized (threadsIdle) { threadsIdle.notifyAll(); }
        }).start();


        for (TestAddNeurons thread : threads) {
            while (!exit.value && thread.isAlive()) {
                thread.interrupt();
                try {
                    thread.join(1000);

                } catch (InterruptedException e) {
                    if (!exit.value) e.printStackTrace(System.err);
                }
            }
        }
        if (exit.value) {
            System_all.println("\nProceeding without waiting for threads to exit...\n");

        } else {
            exit.value = true;
            if (listener.isAlive()) try {
                listener.interrupt();
                listener.join(1000);
            }
            catch (InterruptedException e) { }
            finally { Thread.interrupted(); } // clear interrupted status of this thread
        }
    }

    private static boolean askToSave(String filename, BufferedReader reader) {
        long lastErr = 0;
        try {
            while (reader.ready()) reader.read(); //clear input stream

        } catch(IOException e) {
            e.printStackTrace(System.err);
        }


        while (true) {
            try {
                System.out.println("Save results as '" + filename + "'\n\tenter 'y' or 'n'");

                String in = reader.readLine().toLowerCase();
                if (in.equals("n")) return false;
                else if (in.equals("y")) return true;

                System.err.println("Invalid input");

            } catch (IOException e) {
                e.printStackTrace(System.err);
                if (System.currentTimeMillis() - lastErr < 1000) {
                    System.err.println("Repeated input errors... saving as " + filename);
                    return true;
                }
                lastErr = System.currentTimeMillis();
            }
        }
    }

    private static void filterHashes() {
        //System.out.println("\nMain thread is filtering hashes to allow for garbage collection of extinct lineages\n");

        Set<Long> preserve = new TreeSet<>();
        for (Object nets : List.of(currentNets, legacy.keySet())) {
            for (BoardNet net : (Iterable<BoardNet>)nets) {
                preserve.add(net.getNeuralHash());
                for (long hash : net.getLineage()) {
                    preserve.add(hash);
                }
            }
        }

        hashes.keySet().retainAll(preserve);
    }
}