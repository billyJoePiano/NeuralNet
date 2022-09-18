package neuralNet.evolve;

import neuralNet.network.*;
import neuralNet.neuron.*;

import java.io.*;
import java.util.*;

public class ComplexNeuron extends CachingNeuron implements ComplexNeuronMember {

    private final int numInputs;
    private final int numOutputs;
    private transient List<SignalProvider> inputsView = this.getInputs();

    private final InternalNet net;

    private final List<InternalNet.Input> inputSensors;

    private final InternalNet.Output decisionNode0;
    private final List<ComplexNeuronMember> members;

    private transient Fitness fitness;

    protected Object readResolve() throws ObjectStreamException {
        super.readResolve();
        this.inputsView = this.getInputs();
        return this;
    }

    /*
    private ComplexNeuron(ComplexNeuron deserializedFrom, Void v) {
        super(deserializedFrom, null);
        this.numInputs = deserializedFrom.numInputs;
        this.numOutputs = deserializedFrom.numOutputs;
        this.net = new InternalNet(deserializedFrom.net, null);
        this.decisionNode0 = this.net.decisionNodes.get(0);
        this.members = this.makeOutputs();
        this.inputSensors = this.net.sensors;
        this.addConsumers(this.members);
        this.fitness = deserializedFrom.fitness;
    }
     */

    public ComplexNeuron() { this(1, 1); }

    public ComplexNeuron(int numInputs, int numOutputs) {
        super();
        if (numInputs < 1 || numOutputs < 1) throw new IllegalArgumentException();
        this.numInputs = numInputs;
        this.numOutputs = numOutputs;
        this.net = new InternalNet();
        this.decisionNode0 = this.net.decisionNodes.get(0);
        this.members = this.makeOutputs();
        this.inputSensors = this.net.sensors;
        this.addConsumers(this.members);
    }

    public ComplexNeuron(ComplexNeuron cloneFrom) {
        super(cloneFrom);
        this.numInputs = cloneFrom.numInputs;
        this.numOutputs = cloneFrom.numOutputs;
        this.net = new InternalNet(cloneFrom.net);
        this.decisionNode0 = this.net.decisionNodes.get(0);
        this.members = this.makeOutputs();
        this.inputSensors = this.net.sensors;
        this.addConsumers(this.members);
        this.fitness = cloneFrom.fitness;
    }

    public ComplexNeuron(ComplexNeuron cloneFrom, int numInputs, int numOutputs,
                         Map<SignalProvider, SignalProvider> providersMap,
                         Map<SignalConsumer, SignalConsumer> consumersMap) {
        super(cloneFrom);
        this.numInputs = numInputs;
        this.numOutputs = numOutputs;
        this.net = new InternalNet(cloneFrom.net, providersMap, consumersMap);
        this.decisionNode0 = this.net.decisionNodes.get(0);
        this.members = this.makeOutputs();
        this.inputSensors = this.net.sensors;
        this.addConsumers(this.members);
    }

    private List<ComplexNeuronMember> makeOutputs() {
        if (this.numOutputs == 1) return List.of(this);
        List<ComplexNeuronMember> outputs = new ArrayList<>(this.numOutputs);
        outputs.add(this);
        for (int i = 1; i < this.numOutputs; i++) {
            outputs.add(new SecondaryMember(this.net.decisionNodes.get(i)));
        }
        return Collections.unmodifiableList(outputs);
    }

    @Override
    protected short calcOutput() {
        return this.decisionNode0.getWeight();
    }

    @Override
    protected short calcOutput(List<SignalProvider> inputs) {
        return this.decisionNode0.getWeight();
    }

    @Override
    public ComplexNeuron clone() {
        return new ComplexNeuron(this);
    }

    @Override
    public int numSensorNodesRequired() {
        return this.numInputs;
    }

    @Override
    public int getMinInputs() {
        return this.numInputs;
    }

    @Override
    public int getMaxInputs() {
        return this.numInputs;
    }

    @Override
    public boolean inputOrderMatters() {
        return true;
    }

    @Override
    public int decisionCount() {
        return this.numOutputs;
    }

    @Override
    public int getMaxNoOpRounds() {
        return 0;
    }

    @Override
    public ComplexNeuronMember getPrimaryNeuron() {
        return this;
    }

    @Override
    public boolean isPrimaryNeuron() {
        return true;
    }

    @Override
    public List<ComplexNeuronMember> getMembers() {
        return this.members;
    }

    @Override
    public InternalNet getInternalDecisionProvider() {
        return this.net;
    }

    public class SecondaryMember extends CachingNeuron implements ComplexNeuronMember {
        private final InternalNet.Output decisionNode;

        private SecondaryMember(InternalNet.Output decisionNode) {
            super(ComplexNeuron.this.inputs, ComplexNeuron.this.inputsView);
            this.decisionNode = decisionNode;
        }

        @Override
        public int getMinInputs() {
            return ComplexNeuron.this.numInputs;
        }

        @Override
        public int getMaxInputs() {
            return ComplexNeuron.this.numInputs;
        }

        @Override
        protected short calcOutput() {
            return this.decisionNode.getWeight();
        }

        @Override
        protected short calcOutput(List<SignalProvider> inputs) {
            return this.decisionNode.getWeight();
        }

        @Override
        public CachingNeuron clone() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setInputs(List<SignalProvider> inputs) throws IllegalArgumentException {
            ComplexNeuron.this.setInputs(inputs);
        }

        @Override
        public void addInput(SignalProvider newProvider) {
            ComplexNeuron.this.addInput(newProvider);
        }

        @Override
        public void addInput(int index, SignalProvider newProvider) {
            ComplexNeuron.this.addInput(newProvider);
        }

        @Override
        public SignalProvider removeInput(int index) {
            return ComplexNeuron.this.removeInput(index);
        }

        @Override
        public boolean removeInput(SignalProvider removeAll) {
            return ComplexNeuron.this.removeInput(removeAll);
        }

        @Override
        public SignalProvider replaceInput(int index, SignalProvider newProvider) throws NullPointerException {
            return ComplexNeuron.this.replaceInput(index, newProvider);
        }

        @Override
        public boolean replaceInput(SignalProvider oldProvider, SignalProvider newProvider) throws IllegalArgumentException, NullPointerException {
            return ComplexNeuron.this.replaceInput(oldProvider, newProvider);
        }

        @Override
        public void replaceInputs(Map<SignalProvider, SignalProvider> neuronMap) throws NoSuchElementException {
            ComplexNeuron.this.replaceInputs(neuronMap);
        }

        @Override
        public void clearInputs() {
            ComplexNeuron.this.clearInputs();
        }

        @Override
        public long calcNeuralHashFor(LoopingNeuron looper) {
            return this.decisionNode.getInputs().get(0).getNeuralHashFor(looper);
        }

        @Override
        public void replaceConsumers(Map<SignalConsumer, SignalConsumer> neuronMap) {
            ComplexNeuron.this.replaceConsumers(neuronMap);
        }

        @Override
        public ComplexNeuron getPrimaryNeuron() {
            return ComplexNeuron.this;
        }

        @Override
        public boolean isPrimaryNeuron() {
            return false;
        }

        @Override
        public List<ComplexNeuronMember> getMembers() {
            return ComplexNeuron.this.members;
        }

        @Override
        public InternalNet getInternalDecisionProvider() {
            return ComplexNeuron.this.net;
        }

        /*
        @Override
        public boolean removeConsumer(SignalConsumer consumer) {
            if (ComplexNeuron.this.outputs.contains(consumer)) return false;
            return super.removeConsumer(consumer);
        }

        @Override
        public void replaceConsumers(Map<SignalConsumer, SignalConsumer> neuronMap) {
            for (Neuron output : ComplexNeuron.this.outputs) {
                if (output == ComplexNeuron.this) {
                    ((ComplexNeuron)output).replaceConsumersNoRecurse(neuronMap);
                } else {
                    ((MultiOutput)output).replaceConsumersNoRecurse(neuronMap);
                }
            }
        }

        private void replaceConsumersNoRecurse(Map<SignalConsumer, SignalConsumer> neuronMap) {
            super.replaceConsumers(neuronMap);
            this.addConsumers(ComplexNeuron.this.outputs);
        }
        */
        /*
        @Override
        public boolean replaceConsumer(SignalConsumer oldConsumer, SignalConsumer newConsumer) {
            if (ComplexNeuron.this.outputs.contains(oldConsumer)) return false;
            return super.replaceConsumer(oldConsumer, newConsumer);
        }
         */

        @Override
        public void traceConsumers(Set<SignalConsumer> addToExistingSet) {
            ComplexNeuron.this.traceConsumers(addToExistingSet);
        }

        @Override
        public void traceProviders(Set<SignalProvider> addToExistingSet) {
            ComplexNeuron.this.traceProviders(addToExistingSet);
        }

        @Override
        public int decisionCount() {
            return ComplexNeuron.this.numOutputs;
        }

        @Override
        public int getMaxNoOpRounds() {
            return 0;
        }

        @Override
        public int numSensorNodesRequired() {
            return ComplexNeuron.this.numInputs;
        }
    }

    public class InternalNet extends NeuralNet<ComplexNeuronMember, InternalNet, ComplexNeuronMember> {
        private final List<Input> sensors = makeSensors();
        private final List<Output> decisionNodes = makeDecisionNodes();

        private InternalNet(InternalNet deserializedFrom, Void v) {
            super(deserializedFrom, v);
        }

        private InternalNet() { }

        private InternalNet(NeuralNet cloneFrom) {
            this.cloneNeurons(cloneFrom, null, null);
        }

        private InternalNet(NeuralNet cloneFrom,
                            Map<SignalProvider, SignalProvider> providersMap,
                            Map<SignalConsumer, SignalConsumer> consumersMap) {

            this.cloneNeurons(cloneFrom, providersMap, consumersMap);
        }


        private List<Input> makeSensors() {
            List<Input> list = new ArrayList<>(ComplexNeuron.this.numInputs);
            for (int i = 0; i < ComplexNeuron.this.numInputs; i++) {
                list.add(new Input(i));
            }
            return Collections.unmodifiableList(list);
        }

        private List<Output> makeDecisionNodes() {
            List<Output> list = new ArrayList<>(ComplexNeuron.this.numInputs);
            for (int i = 0; i < ComplexNeuron.this.numOutputs; i++) {
                list.add(new Output(i));
            }
            return Collections.unmodifiableList(list);
        }

        @Override
        public InternalNet traceNeuronsSet() {
            ComplexNeuron.this.fitness = null;
            return super.traceNeuronsSet();
        }

        @Override
        public List<Input> getSensors() {
            return this.sensors;
        }

        @Override
        public List<Output> getDecisionNodes() {
            return this.decisionNodes;
        }

        @Override
        public InternalNet clone() {
            throw new UnsupportedOperationException();
        }

        @Override
        public InternalNet cloneWith(Map p, Map c) {
            throw new UnsupportedOperationException();
        }

        private class Input extends NeuralNet<ComplexNeuronMember, InternalNet, ComplexNeuronMember>.Sensor {
            private final int index;
            private SignalProvider input;

            private Input(int index) {
                this.index = index;
            }

            @Override
            public int getSensorId() {
                return this.index;
            }

            @Override
            public short sense() {
                return this.input.getOutput();
            }

            /**
             * For the Internal Sensors and Decision nodes of complex neurons, we do not bitmask their neuralHashes
             * with the normal headers/masks for Sensors/DecisionNodes.  Instead, the neural hash from the input/
             * output providers is passed straight through.  Therefore, a neural net with a complex neuron wrapping
             * a subsection of it should have an identical neural hash to the same network in which the complex
             * neuron is "unwrapped" and its internal neurons become part of the outer net.
             *
             * @return Neural hash of the input SignalProvider of the outer net that is associated with
             * this internal sensor node
             */
            @Override
            public long calcNeuralHash() {
                return this.input.getNeuralHash();
            }

            @Override
            public long getNeuralHashFor(LoopingNeuron looper) {
                // no 'calcNeuralHashFor(looper)' method for a CachingSignalProvider (Sensors are not a CachingNeuron)
                // ... overriding getNeuralHashFor instead
                return this.input.getNeuralHashFor(looper);
            }
        }

        private class Output extends NeuralNet<ComplexNeuronMember, InternalNet, ComplexNeuronMember>.Decision {
            private final int index;

            private Output(int index) {
                this.index = index;
            }

            @Override
            public int getDecisionId() {
                return this.index;
            }

            /**
             * For the Internal Sensors and Decision nodes of complex neurons, we do not bitmask their neuralHashes
             * with the normal headers/masks for Sensors/DecisionNodes.  Instead, the neural hash from the input/
             * output providers is passed straight through.  Therefore, a neural net with a complex neuron wrapping
             * a subsection of it should have an identical neural hash to the same network in which the complex
             * neuron is "unwrapped" and its internal neurons become part of the outer net.
             *
             * @return Neural hash of the SignalProvider of the inner net that provides the input signal to this
             * decision node
             */
            public long getNeuralHash() {
                return this.getInputs().get(0).getNeuralHash();
            }
        }
    }


    private void updateSensorInputs(int fromIndex, int toIndex) {
        toIndex = Math.min(this.numInputs, toIndex);
        if (fromIndex >= toIndex) return;

        int i = fromIndex;
        int end = Math.min(this.inputs.size(), toIndex);
        for (; i < end; i++) {
            this.inputSensors.get(i).input = this.inputs.get(i);
        }

        for (; i < toIndex; i++) {
            this.inputSensors.get(i).input = null;
        }
    }

    @Override
    public void setInputs(List<SignalProvider> inputs) throws IllegalArgumentException {
        super.setInputs(inputs);
        this.updateSensorInputs(0, this.numInputs);
    }

    @Override
    public void addInput(SignalProvider newProvider) {
        super.addInput(newProvider);
        int size = this.inputs.size();
        this.updateSensorInputs(size - 1, size);
    }

    @Override
    public void addInput(int index, SignalProvider newProvider) {
        super.addInput(index, newProvider);
        this.updateSensorInputs(index, this.numInputs);
    }

    @Override
    public SignalProvider removeInput(int index) {
        SignalProvider val = super.removeInput(index);
        this.updateSensorInputs(index, this.numInputs);
        return val;
    }

    @Override
    public boolean removeInput(SignalProvider removeAll) {
        boolean val = super.removeInput(removeAll);
        this.updateSensorInputs(0, this.numInputs);
        return val;
    }

    @Override
    public SignalProvider replaceInput(int index, SignalProvider newProvider) throws NullPointerException {
        SignalProvider val = super.replaceInput(index, newProvider);
        this.updateSensorInputs(index, index + 1);
        return val;
    }

    @Override
    public boolean replaceInput(SignalProvider oldProvider, SignalProvider newProvider) throws IllegalArgumentException, NullPointerException {
        boolean val = super.replaceInput(oldProvider, newProvider);
        this.updateSensorInputs(0, this.numInputs);
        return val;
    }

    @Override
    public void replaceInputs(Map<SignalProvider, SignalProvider> neuronMap) throws NoSuchElementException {
        super.replaceInputs(neuronMap);
        this.updateSensorInputs(0, this.numInputs);
    }

    @Override
    public void clearInputs() {
        super.clearInputs();
        for (int i = 0; i < this.numInputs; i++) {
            this.inputSensors.get(i).input = null;
        }
    }

    @Override
    public void replaceConsumers(Map<SignalConsumer, SignalConsumer> neuronMap) {
        super.replaceConsumers(neuronMap);
        for (Neuron output : ComplexNeuron.this.members) {
            if (output != this) output.replaceConsumers(neuronMap);
        }
    }

    /*
    @Override
    public boolean removeConsumer(SignalConsumer consumer) {
        if (this.outputs.contains(consumer)) return false;
        return super.removeConsumer(consumer);
    }

    @Override
    public void replaceConsumers(Map<SignalConsumer, SignalConsumer> neuronMap) {
        for (Neuron output : ComplexNeuron.this.outputs) {
            if (output == this) {
                ((ComplexNeuron)output).replaceConsumersNoRecurse(neuronMap);
            } else {
                ((MultiOutput)output).replaceConsumersNoRecurse(neuronMap);
            }
        }
    }

    private void replaceConsumersNoRecurse(Map<SignalConsumer, SignalConsumer> neuronMap) {
        super.replaceConsumers(neuronMap);
        this.addConsumers(this.outputs);
    }
    */
    /*
    @Override
    public boolean replaceConsumer(SignalConsumer oldConsumer, SignalConsumer newConsumer) {
        if (this.outputs.contains(oldConsumer)) return false;
        return super.replaceConsumer(oldConsumer, newConsumer);
    }
     */


    @Override
    public void traceProviders(Set<SignalProvider> addToExistingSet) {
        if (addToExistingSet.contains(this)) return;
        addToExistingSet.addAll(this.members);

        for (SignalProvider neuron : this.inputs) {
            if (neuron instanceof Neuron) {
                ((Neuron)neuron).traceProviders(addToExistingSet);
            }
        }
    }

    @Override
    public void traceConsumers(Set<SignalConsumer> addToExistingSet) {
        if (addToExistingSet.contains(this)) return;
        addToExistingSet.addAll(this.members);
        Set<SignalConsumer> consumers = this.getConsumers();
        for (Neuron output : this.members) {
            if (output != this) consumers.addAll(output.getConsumers());
        }

        for (SignalConsumer neuron : consumers) {
            if (neuron instanceof Neuron) {
                ((Neuron)neuron).traceConsumers(addToExistingSet);
            }
        }
    }

    @Override
    public long calcNeuralHashFor(LoopingNeuron looper) {
        return this.decisionNode0.getInputs().get(0).getNeuralHashFor(looper);
    }
}
