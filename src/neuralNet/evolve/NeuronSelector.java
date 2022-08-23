package neuralNet.evolve;

import neuralNet.function.*;
import neuralNet.neuron.*;

import java.util.*;

public class NeuronSelector {

    //TODO: Add Multiply neurons to the below list
    //TODO: Create StaticWeightedAverage and VariableWeightedAverage functions
    public static final List<SignalProvider> TEMPLATES = List.of(
            AdditionCircular.makeNeuron(),
            AdditionClipped.makeNeuron(),
            Average.makeNeuron(),
            Ceiling.makeNeuron((short)-1),
            Closeness.makeNeuron(),
            Decrease.makeNeuron(),
            Deviation.makeNeuron(),
            DifferenceCircular.makeNeuron(),
            DifferenceClipped.makeNeuron(),
            DifferenceNormalized.makeNeuron(),
            Equals.makeNeuron(),
            Farness.makeNeuron(),
            Floor.makeNeuron((short)0),
            GreaterThan.makeNeuron(),
            GreaterThanOrEqualTo.makeNeuron(),
            HardSwitch.makeNeuron(),
            Increase.makeNeuron(),
            LessThan.makeNeuron(),
            LessThanOrEqualTo.makeNeuron(),
            LinearTransformCircular.makeNeuron(1, 0),
            LinearTransformClipped.makeNeuron(1, 0),
            Max.makeNeuron(),
            Min.makeNeuron(),
            MultiplyCircular.makeNeuron(),
            MultiplyClipped.makeNeuron(),
            MultiplyNormalized.makeNeuron(),
            Narrow.makeNeuron(),
            NegateBalanced.makeNeuron(),
            NegateClipped.makeNeuron(),
            NotEquals.makeNeuron(),
            RandomValue.makeNeuron(),
            SawWave.makeNeuron(16, 0),
            SineWave.makeNeuron(16, 0),
            SoftSwitch.makeNeuron(),
            SoftSwitchCircular.makeNeuron(),
            SquareWave.makeNeuron(16, 0),
            TriangleWave.makeNeuron(16, 0),
            Uniformity.makeNeuron(),
            Widen.makeNeuron()
        );
}
