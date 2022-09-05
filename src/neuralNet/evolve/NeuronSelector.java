package neuralNet.evolve;

import neuralNet.function.*;
import neuralNet.network.*;
import neuralNet.neuron.*;

import java.util.*;
import java.util.concurrent.*;

import static neuralNet.evolve.Tweakable.*;
import static neuralNet.test.TestUtil.compareObjects;

public class NeuronSelector<N extends NeuralNet<?, N, ?>> {

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

    public static final List<SignalProvider> NO_INPUT = makeSublist(0);
    public static final List<SignalProvider> ONE_INPUT = makeSublist(1);
    public static final List<SignalProvider> TWO_INPUTS = makeSublist(2);
    public static final List<SignalProvider> MORE_THAN_TWO_INPUTS = makeSublist(3);
    public static final int MAX_NEW_INPUTS = 10;
    public static final int MAX_NEW_CONSUMERS = 5;

    private static List<SignalProvider> makeSublist(int inputs) {
        List<SignalProvider> list = new ArrayList<>();
        for (SignalProvider provider : TEMPLATES) {
            if (provider instanceof SignalConsumer) {
                SignalConsumer consumer = (SignalConsumer)provider;
                if (inputs >= consumer.getMinInputs() && inputs <= consumer.getMaxInputs()) {
                    list.add(provider);
                }

            } else if (inputs == 0) {
                list.add(provider);
            }
        }

        return Collections.unmodifiableList(list);
    }

    public static SignalProvider makeRandom() {
        SignalProvider neuron = getRandomTemplate();
        if (neuron instanceof VariableWeightedAverage) {
            System.out.println(neuron);
        }

        if (!(neuron instanceof SignalProvider.Tweakable tweakable)) return neuron.clone();

        ThreadLocalRandom rand = ThreadLocalRandom.current();

        List<Param> params = tweakable.getTweakingParams();
        short[] tweaks = new short[params.size()];
        for (int i = 0; i < tweaks.length; i++) {
            Param param = params.get(i);
            if (param.min > param.max) throw new IllegalStateException();

            double range = Math.max(Math.abs(param.min), Math.abs(param.max));
            int offset = 0;
            Boolean forceSign = null;

            if (param.min == param.max) {
                tweaks[i] = param.min;
                continue;

            } else if (param.min >= 0) {
                forceSign = Boolean.TRUE;
                if (param.min > 0) {
                    range -= param.min;
                    offset = param.min;
                }

            } else if (param.max <= 0) {
                forceSign = Boolean.FALSE;
                if (param.max < 0) {
                    range += param.max;
                    offset = param.max;
                }
            }

            range /= 3.0; // 3 standard deviations, with std = 1.0 for the Gaussian random number generator

            int val;
            do {
                val = (int)Math.round(rand.nextGaussian() * range);
                if (forceSign != null) val = (forceSign ? val < 0 : val > 0) ? -val : val;
                val += offset;

            } while(val < param.min || val > param.max);

            tweaks[i] = (short)val;
        }

        return tweakable.tweak(tweaks);
    }

    public static SignalProvider getRandomTemplate() {
        return TEMPLATES.get(ThreadLocalRandom.current().nextInt(TEMPLATES.size()));
    }

    public static SignalProvider getRandomTemplate(int numInputs) {
        List<SignalProvider> list;
        switch(numInputs) {
            case 0: list = NO_INPUT; break;
            case 1: list = ONE_INPUT; break;
            case 2: list = TWO_INPUTS; break;
            default:
                if (numInputs < 0) throw new IllegalArgumentException(numInputs + "");
                list = MORE_THAN_TWO_INPUTS;
        }

        return list.get(ThreadLocalRandom.current().nextInt(list.size())).clone();
    }

    /**
     * Quick test of the template lists, to ensure they were properly populated
     * @param args
     */
    public static void main(String[] args) {
        System.out.println(TEMPLATES.size() + " " + TEMPLATES);
        System.out.println(NO_INPUT.size() + " " + NO_INPUT);
        System.out.println(ONE_INPUT.size() + " " + ONE_INPUT);
        System.out.println(TWO_INPUTS.size() + " " + TWO_INPUTS);
        System.out.println(MORE_THAN_TWO_INPUTS.size() + " " + MORE_THAN_TWO_INPUTS);

        System.out.println();

        for (SignalProvider provider : TEMPLATES) {
            int count = 0;
            if (NO_INPUT.contains(provider)) count++;
            if (ONE_INPUT.contains(provider)) count++;
            if (TWO_INPUTS.contains(provider)) count++;
            if (MORE_THAN_TWO_INPUTS.contains(provider)) count++;

            System.out.println(provider + " : " + count);

            assert count > 0;
        }

        for (int i = 0; i < 256; i++) {
            SignalProvider rand = makeRandom();
            compareObjects(rand, rand);
        }
    }
}
