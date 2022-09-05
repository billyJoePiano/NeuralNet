package neuralNet.util;

import neuralNet.function.*;
import neuralNet.neuron.*;

import java.util.*;

import static neuralNet.util.Util.leftFill;
import static neuralNet.util.Util.rightFill;

/**
 * Used to generate the source code at the top of NeuralHash, which populates the hash headers for each class
 */
public class GenerateFunctionNeuralHashHeaders {
    //copied from NeuronSelector
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
            new FixedValueProvider((short)0),
            Floor.makeNeuron((short)0),
            GreaterThan.makeNeuron(),
            GreaterThanOrEqualTo.makeNeuron(),
            HardSwitch.makeNeuron(),
            Increase.makeNeuron(),
            LessThan.makeNeuron(),
            LessThanOrEqualTo.makeNeuron(),
            LinearTransformCircular.makeNeuron(1, 0),
            LinearTransformClipped.makeNeuron(1, 0),
            new LongTermMemoryNeuron((short)0, 16, 16),
            Max.makeNeuron(),
            Min.makeNeuron(),
            MultiplyCircular.makeNeuron(),
            MultiplyClipped.makeNeuron(),
            MultiplyNormalized.makeNeuron(),
            Narrow.makeNeuron(),
            NegateBalanced.makeNeuron(),
            Negate.makeNeuron(),
            NotEquals.makeNeuron(),
            new RandomValueProvider(),
            SawWave.makeNeuron(32, 0),
            new ShortTermMemoryNeuron((short)0, 8, 8, 16, 8),
            SineWave.makeNeuron(32, 0),
            SoftSwitch.makeNeuron(),
            SoftSwitchCircular.makeNeuron(),
            SquareWave.makeNeuron(32, 0),
            TriangleWave.makeNeuron(32, 0),
            Uniformity.makeNeuron(),
            new VariableWaveNeuron(SawWave.INSTANCE, 16, 64),
            new VariableWaveNeuron(SineWave.INSTANCE, 16, 64),
            new VariableWaveNeuron(SquareWave.INSTANCE, 16, 64),
            new VariableWaveNeuron(TriangleWave.INSTANCE, 16, 64),
            VariableWeightedAverage.makeNeuron(0.5, 2),
            WeightedAverage.makeNeuron(1, 2),
            Widen.makeNeuron()
    );

    public static void main(String[] args) {
        int longestName = 0;
        for (SignalProvider provider : TEMPLATES) {
            if (!(provider instanceof CachingNeuronUsingFunction func)) continue;
            longestName = Math.max(func.outputFunction.getClass().getSimpleName().length(), longestName);
        }

        longestName += ".class, ".length();

        int fill = (longestName / 4) * 4;
        if (fill != longestName) fill += 4;

        int starting = 0b11_001000;
        boolean first = true;
        StringBuilder str = new StringBuilder();

        for (SignalProvider provider : TEMPLATES) {
            if (!(provider instanceof CachingNeuronUsingFunction func)) continue;

            if (first) first = false;
            else str.append(",\n");

            str.append(rightFill(func.outputFunction.getClass().getSimpleName() + ".class,", ' ', fill));
            str.append("header(0b");
            String bin = leftFill(Integer.toBinaryString(starting++), '0', 8);
            str.append(bin.substring(0, 2));
            str.append('_');
            str.append(bin.substring(2));
            str.append(')');
        }

        System.out.println(str);
    }
}
