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
import java.util.function.*;
import java.util.stream.*;

import static neuralNet.util.Util.*;

public class TestAddNeurons extends Thread {
    public static final int GENERATIONS = 2048;
    public static final int NETS_PER_GENERATION = 32;
    public static final int REPRODUCE_BEST = 8;
    public static final int THREADS = 6;
    public static final int MIN_RAND_LINEAGE_RETAINED = 4;
    public static final int RETEST_FREQUENCY = 16; //max generations before a new fitness test is conducted on a retained/legacy net
    public static final long NS_SLOW_HASH_CALCULATION = BILLION_LONG / 4;

    private static final BoardNet EDGE_NET = TestBoardNet.makeEdgeNet(),
            RAND_NET = TestBoardNet.makeRandomNet();

    public static final Predicate<BoardNetFitness> RAND_NET_FITNESS_FILTER = f -> isRandLineage(f.net);

    private static final List<BoardNet> randNetsToKeep = new ArrayList<>(MIN_RAND_LINEAGE_RETAINED);

    private static KeepEdgeAndRand KEEP_EDGE_AND_RAND = KeepEdgeAndRand.INSTANCE;
    private enum KeepEdgeAndRand implements NetTracker.KeepLambda<BoardNet> {
        INSTANCE;

        @Override
        public boolean keep(long currentGen, long genRating, BoardNet net) {
            if (net == EDGE_NET || net == RAND_NET || randNetsToKeep.contains(net)) return true;
            else return NetTracker.DEFAULT_KEEP_LAMBDA.keep(currentGen, genRating, net);
        }
    }

    public static final long RAND_HASH = RAND_NET.getNeuralHash();

    public static final String FILENAME = "TestAddNeurons "
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH_mm_ss"));

    private static final List<TestAddNeurons> threads = new ArrayList<>(THREADS);
    private static final Thread mainThread = Thread.currentThread();

    private static Set<BoardNet> currentNets = Set.of(EDGE_NET, RAND_NET);
    private static Set<BoardNet> fittest;
    private static final Var<Iterator<BoardNet>> currentIterator = new Var<>();
    private static final Set<Thread> threadsIdle = new HashSet<>();
    private static TreeSet<BoardNetFitness> fitnesses;
    private static final NetTracker<BoardNet, BoardNetFitness> netTracker = new NetTracker<>(KEEP_EDGE_AND_RAND);

    private static final PrintStream outFile;
    private static final PrintStream errFile;

    static {
        try {
            outFile = new PrintStream(new FileOutputStream(FILENAME + ".out.txt"));
            errFile = new PrintStream(new FileOutputStream(FILENAME + ".err.txt"));

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static final GenerationHeaderPrintStream
            outGenTee = new GenerationHeaderPrintStream(System.out, outFile),
            errGen = new GenerationHeaderPrintStream(errFile);

    private static final TeePrintStream
            errTee = new TeePrintStream(System.err, errGen, outFile),
            System_all = new TeePrintStream(outGenTee, errGen);

    private static boolean exit = false;
    private static boolean finished = false;

    private static final Thread SHUTDOWN_HOOK = new Thread(TestAddNeurons::shutdownHook);

    private static boolean loadSaved(String filename) {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename))) {
            Object nets = in.readObject();
            if (nets instanceof SavedNets savedNets) {
                savedNets.restoreState();

            } else if (nets instanceof NetTracker t) {
                NetTracker<BoardNet, BoardNetFitness> tracker = (NetTracker<BoardNet, BoardNetFitness>)t;

                //make copies to avoid ConcurrentModificationException when iterating and removing
                Set<BoardNet> edgeNets = new HashSet<>(tracker.getHash(EDGE_NET.getNeuralHash()));
                Set<BoardNet> randNets = new HashSet<>(tracker.getHash(RAND_HASH));

                List<Long> edgeGenRatings = new ArrayList<>(edgeNets.size());
                List<Long> randGenRatings = new ArrayList<>(randNets.size());

                for (BoardNet net : edgeNets) {
                    if (net.generation != 0) {
                        System.err.println("Unexpected hash match to EDGE_NET:\n\t" + net);
                        continue;
                    }
                    edgeGenRatings.add(tracker.getGenRating(net));
                    tracker.removeFromAll(net);
                }

                for (BoardNet net : randNets) {
                    if (net.generation != 0) {
                        System.err.println("Unexpected hash match to RAND_NET:\n\t" + net);
                        continue;
                    }
                    randGenRatings.add(tracker.getGenRating(net));
                    tracker.removeFromAll(net);
                }

                if (randGenRatings.size() == 0 || edgeGenRatings.size() == 0) {
                    throw new IllegalStateException();
                }

                long edgeRating = Math.round(edgeGenRatings.stream().map(l -> (double)l).reduce(0.0, Double::sum) / edgeGenRatings.size());

                long randRating = Math.round(randGenRatings.stream().map(l -> (double)l).reduce(0.0, Double::sum) / randGenRatings.size());

                tracker.setGenRating(EDGE_NET, edgeRating);
                tracker.setGenRating(RAND_NET, randRating);

                netTracker.copyFrom(tracker);

                currentNets = netTracker.removeSpecialNets("currentNets");
                if (currentNets == null) currentNets = tracker;
                else currentNets.addAll(tracker);
            }

        } catch (Exception e) {
            e.printStackTrace(System.err);
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                while (true) {
                    System.err.println("Error loading save file " + filename + "\n continue anyways? Enter 'y' or 'n'");
                    String output = reader.readLine().toLowerCase();
                    if ("y".equals(output)) return true;
                    else if ("n".equals(output)) return false;
                    System.err.println("Invalid response");
                }

            } catch (Exception e2) {
                throw new RuntimeException(e2);
            }
        }

        System.out.println("Successfully loaded saved file '" + filename + "'");
        return true;
    }

    public static void main(String[] args) throws FileNotFoundException {
        if (Thread.currentThread() != mainThread) throw new IllegalStateException();

        System.setOut(outGenTee);
        System.setErr(errTee);

        if (args.length > 0) {
            if (!loadSaved(args[0])) return;
        }

        Runtime.getRuntime().addShutdownHook(SHUTDOWN_HOOK);

        netTracker.add(EDGE_NET);
        netTracker.add(RAND_NET);

        for (int i = 0; i < THREADS; i++) {
            new TestAddNeurons().start();
        }

        fittest = currentNets;
        boolean incrementGeneration = fittest.size() < NETS_PER_GENERATION;
        long i = NeuralNet.getCurrentGeneration(), end = i + GENERATIONS;
        for (;i <= end && !exit; i++) {

            if (incrementGeneration) i = NeuralNet.nextGeneration(); //only increment to nextGeneration if i < end, so the Shutdown hook has correct generations
            else incrementGeneration = true; //for the first iteration only

            iteration(i);
        }

        netTracker.cullOld();

        System.out.println("MAIN THREAD HAS FINISHED CRITICAL SHUTDOWN TASKS... cleaning up");
        finished = i >= end & !exit;

        synchronized (currentIterator) {
            currentNets = fittest;
            exit = true;
            currentIterator.notifyAll();
        }
    }

    private static void iteration(long gen) {
        boolean notFirstIteration = fitnesses != null; //skip on the first time around

        if (notFirstIteration) outGenTee.printGenerationHeader();
                // let the work threads do it on the first iteration
                // so the error message from AffinityLock library is not under the genHeader

        currentNets = makeMutations();
        fitnesses = new TreeSet<>();

        dispatchWorkerThreads(gen);

        if (notFirstIteration) processLegacies(gen);
        while (runRound()) { }
        waitForWorkerThreads();
        if (exit) return;

        fittest = netTracker.addFittest(fitnesses, REPRODUCE_BEST);
        checkForRandNets();
    }

    private static Set<BoardNet> makeMutations() {
        Set<BoardNet> newGen = new LinkedHashSet<>(NETS_PER_GENERATION + netTracker.size());
        newGen.addAll(fittest);

        int[] counts = MutatorFactory.calcCounts(fittest.size(), NETS_PER_GENERATION - fittest.size());

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
        synchronized (currentIterator) {
            currentIterator.value = currentNets.iterator();
            currentIterator.notifyAll();
        }
    }

    private static final BoardInterface mainsBoard = new BoardInterface();

    private static boolean runRound() {
        if (exit) return false;
        BoardNet net;
        synchronized (currentIterator) {
            if (exit || !currentIterator.value.hasNext()) return false;
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
            while (threadsIdle.size() < THREADS) {
                try {
                    threadsIdle.wait();

                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
            }
        }
    }

    private static boolean isRandLineage(BoardNet net) {
        long[] lineage = net.getLineage();

        return lineage.length == 0
                ? net == RAND_NET
                : lineage[lineage.length - 1] == RAND_HASH;
    }

    private static void processLegacies(long gen) {
        netTracker.cullOld(gen - 1);

        Set<BoardNet> legaciesToCheck = new LinkedHashSet<>();

        // The worst fitnesses are allowed to attempt again, rather than using their saved
        // fitnesses for the next ~16 rounds

        int worstCount = 0;
        long genPlusRetest = gen + RETEST_FREQUENCY;

        for (Iterator<BoardNetFitness> iterator = netTracker.getFitnesses().descendingIterator();
                iterator.hasNext();) {

            BoardNetFitness fitness = iterator.next();
            BoardNet net = fitness.net;
            if (net == null) throw new NullPointerException();

            if (worstCount < REPRODUCE_BEST) {
                if (!currentNets.contains(net)) {
                    worstCount++;
                    legaciesToCheck.add(net);
                }

            } else if (gen - fitness.generation > RETEST_FREQUENCY) {
                if (!currentNets.contains(net)) legaciesToCheck.add(net);

            } else if (!KEEP_EDGE_AND_RAND.keep(genPlusRetest, netTracker.getGenRating(fitness.net), fitness.net)) {
                if (!currentNets.contains(net)) legaciesToCheck.add(net);
            }

        }


        if (legaciesToCheck.size() == 0) return;


        //wait for the currentNets iterator to finish then swap out for the legaciesToCheck iterator.
        // The swapping out task is mostly waiting, so this can be delegated to a small extra thread
        // while the main thread focuses on filterHashes()
        Thread iteratorSwapper = new Thread(() -> {
            if (exit) return;
            synchronized (currentIterator) {
                while (currentIterator.value.hasNext()) {
                    if (exit) return;
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

        while (iteratorSwapper.isAlive()) {
            if (exit) return;
            if (!runRound()) {
                try { iteratorSwapper.join(1000); }
                catch (InterruptedException e) { e.printStackTrace(System.err); }
            }
        }

        currentNets.addAll(legaciesToCheck); //wait till after filterHashes is done AND the currentNets iterator
                                            // is finished so we don't get a ConcurrentModificationException
    }

    private static void checkForRandNets() {
        randNetsToKeep.clear();

        Iterator<BoardNetFitness> oldF = netTracker.getFitnesses().stream().filter(RAND_NET_FITNESS_FILTER).iterator();
        Iterator<BoardNetFitness> newF = fitnesses.stream().filter(RAND_NET_FITNESS_FILTER).iterator();

        BoardNetFitness bestRand = null;
        BoardNetFitness next = null;

        while (randNetsToKeep.size() < MIN_RAND_LINEAGE_RETAINED) {
            if (next == null) {
                if (newF.hasNext()) next = newF.next();
                else break;
                if (next.net == null) throw new NullPointerException();
            }

            if (bestRand == null) {
                if (oldF.hasNext()) {
                    bestRand = oldF.next();
                    if (bestRand.net == null) throw new NullPointerException();

                } else {
                    randNetsToKeep.add(next.net);
                    while (randNetsToKeep.size() < MIN_RAND_LINEAGE_RETAINED && newF.hasNext()) {
                        next = newF.next();
                        if (next.net == null) throw new NullPointerException();
                        randNetsToKeep.add(next.net);
                    }
                    break;
                }
            }

            if (((next == bestRand) && (next = null) ==  null) || next.compareTo(bestRand) > 0) {
                randNetsToKeep.add(bestRand.net);
                bestRand = null;

            } else {
                randNetsToKeep.add(next.net);
                next = null;
            }
        }

        if (randNetsToKeep.size() > 0) {
            fittest.add(randNetsToKeep.get(0));

        } else {
            fittest.add(RAND_NET);
        }
    }

    private BoardNet getNext() {
        Thread current = Thread.currentThread(); //should be this
        assert current == this;

        boolean addedToThreadsIdle = false;
        try {
            synchronized (currentIterator) {
                if (exit) return null;
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
                    if (exit) return null;
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
            // force the initial generation headers to print (especially in the error file)
            // AFTER the error message from AffinityLock library has already gone through System.err
            outGenTee.startGenHeaders();
            outGenTee.printGenerationHeader();
            errGen.startGenHeaders();
            errGen.printGenerationHeader();


            runWithAffinity();

        } finally {
            synchronized (threadsIdle) {
                // in case a thread dies from uncaught exception, we don't want the main thread waiting for it
                threadsIdle.add(Thread.currentThread());
                threadsIdle.notifyAll();
            }
        }
    }

    private void runWithAffinity() {
        BoardNet net = getNext();
        while (net != null) {
            if (net.generation == NeuralNet.getCurrentGeneration()) {
                net.traceNeuronsSet();
                long start = System.nanoTime();
                long hash = net.getNeuralHash();
                long end = System.nanoTime();
                if (end - start >= NS_SLOW_HASH_CALCULATION) {
                    System.err.println("SLOW HASH (" + ((double)(end - start) / BILLION) + "seconds) : " + NeuralHash.toHex(hash));
                    synchronized (netTracker) {
                        netTracker.addToSpecialNets("Slow Hashes", net);
                    }
                }
            }
            BoardNetFitness fitness = board.testFitness(net, null);
            System.out.println(fitness + "\n");
            net = addResult(fitness);
        }
    }


    /**
     * Shutdown hook methods below
     *
     */
    private static void shutdownHook() {
        String filename = FILENAME + " (gen " + NeuralNet.getCurrentGeneration() + ").nnt";

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        if (!finished) {
            waitForThreadsToExit(reader);
            if (!askToSave(filename, reader)) {
                System_all.println("Not saving...\nBYE");
                return;
            }

            try {
                netTracker.addToSpecialNets("currentNets", currentNets);

            } catch (Exception e) {
                System.err.println("Exception while trying to add the currentNets to the netsTracker ... proceeding to save without this ....");
                e.printStackTrace(System.err);
            }
        }

        saveNetsTracker(filename, reader);

        System_all.print("BYE");
    }

    private static void waitForThreadsToExit(BufferedReader reader) {
        exit = true;
        System.err.println("\n*****************************************************************************\n\n"
                + "PROCESS INTERRUPTED ... waiting for threads to finish before attempting to save\n"
                + "Press Enter to continue without waiting...\n"
                + "\n*****************************************************************************\n");

        Thread current = Thread.currentThread();
        Var.Bool done = new Var.Bool(false);

        Thread listener = new Thread(() -> {
            //listen for user input, to skip waiting for the other threads
            while (!done.value) try {
                Thread.sleep(200);
                if (!reader.ready()) continue;
                if (done.value) return;

                System.err.println("Skip waiting request detected... attempting to print stack trace of all threads first...\n");

                //spin up an extra thread to set done.value = true after 15 seconds,
                // in case the stack trace printing gets stuck
                new Thread(() -> {
                    try { Thread.sleep(15000); }
                    catch (InterruptedException e) {
                        done.value = true;
                        throw new RuntimeException(e);
                    }
                    done.value = true;

                }).start();

                for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
                    System.err.println(entry.getKey());
                    for (StackTraceElement element : entry.getValue()) {
                        System.err.println(element);
                    }
                    System.err.println();
                }

                done.value = true;
                current.interrupt();

                return;

            } catch (IOException e) {
                e.printStackTrace(System.err);

            } catch (InterruptedException e) {
                if (!done.value) {
                    e.printStackTrace(System.err);
                }
            }
        });
        listener.start();

        //send notifications to the two monitor objects, but don't block this thread waiting to obtain
        // the lock on them
        new Thread(() -> {
            //while(!done.value) {
                synchronized (currentIterator) {
                    currentIterator.notifyAll();
                    //try { currentIterator.wait(1000); }
                    //catch (InterruptedException e) { e.printStackTrace(System.err); }
                }
            //}
        }).start();

        new Thread(() -> {
            //while (!done.value) {
                synchronized (threadsIdle) {
                    threadsIdle.notifyAll();
                    //try { threadsIdle.wait(1000); }
                    //catch (InterruptedException e) { e.printStackTrace(System.err); }
                }
            //}
        }).start();


        for (TestAddNeurons thread : threads) {
            while (!done.value && thread.isAlive()) {
                thread.interrupt();
                try {
                    thread.join(1000);

                } catch (InterruptedException e) {
                    if (!done.value) e.printStackTrace(System.err);
                }
            }
        }

        while (!done.value && mainThread.isAlive()) try {
            mainThread.join(1000);

        } catch (InterruptedException e) {
            if (!done.value) e.printStackTrace(System.err);
        }

        if (done.value) {
            System_all.println("\nProceeding without waiting for threads to be done...\n");

        } else {
            done.value = true;
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

    private static void saveNetsTracker(String filename, BufferedReader reader) {

        while (true) try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename))) {
            System_all.println("SAVING AS " + filename);
            out.writeObject(netTracker);
            return;

        } catch (IOException e) {
            System.err.println("Exception while serializing/saving netsTracker");
            e.printStackTrace(System.err);

            while (true) try {
                System.err.println("ATTEMPT AGAIN?  (enter 'y' or 'n'");
                String in = reader.readLine().toLowerCase();

                if ("y".equals(in)) break;
                else if ("n".equals(in)) return;

                System.err.println("Invalid input: " + in);

            } catch (IOException e2) {
                System.err.println("Exception while trying to read user input... exiting");
                e2.printStackTrace(System.err);
                return;
            }
        }
    }



    public static class SavedNets implements Serializable {
        public static final long serialVersionUID = 5228078204316649402L;

        public final Collection<BoardNet> fittest = TestAddNeurons.fittest;
        public final Map<Long, BoardNet> hashes = null;
        public final Map<Long, Set<BoardNet>> hashCollisions = null;
        public final Map<BoardNet, Long> legacy = null;// = TestAddNeurons.netTracker.toMap();

        private SavedNets() { }

        public void restoreState() {
            long edgeHash = EDGE_NET.getNeuralHash();

            // re-calculate the hashes
            for (Map.Entry<Long, BoardNet> entry : this.hashes.entrySet()) {
                BoardNet net = entry.getValue();
                long hash = net.getNeuralHash();
                long oldHash = entry.getKey();
                if (hash != oldHash) {
                    System.err.println("Old hash no longer matches current algorithm: "
                            + oldHash + "\n" + net);
                }

                if (net.generation == 0 || net.getLineage().length == 0) {
                    if (hash == edgeHash) net = EDGE_NET;
                    else if (hash == RAND_HASH) net = RAND_NET;
                    else throw new IllegalStateException();
                }

                TestAddNeurons.netTracker.addToHashes(net);
            }

            currentNets = this.fittest.stream().filter(n -> n.generation != 0).collect(Collectors.toCollection(LinkedHashSet::new));
            currentNets.add(EDGE_NET);
            currentNets.add(RAND_NET);

            if (this.hashCollisions != null) {
                for (Set<BoardNet> nets : hashCollisions.values()) {
                    for (BoardNet net : nets) {
                        if (net.generation == 0 || net.getLineage().length == 0) {
                            if (net.getNeuralHash() == edgeHash) net = EDGE_NET;
                            else if (net.getNeuralHash() == RAND_HASH) net = RAND_NET;
                            else throw new IllegalStateException();
                        }
                        TestAddNeurons.netTracker.addToHashes(net);
                    }
                }
            }

            Map<BoardNet, Long> legacy = this.legacy;

            if (legacy == null) {
                legacy = new LinkedHashMap<>();

                long current = NeuralNet.getCurrentGeneration();

                for (BoardNet net : this.fittest) {
                    if (net.generation >= current) continue;
                    if (net.generation == 0 || net.getLineage().length == 0) {
                        if (net.getNeuralHash() == edgeHash) net = EDGE_NET;
                        else if (net.getNeuralHash() == RAND_HASH) net = RAND_NET;
                        else throw new IllegalStateException();
                    }

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
                BoardNet net = entry.getKey();
                if (net.generation == 0  || net.getLineage().length == 0) {
                    if (net.getNeuralHash() == EDGE_NET.getNeuralHash()) net = EDGE_NET;
                    else if (net.getNeuralHash() == RAND_HASH) net = RAND_NET;
                    else throw new IllegalStateException();
                }
                TestAddNeurons.netTracker.setGenRating(net, entry.getValue());
            }

            System.out.println();
        }
    }
}
