package neuralNet.evolve;

import game2048.*;
import neuralNet.network.*;
import neuralNet.neuron.*;
import neuralNet.test.*;
import neuralNet.util.*;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static neuralNet.util.Util.*;

public class EvolutionaryEngine {
    public static final String SAVE_DIR = "../nnts/";

    public final String filename;
    public final MutatorFactory<BoardNet, BoardInterface> mutatorFactory;

    private int generations = 2048;
    private int netsPerGeneration = 32;
    private int reproduceBest = 8;
    private int threadsCount = 6;
    private int minRandLineageRetained = 4;
    private int retestFrequency = 16; //max generations before a new fitness test is conducted on a retained/legacy net
    private long slowHashCalculationNs = BILLION_LONG / 4;
    

    private final BoardNet  edgeNet = TestBoardNet.makeEdgeNet(),
                            randNet = TestBoardNet.makeRandomNet();

    public final Predicate<BoardInterface.BoardNetFitness> RAND_NET_FITNESS_FILTER = f -> isRandLineage(f.net);

    private final List<BoardNet> randNetsToKeep = new ArrayList<>(this.minRandLineageRetained);
    
    private NetTracker.KeepLambda<BoardNet> keepEdgeAndRand = new NetTracker.KeepIncluding<>(List.of(edgeNet, randNet));

    public final long RAND_HASH = this.randNet.getNeuralHash();



    private final List<WorkerThread> threads = new ArrayList<>(this.threadsCount);
    private final Thread mainThread = Thread.currentThread();

    private Set<BoardNet> currentNets = Set.of(this.edgeNet, this.randNet);
    private Set<BoardNet> fittest;
    private final Var<Iterator<Task>> currentIterator = new Var<>(Collections.emptyIterator());
    private final Set<Thread> threadsIdle = new HashSet<>();
    private TreeSet<BoardInterface.BoardNetFitness> fitnesses;
    private final NetTracker<BoardNet, BoardInterface.BoardNetFitness> netTracker = new NetTracker<>(keepEdgeAndRand);

    public final GenerationHeaderPrintStream System_out, errGen;
    public final TeePrintStream System_err, System_all;

    public EvolutionaryEngine(MutatorFactory<BoardNet, BoardInterface> mutatorFactory) {
        this.mutatorFactory = mutatorFactory;
        this.filename = mutatorFactory.getClass().getSimpleName() + " "
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH_mm_ss"));


        PrintStream outFile;
        PrintStream errFile;
        try {
            outFile = new PrintStream(new FileOutputStream(this.filename + ".out.txt"));
            errFile = new PrintStream(new FileOutputStream(this.filename + ".err.txt"));

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        System_out = new GenerationHeaderPrintStream(System_out_orig, outFile);
        errGen = new GenerationHeaderPrintStream(errFile);

        this.System_err = new TeePrintStream(System_err_orig, errGen, outFile);
        this.System_all = new TeePrintStream(System_out, errGen);
    }

    public void replaceSystemStreams() {
        System.setErr(this.System_err);
        System.setOut(this.System_out);
    }

    public static final PrintStream System_out_orig = System.out, System_err_orig = System.err;
    public static void restoreSystemStreams() {
        System.setErr(System_err_orig);
        System.setOut(System_out_orig);
    }



    private boolean exit = false;
    private boolean finished = false;

    private final Thread SHUTDOWN_HOOK = new Thread(this::shutdownHook);

    public void loadSaved(String filename) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename))) {
            Object nets = in.readObject();
            if (nets instanceof NetTracker t) {
                NetTracker<BoardNet, BoardInterface.BoardNetFitness> tracker = (NetTracker<BoardNet, BoardInterface.BoardNetFitness>)t;

                //make copies to avoid ConcurrentModificationException when iterating and removing
                Set<BoardNet> edgeNets = new HashSet<>(tracker.getHash(this.edgeNet.getNeuralHash()));
                Set<BoardNet> randNets = new HashSet<>(tracker.getHash(RAND_HASH));

                List<Long> edgeGenRatings = new ArrayList<>(edgeNets.size());
                List<Long> randGenRatings = new ArrayList<>(randNets.size());

                for (BoardNet net : edgeNets) {
                    if (net.generation != 0) {
                        System_err.println("Unexpected hash match to this.edgeNet:\n\t" + net);
                        continue;
                    }
                    edgeGenRatings.add(tracker.getGenRating(net));
                    tracker.removeFromAll(net);
                }

                for (BoardNet net : randNets) {
                    if (net.generation != 0) {
                        System_err.println("Unexpected hash match to this.randNet:\n\t" + net);
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

                tracker.setGenRating(this.edgeNet, edgeRating);
                tracker.setGenRating(this.randNet, randRating);

                netTracker.copyFrom(tracker);

                currentNets = netTracker.removeSpecialNets("currentNets");
                if (currentNets == null) currentNets = tracker;
                else currentNets.addAll(tracker);

            } else throw new UnsupportedOperationException("Unrecognized deserialized format : " + nets.getClass());

        }

        System_out.println("Successfully loaded saved file '" + filename + "'");
    }

    public void run() throws FileNotFoundException {
        if (Thread.currentThread() != this.mainThread) throw new IllegalStateException();

        Runtime.getRuntime().addShutdownHook(SHUTDOWN_HOOK);

        this.netTracker.add(this.edgeNet);
        this.netTracker.add(this.randNet);

        for (int i = 0; i < this.threadsCount; i++) {
            new WorkerThread().start();
        }

        fittest = currentNets;
        boolean incrementGeneration = fittest.size() < this.netsPerGeneration;
        long i = NeuralNet.getCurrentGeneration(), end = i + this.generations;
        for (;i <= end && !this.exit; i++) {

            if (incrementGeneration) i = NeuralNet.nextGeneration(); //only increment to nextGeneration if i < end, so the Shutdown hook has correct generations
            else incrementGeneration = true; //for the first iteration only

            iteration(i);
        }

        netTracker.cullOld();

        System_out.println("MAIN THREAD HAS FINISHED CRITICAL SHUTDOWN TASKS... cleaning up");
        this.finished = i >= end & !this.exit;

        synchronized (currentIterator) {
            currentNets = fittest;
            this.exit = true;
            currentIterator.notifyAll();
        }
    }

    private void iteration(long gen) {
        boolean notFirstIteration = fitnesses != null; //skip on the first time around

        if (notFirstIteration) System_out.printGenerationHeader();
        // let the work threads do it on the first iteration
        // so the error message from AffinityLock library is not under the genHeader

        this.fitnesses = new TreeSet<>();
        makeMutations(notFirstIteration);

        if (notFirstIteration) processLegacies(gen);
        while (runRound()) { }
        waitForWorkerThreads();
        if (this.exit) return;

        fittest = netTracker.addFittest(fitnesses, this.reproduceBest);
        checkForRandNets();

        this.mutatorFactory.newFitnessResults(this.fitnesses.stream()
                .filter(fitness -> netTracker.contains(fitness.getDecisionProvider())
                                    || fittest.contains(fitness.getDecisionProvider()))
                .collect(Collectors.toList()));
    }

    private void makeMutations(boolean notFirstIteration) {
        Var<Map<BoardNet, Double>> newGen = new Var<>(new LinkedHashMap<>(this.netsPerGeneration + netTracker.size()));

        int count = this.netsPerGeneration - fittest.size();
        Collection<? extends Mutator<? extends BoardNet>> mutators
                = mutatorFactory.makeMutators(count, this.fittest, this.netTracker.getFitnesses());

        if (notFirstIteration) {
            for (BoardNet net : this.fittest) {
                newGen.value.put(net, this.mutatorFactory.estimatedFitnessTestTime(net));
            }

        } else {
            for (BoardNet net : this.fittest) {
                newGen.value.put(net, 0.0);
            }
        }

        List<Task> tasks = new ArrayList<>(mutators.size());
        Var.Int mutatorsFinished = new Var.Int();

        int n = 0;
        for (Mutator<? extends BoardNet> mutator : mutators) {
            Task task = new Task(mutator.estimatedMakeMutantsTime(), worker -> {
                try {
                    worker.makeMutations(mutator, newGen);

                } finally {
                    synchronized (mutatorsFinished) {
                        mutatorsFinished.value++;
                        if (mutatorsFinished.value >= tasks.size()) mutatorsFinished.notifyAll();
                    }
                }
            });
            tasks.add(task);
        }

        this.addTasks(tasks);

        while (runRound()) { }

        synchronized (mutatorsFinished) {
            if (mutatorsFinished.value >= tasks.size()) {
                this.addFitnessTestTasks(newGen.value);
                this.currentNets = new LinkedHashSet<>(newGen.value.keySet());
                System_err_orig.println("Mutators finished together");
                return;
            }
        }

        System_err_orig.println("Mutators finished asynchronously");

        Map<BoardNet, Double> batch1;
        synchronized (newGen) {
            batch1 = newGen.value;
            newGen.value = new LinkedHashMap<>(this.netsPerGeneration + netTracker.size() - batch1.size());
        }

        this.addFitnessTestTasks(batch1);
        this.currentNets = new LinkedHashSet<>(batch1.keySet());

        while (true) {
            synchronized (mutatorsFinished) {
                if (mutatorsFinished.value >= tasks.size()) break;

                try {
                    mutatorsFinished.wait();

                } catch (InterruptedException e) {
                    if (this.exit) return;
                    e.printStackTrace(System_err);
                }
            }
        }

        this.addFitnessTestTasks(newGen.value);
        this.currentNets.addAll(newGen.value.keySet());
    }

    private void addTasks(Collection<Task> tasks) {
        synchronized (currentIterator) {
            TreeSet<Task> sorted = new TreeSet<>(tasks);
            while (currentIterator.value.hasNext()) {
                sorted.add(currentIterator.value.next());
            }
            currentIterator.value = sorted.iterator();
            currentIterator.notifyAll();
        }
    }

    private void addFitnessTestTasks(Map<BoardNet, Double> nets) {
        List<Task> tasks = new ArrayList<>(nets.size());
        for (Map.Entry<BoardNet, Double> entry : nets.entrySet()) {
            BoardNet net = entry.getKey();
            tasks.add(new Task(entry.getValue(), worker -> worker.runFitnessTest(net)));
        }

        this.addTasks(tasks);
    }

    private void addFitnessTestTasks(Set<BoardNet> legacyNets) {
        List<Task> tasks = new ArrayList<>(legacyNets.size());
        for (BoardNet net : legacyNets) {
            tasks.add(new Task(this.mutatorFactory.estimatedFitnessTestTime(net), worker -> worker.runFitnessTest(net)));
        }

        this.addTasks(tasks);
    }

    private final WorkerThread mainsWorker = new WorkerThread();

    private boolean runRound() {
        if (this.exit) return false;
        Task task;
        synchronized (currentIterator) {
            if (this.exit || !currentIterator.value.hasNext()) return false;
            task = currentIterator.value.next();
            currentIterator.notifyAll();
        }

        task.task.accept(this.mainsWorker);
        return true;

    }

    private void waitForWorkerThreads() {
        synchronized (threadsIdle) {
            while (threadsIdle.size() < this.threadsCount) {
                try {
                    threadsIdle.wait();

                } catch (InterruptedException e) {
                    e.printStackTrace(System_err);
                }
            }
        }
    }

    private boolean isRandLineage(BoardNet net) {
        return net.getLineage().lineageContains(RAND_HASH) > 0.5;
    }

    private void processLegacies(long gen) {
        netTracker.cullOld(gen - 1);

        Set<BoardNet> legaciesToCheck = new LinkedHashSet<>();

        // The worst fitnesses are allowed to attempt again, rather than using their saved
        // fitnesses for the next ~16 rounds

        int worstCount = 0;
        long genPlusRetest = gen + this.retestFrequency;

        for (Iterator<BoardInterface.BoardNetFitness> iterator = netTracker.getFitnesses().descendingIterator();
             iterator.hasNext();) {

            BoardInterface.BoardNetFitness fitness = iterator.next();
            BoardNet net = fitness.net;
            if (net == null) throw new NullPointerException();

            if (worstCount < this.reproduceBest) {
                if (!currentNets.contains(net)) {
                    worstCount++;
                    legaciesToCheck.add(net);
                }

            } else if (gen - fitness.generation > this.retestFrequency) {
                if (!currentNets.contains(net)) legaciesToCheck.add(net);

            } else if (!keepEdgeAndRand.keep(genPlusRetest, netTracker.getGenRating(fitness.net), fitness.net)) {
                if (!currentNets.contains(net)) legaciesToCheck.add(net);
            }

        }


        if (legaciesToCheck.size() != 0) this.addFitnessTestTasks(legaciesToCheck);

        this.currentNets.addAll(legaciesToCheck); //wait till after filterHashes is done AND the currentNets iterator
        // is finished so we don't get a ConcurrentModificationException
    }

    private void checkForRandNets() {
        randNetsToKeep.clear();

        Iterator<BoardInterface.BoardNetFitness> oldF = netTracker.getFitnesses().stream().filter(RAND_NET_FITNESS_FILTER).iterator();
        Iterator<BoardInterface.BoardNetFitness> newF = fitnesses.stream().filter(RAND_NET_FITNESS_FILTER).iterator();

        BoardInterface.BoardNetFitness bestRand = null;
        BoardInterface.BoardNetFitness next = null;

        while (randNetsToKeep.size() < this.minRandLineageRetained) {
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
                    while (randNetsToKeep.size() < this.minRandLineageRetained && newF.hasNext()) {
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
            fittest.add(this.randNet);
        }
    }

    private class Task implements Comparable<Task> {
        private final double expectedTime;
        private final Consumer<WorkerThread> task;

        private Task(double expectedTime, Consumer<WorkerThread> task) {
            this.expectedTime = expectedTime;
            this.task = task;
        }

        public int compareTo(Task other) throws CannotResolveComparisonException {
            if (other == this) return 0;
            else if (other == null) throw new NullPointerException();
            if (this.expectedTime != other.expectedTime) {
                return Double.compare(other.expectedTime, this.expectedTime);
            }

            if (this.hashCode() != other.hashCode()) {
                return Integer.compare(this.hashCode(), other.hashCode());
            }

            if (this.task.hashCode() != other.task.hashCode()) {
                return Integer.compare(this.task.hashCode(), other.task.hashCode());
            }

            throw new CannotResolveComparisonException(this, other);
        }
    }

    private class WorkerThread extends Thread {

        private BoardInterface board = new BoardInterface();

        private WorkerThread() {
            threads.add(this);
        }

        public void run() {
            try (UniqueAffinityLock af = UniqueAffinityLock.obtain()) {
                // force the initial generation headers to print (especially in the error file)
                // AFTER the error message from AffinityLock library has already gone through System_err
                System_out.startGenHeaders();
                System_out.printGenerationHeader();
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
            Task task;
            while(true) {
                task = getNext();

                if (task == null) break;
                else task.task.accept(this);
            }
        }


        private Task getNext() {
            Thread current = Thread.currentThread(); //should be this
            assert current == this;

            boolean addedToThreadsIdle = false;
            try {
                synchronized (currentIterator) {
                    if (EvolutionaryEngine.this.exit) return null;
                    while (currentIterator.value == null || !currentIterator.value.hasNext()) {
                        synchronized (threadsIdle) {
                            addedToThreadsIdle = true;
                            threadsIdle.add(current);
                            if (threadsIdle.size() >= EvolutionaryEngine.this.threadsCount) {
                                threadsIdle.notifyAll(); //tell the main thread that worker threads are done
                            }
                        }
                        try {
                            currentIterator.wait();

                        } catch (InterruptedException e) {
                            System_err.println("Worker tread, Id: " + current.getId());
                            e.printStackTrace(System_err);
                        }
                        if (EvolutionaryEngine.this.exit) return null;
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

        private void traceNeuronsAndCalcHash(BoardNet net) {
            net.traceNeuronsSet();
            long start = System.nanoTime();
            long hash = net.getNeuralHash();
            long end = System.nanoTime();
            if (end - start >= EvolutionaryEngine.this.slowHashCalculationNs) {
                System_err.println("SLOW HASH (" + ((double) (end - start) / BILLION) + "seconds) : " + NeuralHash.toHex(hash));
                synchronized (netTracker) {
                    netTracker.addToSpecialNets("Slow Hashes", net);
                }
            }
        }

        private void runFitnessTest(BoardNet net) {
            BoardInterface.BoardNetFitness fitness = this.board.testFitness(net, null);
            System_out.println(fitness + "\n");

            synchronized (threadsIdle) {
                fitnesses.add(fitness);
            }
        }

        private void makeMutations(Mutator<? extends BoardNet> mutator,
                                   Var<Map<BoardNet, Double>> newGen) {

            Double testTime = mutator.estimatedFitnessTestTime();
            List<? extends BoardNet> nets = mutator.makeMutants();

            for (BoardNet net : nets) {
                this.traceNeuronsAndCalcHash(net);
                synchronized (newGen) {
                    newGen.value.put(net, testTime);
                }
            }
        }
    }


    /**
     * Shutdown hook methods below
     *
     */
    private void shutdownHook() {
        String filename = this.filename + " (gen " + NeuralNet.getCurrentGeneration() + ").nnt";

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        if (!EvolutionaryEngine.this.finished) {
            waitForThreadsToExit(reader);
            if (!askToSave(filename, reader)) {
                System_all.println("Not saving...\nBYE");
                return;
            }

            try {
                netTracker.addToSpecialNets("currentNets", currentNets);

            } catch (Exception e) {
                System_err.println("Exception while trying to add the currentNets to the netsTracker ... proceeding to save without this ....");
                e.printStackTrace(System_err);
            }
        }

        saveNetsTracker(filename, reader);

        System_all.print("BYE");
    }

    private void waitForThreadsToExit(BufferedReader reader) {
        this.exit = true;
        System_err.println("\n*****************************************************************************\n\n"
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

                System_err.println("Skip waiting request detected... attempting to print stack trace of all threads first...\n");

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
                    System_err.println(entry.getKey());
                    for (StackTraceElement element : entry.getValue()) {
                        System_err.println(element);
                    }
                    System_err.println();
                }

                done.value = true;
                current.interrupt();

                return;

            } catch (IOException e) {
                e.printStackTrace(System_err);

            } catch (InterruptedException e) {
                if (!done.value) {
                    e.printStackTrace(System_err);
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
                //catch (InterruptedException e) { e.printStackTrace(System_err); }
            }
            //}
        }).start();

        new Thread(() -> {
            //while (!done.value) {
            synchronized (threadsIdle) {
                threadsIdle.notifyAll();
                //try { threadsIdle.wait(1000); }
                //catch (InterruptedException e) { e.printStackTrace(System_err); }
            }
            //}
        }).start();


        for (WorkerThread thread : threads) {
            while (!done.value && thread.isAlive()) {
                thread.interrupt();
                try {
                    thread.join(1000);

                } catch (InterruptedException e) {
                    if (!done.value) e.printStackTrace(System_err);
                }
            }
        }

        while (!done.value && mainThread.isAlive()) try {
            mainThread.join(1000);

        } catch (InterruptedException e) {
            if (!done.value) e.printStackTrace(System_err);
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

    private boolean askToSave(String filename, BufferedReader reader) {
        long lastErr = 0;
        try {
            while (reader.ready()) reader.read(); //clear input stream

        } catch(IOException e) {
            e.printStackTrace(System_err);
        }


        while (true) {
            try {
                System_out.println("Save results as '" + filename + "'\n\tenter 'y' or 'n'");

                String in = reader.readLine().toLowerCase();
                if (in.equals("n")) return false;
                else if (in.equals("y")) return true;

                System_err.println("Invalid input");

            } catch (IOException e) {
                e.printStackTrace(System_err);
                if (System.currentTimeMillis() - lastErr < 1000) {
                    System_err.println("Repeated input errors... saving as " + filename);
                    return true;
                }
                lastErr = System.currentTimeMillis();
            }
        }
    }

    private void saveNetsTracker(String filename, BufferedReader reader) {

        while (true) try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(SAVE_DIR + filename))) {
            System_all.println("SAVING AS " + filename);
            out.writeObject(netTracker);
            break;

        } catch (IOException e) {
            System_err.println("Exception while serializing/saving netsTracker");
            e.printStackTrace(System_err);

            while (true) try {
                System_err.println("ATTEMPT AGAIN?  (enter 'y' or 'n')");
                String in = reader.readLine().toLowerCase();

                if ("y".equals(in)) break;
                else if ("n".equals(in)) return;

                System_err.println("Invalid input: " + in);

            } catch (IOException e2) {
                System_err.println("Exception while trying to read user input... exiting");
                e2.printStackTrace(System_err);
                return;
            }
        }

        System_out.print("Successfully wrote file to archive. Copying to working directory... ");

        try {
            Files.copy(new File(SAVE_DIR + filename).toPath(),
                        new File(filename).toPath(),
                        StandardCopyOption.COPY_ATTRIBUTES);

        } catch (IOException e) {
            System_out.println();
            System_err.println("Exception while copying output file to working directory");
            e.printStackTrace(System_err);
            return;
        }

        System_out.println("success");
    }

    public int getGenerations() {
        return this.generations;
    }

    public void setGenerations(int generations) {
        this.generations = generations;
    }

    public int getNetsPerGeneration() {
        return this.netsPerGeneration;
    }

    public void setNetsPerGeneration(int netsPerGeneration) {
        this.netsPerGeneration = netsPerGeneration;
    }

    public int getReproduceBest() {
        return this.reproduceBest;
    }

    public void setReproduceBest(int reproduceBest) {
        this.reproduceBest = reproduceBest;
    }

    public int getThreadsCount() {
        return this.threadsCount;
    }

    public void setThreadsCount(int threadsCount) {
        this.threadsCount = threadsCount;
    }

    public int getMinRandLineageRetained() {
        return this.minRandLineageRetained;
    }

    public void setMinRandLineageRetained(int minRandLineageRetained) {
        this.minRandLineageRetained = minRandLineageRetained;
    }

    public int getRetestFrequency() {
        return this.retestFrequency;
    }

    public void setRetestFrequency(int retestFrequency) {
        this.retestFrequency = retestFrequency;
    }

    public long getSlowHashCalculationNs() {
        return this.slowHashCalculationNs;
    }

    public void setSlowHashCalculationNs(long slowHashCalculationNs) {
        this.slowHashCalculationNs = slowHashCalculationNs;
    }
}
