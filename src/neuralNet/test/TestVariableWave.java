package neuralNet.test;

import neuralNet.function.*;
import neuralNet.network.*;
import neuralNet.neuron.*;

import java.util.*;

/**
 * Important to run with debugger and put a breakpoint in TestWave at the point it checks for out of phase
 * waves.  Look for 'boolean breakpoint = true;' (currently line 177 of TestWave, 8-19-2022)
 */
public class TestVariableWave {
    public static final double PERIOD_MIN = 256;
    public static final double PERIOD_MAX = PERIOD_MIN * 256;

    public static final int ROUNDS = ((int)Math.ceil(Math.abs(PERIOD_MAX)) + (int)Math.ceil(Math.abs(PERIOD_MIN))) * 256;

    public static final Neuron periodRandom = RandomValue.makeNeuron();
    public static final Neuron periodMemory = new ShortTermMemoryNeuron(periodRandom, 16);
    public static final Neuron periodAverage = Average.makeNeuron(periodMemory, periodRandom);
    public static final Neuron period = Widen.makeNeuron(periodAverage);
    public static final Neuron phase = SineWave.makeNeuron(PERIOD_MAX * 256, 0);


    public static final VariableWaveNeuron sine = SineWave.makeNeuron(period, phase, PERIOD_MIN, PERIOD_MAX);
    public static final VariableWaveNeuron triangle = TriangleWave.makeNeuron(period, phase, PERIOD_MIN, PERIOD_MAX);
    public static final VariableWaveNeuron saw = SawWave.makeNeuron(period, phase, PERIOD_MIN, PERIOD_MAX);
    public static final VariableWaveNeuron square = SquareWave.makeNeuron(period, phase, PERIOD_MIN, PERIOD_MAX);
    public static final StaticWaveNeuron outOfPhase = SawWave.makeNeuron(PERIOD_MIN, 0);

    public static final List<Neuron> waves = List.of(sine, triangle, saw, square);
    //public static final List<Neuron> waves = List.of(sine, triangle, saw, square, outOfPhase); //for testing the neuralNet.test... make sure it catches an out of phase wave
    public static final List<Neuron> others = List.of(periodRandom, periodMemory, periodAverage, period, phase);


    public static void main(String[] args) {
        TestWave tester = new TestWave(waves);
        tester.registerDependencies(others);

        tester.runRounds(-1, charSeq -> { /*
            System.out.println(charSeq + "\t\t\tPhase position: " + sine.getCurrentPhasePosition()
                                        + "\t\tPeriod: " + sine.getLastPeriod()
                                        + "\t\tPhase shift: " + sine.getCurrentPhaseShift()); */
            });
    }

}
