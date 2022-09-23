package neuralNet.evolve;

import neuralNet.network.*;
import neuralNet.neuron.*;
import neuralNet.util.*;

import java.util.*;

import static neuralNet.util.Util.*;

public interface ComplexNeuronMember extends Neuron, Sensable<ComplexNeuronMember>, DecisionConsumer<ComplexNeuronMember, ComplexNeuronMember, ComplexNeuronMember.Fitness> {
    public static final double MAX_WAIT_NS = 60 * BILLION;
    public static final double NS_SIGNAL_OFFSET = Short.MAX_VALUE - 1; // reserve Short.MAX_VALUE for 0 nanoseconds, since log of 0 is infinite

    /**
     * For ComplexNeuronMember.Fitness -- every order of magnitude (base e) in nanoseconds equals this much
     * signal strength.  Calculated based on provided MAX_WAIT_NS
     */
    public static final double NS_LOG_MULTIPLIER = -(NS_SIGNAL_OFFSET - Short.MIN_VALUE) / Math.log(MAX_WAIT_NS);

    ComplexNeuronMember getPrimaryNeuron();
    boolean isPrimaryNeuron();
    List<ComplexNeuronMember> getMembers();
    DecisionProvider<ComplexNeuronMember, ? , ComplexNeuronMember> getInternalDecisionProvider();



    default public Fitness testFitness() {
        if (this.getPrimaryNeuron() != this) return this.getPrimaryNeuron().testFitness();

        AccumulatedAverage avg = new AccumulatedAverage();
        List<SignalProvider> limits = List.of(FixedValueProvider.makeMin(), FixedValueProvider.makeZero(), FixedValueProvider.makeMax());

        List<SignalProvider> origInputs = new ArrayList<>(this.getInputs());
        this.clearInputs();

        int minInputs = this.getMinInputs();

        runLimitTrials(0, minInputs - 1, limits, avg);

        this.clearInputs();
        for (int i = 0 ; i < minInputs; i++) {
            this.addInput(new RandomValueProvider());
        }

        int randTrials = Math.max((int)Math.pow(3, this.getMinInputs()), 81); // 81 = 3^4
        List<SignalProvider> inputs = this.getInputs();

        long start = System.nanoTime();

        for (int i = 0; i < randTrials; i++) {
            for (SignalProvider neuron : inputs) neuron.before();
            avg.add(runTrial(i == 0, true, true));
            for (SignalProvider neuron : inputs) neuron.after();
        }

        long end = System.nanoTime();


        this.clearInputs();
        for (SignalProvider neuron : origInputs) {
            this.addInput(neuron);
        }

        return new Fitness(avg.getAverage(), end - start, this.getInternalDecisionProvider());
    }

    default public void runLimitTrials(int inputIndex, int lastIndex,
                                       List<SignalProvider> limits, AccumulatedAverage avg) {
        for (SignalProvider neuron : limits) {
            this.addInput(inputIndex, neuron);

            if (inputIndex <  lastIndex) {
                this.runLimitTrials(inputIndex + 1, lastIndex, limits, avg);

            } else {
                avg.add(runTrial(true, true, true));
            }

            this.removeInput(inputIndex);
        }
    }

    default public double runTrial(boolean reset, boolean before, boolean after) {
        List<ComplexNeuronMember> members = this.getMembers();
        if (reset) for (ComplexNeuronMember member : members) member.reset();
        if (before) for (ComplexNeuronMember member : members) member.before();

        this.getInternalDecisionProvider().sense();

        long start = System.nanoTime();
        for (ComplexNeuronMember member : members) member.getOutput();
        long end = System.nanoTime();

        if (after) for (ComplexNeuronMember member : members) member.after();

        return end - start;
    }

    @Override
    default public boolean takeAction(int decisionId) throws IllegalArgumentException {
        throw new UnsupportedOperationException();
    }

    @Override
    default public Fitness testFitness(DecisionProvider<ComplexNeuronMember, ?, ComplexNeuronMember> decisionProvider, List<ComplexNeuronMember> usingInputs) {
        throw new UnsupportedOperationException();
    }


    public static class Fitness implements neuralNet.network.Fitness<ComplexNeuronMember, Fitness> {

        public final double avgNs;
        public final long testTime;
        public final short signal;
        public final DecisionProvider<?, ?, ComplexNeuronMember> net;
        public final long generation = NeuralNet.getCurrentGeneration();

        private Fitness(double avgNs, long testTime, DecisionProvider<?, ?, ComplexNeuronMember> net) {
            if (avgNs == 0) this.signal = Short.MAX_VALUE;
            else if (!(avgNs > 0 && Double.isFinite(avgNs))) throw new IllegalArgumentException();
            else this.signal = roundClip(Math.log(avgNs) * NS_LOG_MULTIPLIER + NS_SIGNAL_OFFSET);

            if (net == null) throw new NullPointerException();

            this.avgNs = avgNs;
            this.testTime = testTime;
            this.net = net;
        }

        @Override
        public short getSignal() {
            return signal;
        }

        @Override
        public int compareTo(Fitness other) {
            if (other == this) return 0;
            if (other == null) return -1;
            if (this.avgNs > other.avgNs) return 1;
            if (this.avgNs < other.avgNs) return -1;
            if (this.avgNs == other.avgNs) return 0;
            throw new IllegalStateException();
        }

        @Override
        public DecisionProvider<?, ?, ComplexNeuronMember> getDecisionProvider() {
            return this.net;
        }

        @Override
        public long getGeneration() {
            return this.generation;
        }

        @Override
        public long getTestTime() {
            return 0;
        }
    }


}
