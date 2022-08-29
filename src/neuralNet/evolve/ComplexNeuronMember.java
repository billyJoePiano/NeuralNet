package neuralNet.evolve;

import neuralNet.network.*;
import neuralNet.neuron.*;

import java.util.*;

public interface ComplexNeuronMember extends Neuron, Sensable<ComplexNeuronMember>, DecisionConsumer<ComplexNeuronMember, ComplexNeuronMember, ComplexNeuronMember.Fitness> {
    ComplexNeuronMember getPrimaryNeuron();
    List<ComplexNeuronMember> getMembers();
    DecisionProvider<ComplexNeuronMember, ? , ComplexNeuronMember> getInternalDecisionProvider();


    @Override
    default public boolean takeAction(int decisionId) throws IllegalArgumentException {
        throw new UnsupportedOperationException();
    }

    @Override
    default public Fitness testFitness(DecisionProvider<ComplexNeuronMember, ?, ComplexNeuronMember> decisionProvider, List<ComplexNeuronMember> usingInputs) {
        throw new UnsupportedOperationException();
    }


    public static class Fitness implements neuralNet.network.Fitness<ComplexNeuronMember, Fitness> {
        private Fitness() {
            throw new UnsupportedOperationException();
        }

        @Override
        public short getSignal() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int compareTo(Fitness other) {
            throw new UnsupportedOperationException();
        }
    }
}
