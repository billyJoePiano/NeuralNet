package neuralNet.test;

import neuralNet.function.*;
import neuralNet.neuron.*;
import neuralNet.util.*;

import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.stream.*;

import static neuralNet.function.StatelessFunction.*;

// Output can be copied into Excel spreadsheet for graphing
public class TestWave {
    public static final double PERIOD = 14347907;
    public static final double PHASE = Math.sqrt(2);
    public static final int ROUNDS = -1; //(int)Math.abs(PERIOD * 1.125);

    public static final FixedValueNeuron phase = makePhaseNeuron();
    public static final FixedValueNeuron period = new FixedValueNeuron(Short.MAX_VALUE);

    public static final double PHASE_ROUNDED = (double)phase.getOutput() / MAX_PLUS_ONE;

    public static final Neuron sine = SineWave.makeNeuron(PERIOD, PHASE_ROUNDED);
    public static final Neuron triangle = TriangleWave.makeNeuron(PERIOD, PHASE_ROUNDED);
    public static final Neuron saw = SawWave.makeNeuron(PERIOD, PHASE_ROUNDED);
    public static final Neuron square = SquareWave.makeNeuron(PERIOD, PHASE_ROUNDED);

    public static final Neuron sineVar = SineWave.makeNeuron(period, phase, PERIOD / 4, PERIOD);
    public static final Neuron triangleVar = TriangleWave.makeNeuron(period, phase, PERIOD / 4, PERIOD);
    public static final Neuron sawVar = SawWave.makeNeuron(period, phase, PERIOD / 4, PERIOD);
    public static final Neuron squareVar = SquareWave.makeNeuron(period, phase, PERIOD / 4, PERIOD);


    private static FixedValueNeuron makePhaseNeuron() {
        double phase;
        if (PHASE < 1) phase = PHASE;
        else phase = PHASE - 2.0;
        phase = Math.round(phase * MAX_PLUS_ONE);

        return new FixedValueNeuron((short)phase);
    }



    public static void main (String[] args) {

        /*
        TestWave tester = new TestWave(List.of(sine, triangle, saw, square));

        TestWave tester = new TestWave(List.of(sineVar, triangleVar, sawVar, squareVar));

        TestWave tester = new TestWave(List.of(sine, sineVar, triangle, triangleVar, saw, sawVar, square, squareVar));
        tester.registerPair(sine, sineVar);
        tester.registerPair(triangle, triangleVar);
        tester.registerPair(saw, sawVar);
        tester.registerPair(square, squareVar);
         */

        TestWave tester = new TestWave(List.of(sine, sineVar, triangle, triangleVar, saw, sawVar, square, squareVar));
        tester.registerPair(sine, sineVar);
        tester.registerPair(triangle, triangleVar);
        tester.registerPair(saw, sawVar);
        tester.registerPair(square, squareVar);

        String filename = "TestWave "
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH_mm_ss"))
                + ".txt";

        PrintStream writer = System.out;

        //try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.println("PERIOD: " + PERIOD + "\tPHASE: " + PHASE + "\n\n");

            StringBuffer sb = new StringBuffer();

            for (Neuron neuron : tester.waveNeurons) {
                sb.append(neuron.getClass().getSimpleName() + "\t");
            }
            sb.setCharAt(sb.length() - 1, '\n');
            sb.append('\n');

            writer.println(sb.toString());

            Var.Long counter = new Var.Long();

            tester.runRounds(ROUNDS, charSeq -> {
                if (++counter.value % 1_000_000 == 0) writer.println("ROUND # " + String.format("%,d",counter.value));
                //writer.print(charSeq.toString() + "\n")
            });

        /*
            System.out.println("Wrote neuralNet.test output to file " + filename);

        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }


    private final List<Neuron> waveNeurons;
    private final Set<Neuron> allNeurons; // may include neurons which the wave neurons depend upon
    private Map<Neuron, Set<Neuron>> pairs;

    public TestWave(List<Neuron> neurons) {
        this.waveNeurons = new ArrayList<>(neurons);
        this.allNeurons = new HashSet<>(neurons);
    }

    private boolean newPeriod;
    private int outOfPhase;

    public synchronized void runRounds(int numberOfRounds) {
        this.runRounds(numberOfRounds, System.out::println);
    }

    public synchronized void runRounds(int numberOfRounds, WriteLambda writer) {
        /*try (AffinityLock al = AffinityLock.acquireCore()) {
            this.runRoundsWithAffinityLock(numberOfRounds, writer);
        }
    }

    private synchronized void runRoundsWithAffinityLock(int numberOfRounds, WriteLambda writer) {*/
        List<NeuronTracker> trackers;

        if (this.pairs != null) {
            Map<Neuron, NeuronTracker> trackerMap = new HashMap<>(this.waveNeurons.size());
            trackers = waveNeurons.stream()
                    .map(neuron -> new NeuronTracker(neuron, trackerMap))
                    .collect(Collectors.toList());

            trackers.forEach(NeuronTracker::checkForPairedWith);

        } else {
            trackers = waveNeurons.stream()
                    .map(NeuronTracker::new)
                    .collect(Collectors.toList());
        }

        Set<NeuronTracker> newPeriodTrackers = new HashSet<>();

        for (int i = 0; i != numberOfRounds; i++) {
            trackers.forEach(tracker -> tracker.finishedCurrentRound = false);
            this.allNeurons.forEach(Neuron::before);

            this.newPeriod = false;
            //boolean newPeriod = false;

            StringBuffer buffer = new StringBuffer();
            //int newPeriodCount = 0;

            for (NeuronTracker tracker : trackers) {
                short output = tracker.checkOutput();

                if (this.newPeriod) {
                    //newPeriodCount++;
                    this.newPeriod = false;
                    newPeriodTrackers.add(tracker);
                }

                /*
                if (this.newPeriod) {
                    this.newPeriod = false;
                    newPeriod = true;

                    buffer.append('[').append(output).append(']');

                    if (output < 10 && output > -10) {
                        buffer.append('\t');
                    }

                } else {
                    buffer.append(output);
                    if (output < 1000 && output > -100) {
                        buffer.append('\t');
                    }
                }
                 */

                buffer.append(output);

                buffer.append('\t');
            }


            if (newPeriodTrackers.size() > 0) {
                //writer.writeln("New period for " + newPeriodCount + " wave neurons");
                if (newPeriodTrackers.size() < trackers.size()) {
                    // means some of the waves are out of phase with each other
                    Set<NeuronTracker> notNewPeriod = new HashSet<>(trackers);
                    notNewPeriod.removeAll(newPeriodTrackers);

                    newPeriodTrackers.forEach(newPeriod ->
                            notNewPeriod.forEach(notNew ->
                                    newPeriod.outOfPhase.put(notNew.neuron,
                                        newPeriod.outOfPhase.getOrDefault(notNew.neuron, 0) + 1)));

                    notNewPeriod.forEach(notNew ->
                            newPeriodTrackers.forEach(newPeriod ->
                                    notNew.outOfPhase.put(newPeriod.neuron,
                                        notNew.outOfPhase.getOrDefault(newPeriod.neuron, 0) + 1)));

                    //System.err.println("OUT OF PHASE!");
                    //System.err.flush();

                    boolean breakpoint = true;

                } else {
                    //System.out.println("Start of new period");
                    //System.out.flush();
                }
            }

            while (buffer.charAt(buffer.length() - 1) == '\t') {
                buffer.deleteCharAt(buffer.length() - 1);
            }

            try {
                writer.writeln(buffer);

            } catch (IOException e) {
                e.printStackTrace();
            }

            System.err.flush();
            this.allNeurons.forEach(Neuron::after);
            newPeriodTrackers.clear();
        }

        System.out.println("\n\n\n\t\tFINAL RESULT\n");

        for (NeuronTracker tracker : trackers) {
            System.out.println(tracker + "\n");
        }

    }

    public synchronized void registerDependencies(List<Neuron> neurons) {
        this.allNeurons.addAll(neurons);
    }
    public synchronized void registerDependencies(Neuron ... neurons) {
        this.allNeurons.addAll(Arrays.asList(neurons));
    }

    /**
     * Pairing two neurons means the tests will check that the pair always produces the same output,
     * writing a message to stderr when they don't.
     *
     * @param n1
     * @param n2
     */
    public synchronized void registerPair(Neuron n1, Neuron n2) throws IllegalArgumentException {
        if (!(this.waveNeurons.contains(n1) && this.waveNeurons.contains(n2))) {
            throw new IllegalArgumentException();
        }

        if (this.pairs == null) this.pairs = new HashMap<>();

        this.pairs.computeIfAbsent(n1, k -> new HashSet<>()).add(n2);
        this.pairs.computeIfAbsent(n2, k -> new HashSet<>()).add(n1);
    }


    public class NeuronTracker {
        private final Neuron neuron;
        private final Map<Neuron, NeuronTracker> trackerMap;
        private Set<NeuronTracker> pairedWith;
        private Map<Neuron, Integer> offByOne;
        private Map<Neuron, Integer> offByMoreThanOne;
        private final Map<Neuron, Integer> outOfPhase = new HashMap<>();


        private boolean sign;
        private boolean crossedOnce = false;
        private int rounds = 0;
        private int periods = 0;
        private final LinkedList<Double> roundsPerPeriod = new LinkedList<>();
        private boolean finishedCurrentRound = false;

        public NeuronTracker(Neuron waveNeuron) {
            this.neuron = waveNeuron;
            this.trackerMap = null;
        }

        public NeuronTracker(Neuron waveNeuron, Map<Neuron, NeuronTracker> trackerMap) {
            this.neuron = waveNeuron;
            this.trackerMap = trackerMap;
            trackerMap.put(this.neuron, this);
        }

        // should only be invoked when this.trackerMap != null and TestWave.this.pairs != null
        private void checkForPairedWith() {
            Set<Neuron> paired = TestWave.this.pairs.get(this.neuron);
            if (paired == null) return;

            this.pairedWith = new HashSet<>();
            this.offByOne = new HashMap<>();
            this.offByMoreThanOne = new HashMap<>();

            for (Neuron neuron : paired) {
                this.pairedWith.add(this.trackerMap.get(neuron));
                this.offByOne.put(neuron, 0);
                this.offByMoreThanOne.put(neuron, 0);
            }
        }

        private short checkOutput() {
            this.rounds++;

            short output = this.neuron.getOutput();

            if (this.periods == 0 & this.rounds == 1) {
                this.sign = output >= 0;

            } else if (output < 0) {
                if (this.sign) {
                    this.crossed();
                    this.sign = false;
                }

            } else {
                if (!this.sign) {
                    this.crossed();
                    this.sign = true;
                }
            }

            this.finishedCurrentRound = true;
            if (this.pairedWith != null) {
                for (NeuronTracker other : pairedWith) {
                    if (!other.finishedCurrentRound) continue;
                    short otherOutput = other.neuron.getOutput();

                    if (otherOutput != output) {

                        if (Math.abs(output - otherOutput) == 1) {
                            int count = this.offByOne.get(other.neuron) + 1;
                            this.offByOne.put(other.neuron, count);
                            other.offByOne.put(this.neuron, count);

                        } else {
                            int count = this.offByMoreThanOne.get(other.neuron) + 1;
                            this.offByMoreThanOne.put(other.neuron, count);
                            other.offByMoreThanOne.put(this.neuron, count);

                            //For breakpoint debugging ... clear the cache first
                            this.neuron.before();
                            other.neuron.before();

                            //place breakpoints here
                            this.neuron.getOutput();
                            other.neuron.getOutput();

                            /*
                            System.err.println("Inconsistent output: "
                                    + other.neuralNet.neuron.getClass().getSimpleName() + " " + otherOutput + " \t "
                                    + this.neuralNet.neuron.getClass().getSimpleName() + " " + output);
                             */
                        }
                    }
                }
            }

            return output;
        }

        private void crossed() {
            if (this.crossedOnce) {
                this.periods++;
                this.crossedOnce = false;
                TestWave.this.newPeriod = true;

                Double addToAccumulated = Double.valueOf(this.rounds);
                this.rounds = 0;

                for (ListIterator<Double> iterator = this.roundsPerPeriod.listIterator();
                     iterator.hasNext(); ) {

                    Double next = iterator.next();

                    if (next == null) {
                        if (addToAccumulated != null) {
                            iterator.set(addToAccumulated);
                            addToAccumulated = null;
                        }

                    } else if (addToAccumulated != null) {
                        addToAccumulated = (next + addToAccumulated) / 2;
                        iterator.set(null);
                    }
                }

                if (addToAccumulated != null) {
                    this.roundsPerPeriod.add(addToAccumulated);
                }


            } else {
                this.crossedOnce = true;
            }
        }

        public String toString() {
            double roundsPerPeriod = 0;
            double weightSum = 0;
            double currentWeight = 1;
            for (Double rounds : this.roundsPerPeriod) {
                if (rounds != null) {
                    roundsPerPeriod += rounds * currentWeight;
                    weightSum += currentWeight;
                }
                currentWeight *= 2;
            }

            roundsPerPeriod /= weightSum;

            return this.neuron.toString()
                    + "\n\t              Total periods: " + this.periods
                    + "\n\t      Average rounds/period: " + roundsPerPeriod
                    + "\n\t               Out of phase: " + this.outOfPhase
                    + "\n\t          Rounds off by one: " + this.offByOne
                    + "\n\tRounds off by more than one: " + this.offByMoreThanOne;
        }
    }
}
