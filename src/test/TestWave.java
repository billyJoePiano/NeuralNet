package test;

import neuron.*;
import util.*;

import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.stream.*;

import static neuron.StatelessFunction.*;

// Output can be copied into Excel spreadsheet for graphing
public class TestWave {
    public static final double PERIOD = Math.sqrt(5) / 3;
    public static final double PHASE = -0.0625;
    public static final int ROUNDS = -1; //(int)Math.abs(PERIOD * 1.125);

    public static final Neuron sine = new StaticSineWave(PERIOD, PHASE);
    public static final Neuron triangle = new StaticTriangleWave(PERIOD, PHASE);
    public static final Neuron saw = new StaticSawWave(PERIOD, PHASE);
    public static final Neuron square = new StaticSquareWave(PERIOD, PHASE);

    public static final Neuron phase = makePhaseNeuron();
    public static final Neuron period = FixedValue.makeNeuron(Short.MAX_VALUE);

    public static final Neuron sineVar = new VariableSineWave(period, phase, PERIOD / 4, PERIOD);
    public static final Neuron triangleVar = new VariableTriangleWave(period, phase, PERIOD / 4, PERIOD);
    public static final Neuron sawVar = new VariableSawWave(period, phase, PERIOD / 4, PERIOD);
    public static final Neuron squareVar = new VariableSquareWave(period, phase, PERIOD / 4, PERIOD);


    private static Neuron makePhaseNeuron() {
        double phase;
        if (PHASE < 1) phase = PHASE;
        else phase = PHASE - 2.0;
        phase = Math.round(phase * MAX_PLUS_ONE - 0.5);

        return FixedValue.makeNeuron((short)phase);
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

            tester.runRounds(ROUNDS, charSeq -> writer.print(charSeq.toString() + "\n"));

        /*
            System.out.println("Wrote test output to file " + filename);

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

    public synchronized void runRounds(int numberOfRounds) {
        this.runRounds(numberOfRounds, System.out::println);
    }

    public synchronized void runRounds(int numberOfRounds, WriteLambda writer) {
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

        for (int i = 0; i != numberOfRounds; i++) {
            trackers.forEach(tracker -> tracker.finishedCurrentRound = false);
            this.allNeurons.forEach(Neuron::before);

            this.newPeriod = false;
            //boolean newPeriod = false;

            StringBuffer buffer = new StringBuffer();
            int newPeriodCount = 0;

            for (NeuronTracker tracker : trackers) {
                short output = tracker.checkOutput();

                if (this.newPeriod) {
                    newPeriodCount++;
                    this.newPeriod = false;
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


            if (newPeriodCount > 0) {
                //writer.writeln("New period for " + newPeriodCount + " wave neurons");
                if (newPeriodCount < trackers.size()) {
                    // means some of the waves are out of phase with each other
                    System.err.println("OUT OF PHASE!");
                    System.err.flush();

                    boolean breakpoint = true;

                } else {
                    System.out.println("Start of new period");
                    System.out.flush();
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

        private boolean sign;
        private boolean crossedOnce = false;
        private int rounds = 0;
        private int periods = 0;
        private final LinkedList<Integer> roundsPerPeriod = new LinkedList<>();
        private boolean finishedCurrentRound = false;

        public NeuronTracker(Neuron waveNeuron) {
            this.neuron = waveNeuron;
            this.trackerMap = null;
        }

        public NeuronTracker(Neuron waveNeuron, Map<Neuron, NeuronTracker> trackerMap) {
            this.neuron = waveNeuron;
            this.trackerMap = trackerMap;
            trackerMap.put(neuron, this);
        }

        // should only be invoked when this.trackerMap != null and TestWave.this.pairs != null
        private void checkForPairedWith() {
            Set<Neuron> paired = TestWave.this.pairs.get(this.neuron);
            if (paired == null) return;

            this.pairedWith = new HashSet<>();

            for (Neuron neuron : paired) {
                this.pairedWith.add(this.trackerMap.get(neuron));
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

                        //For breakpoint debugging ... clear the cache first
                        this.neuron.before();
                        other.neuron.before();

                        //place breakpoints here
                        this.neuron.getOutput();
                        other.neuron.getOutput();

                        System.err.println("Inconsistent output: "
                                + other.neuron.getClass().getSimpleName() + " " + otherOutput + " \t "
                                + this.neuron.getClass().getSimpleName() + " " + output);
                    }
                }
            }

            return output;
        }

        private void crossed() {
            if (this.crossedOnce) {
                this.periods++;
                this.roundsPerPeriod.add(this.rounds);
                this.rounds = 0;
                this.crossedOnce = false;
                TestWave.this.newPeriod = true;

            } else {
                this.crossedOnce = true;
            }
        }
    }
}
