package neuralNet.neuron;

import neuralNet.function.*;
import neuralNet.network.*;
import neuralNet.util.*;

import java.util.*;

import static neuralNet.util.Util.*;

/**
 * bits meaning (most significant to least significant, 0-indexed)
 *
 * 'Header' byte is identifier
 *
 * 0    Is Consumer
 * 1    Uses Function
 * 2-7  Neuron type identifier ... e.g. Function identifier (if applicable) or other (see below map)
 *
 * bytes 8 - 63 are used to represent tweakable parameters, number of inputs (if applicable), sensor/decision
 * node id, etc... depending upon the neuron type
 *
 * For consumers, once this initial hash is created it is XOR'd with every input's hash rotated 8 bits
 * to the right
 *
 *
 *
 */

public class NeuralHash {
    private NeuralHash() { throw new UnsupportedOperationException(); }

    public static final Map<Class, Long> HEADERS = mapOf(
            Class.class, Long.class,
            //Providers/Consumers/Neurons
            SensorNode.class,               header(0b00_000000),
            DecisionNode.class,             header(0b10_000001), //only pure consumer which needs a neural hash
            FixedValueProvider.class,       header(0b00_000010),
            RandomValueProvider.class,      header(0b01_000011),
            StaticWaveProvider.class,       header(0b01_000100),
            VariableWaveNeuron.class,       header(0b11_000101),
            ShortTermMemoryNeuron.class,    header(0b10_000110),
            LongTermMemoryNeuron.class,     header(0b10_000111),

            //Functions
            AdditionCircular.class,         header(0b11_001000),
            AdditionClipped.class,          header(0b11_001001),
            Average.class,                  header(0b11_001010),
            Ceiling.class,                  header(0b11_001011),
            Closeness.class,                header(0b11_001100),
            Decrease.class,                 header(0b11_001101),
            Deviation.class,                header(0b11_001110),
            DifferenceCircular.class,       header(0b11_001111),
            DifferenceClipped.class,        header(0b11_010000),
            DifferenceNormalized.class,     header(0b11_010001),
            Equals.class,                   header(0b11_010010),
            Farness.class,                  header(0b11_010011),
            Floor.class,                    header(0b11_010100),
            GreaterThan.class,              header(0b11_010101),
            GreaterThanOrEqualTo.class,     header(0b11_010110),
            HardSwitch.class,               header(0b11_010111),
            Increase.class,                 header(0b11_011000),
            LessThan.class,                 header(0b11_011001),
            LessThanOrEqualTo.class,        header(0b11_011010),
            LinearTransformCircular.class,  header(0b11_011011),
            LinearTransformClipped.class,   header(0b11_011100),
            Max.class,                      header(0b11_011101),
            Min.class,                      header(0b11_011110),
            MultiplyCircular.class,         header(0b11_011111),
            MultiplyClipped.class,          header(0b11_100000),
            MultiplyNormalized.class,       header(0b11_100001),
            Narrow.class,                   header(0b11_100010),
            NegateBalanced.class,           header(0b11_100011),
            Negate.class,                   header(0b11_100100),
            NotEquals.class,                header(0b11_100101),
            SoftSwitch.class,               header(0b11_100110),
            SoftSwitchCircular.class,       header(0b11_100111),
            Uniformity.class,               header(0b11_101000),
            VariableWeightedAverage.class,  header(0b11_101001),
            WeightedAverage.class,          header(0b11_101010),
            Widen.class,                    header(0b11_101011)
    );

    public static long header(int header) {
        return byteToHeader((byte)header);
    }

    public static long byteToHeader(byte header) {
        return Long.rotateLeft(header & 0xff, 56);
    }

    /*
    public final long value;
    private transient byte[] bytes;


    public NeuralHash(long value) {
        this.value = value;
    }

    public NeuralHash(byte[] bytes) {
        if (bytes.length != 8) throw new IllegalArgumentException();
        this.value = bytesToLong(bytes);
        this.bytes = bytes.clone();
    }

    public byte[] getBytes() {
        if (this.bytes == null) this.bytes = longToBytes(this.value);
        return this.bytes.clone();
    }

    public String toString() {
        return "NeuralHash(" + toHex(this.value) + ")";
    }

    @Override
    public int hashCode() {
        return (int)this.value ^ (int)(this.value >>> 32);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (o == this) return true;
        if (!Objects.equals(this.getClass(), o.getClass())) return false;
        return ((NeuralHash) o).value == this.value;
    }
     */

    public static boolean equals(byte[] bytes1, byte[] bytes2) {
        if (bytes1 == null) return bytes2 == null;
        else if (bytes2 == null) return false;
        else if (bytes1.length != bytes2.length) return false;

        for (int i = 0; i < bytes1.length; i++) {
            if (bytes1[i] != bytes2[i]) return false;
        }
        return true;
    }

    public static long bytesToLong(byte[] bytes) {
        if (bytes.length != 8) throw new IllegalArgumentException();
        long value = 0;
        for (int i = 0; i < 8; i++) {
            value = (value << 8) + (bytes[i] & 0xff);
        }
        return value;
    }

    public static byte[] longToBytes(long value) {
        byte[] bytes = new byte[8];
        for (int i = 7; i > 0; i--) {
            bytes[i] = (byte)value;
            value >>>= 8;
        }
        bytes[0] = (byte)value; //skip the last bit shift, since it is unneeded
        return bytes;
    }

    // https://mkyong.com/java/java-how-to-convert-bytes-to-hex/
    public static String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte aByte : bytes) {
            result.append(String.format("%02x", aByte));
        }
        return result.toString();
    }

    public static String toHex(long value) {
        return leftFill(Long.toHexString(value), '0', 16);
    }

    public static String toHex(SignalProvider provider) {
        return toHex(provider.getNeuralHash());
    }

    public static String toHex(DecisionProvider provider) {
        return toHex(provider.getNeuralHash());
    }

    public static String toHex(DecisionNode node) {
        return toHex(node.getNeuralHash());
    }

    public static String toHex(long[] lineage) {
        if (lineage == null) return null;
        if (lineage.length == 0) return "[]";
        StringBuilder str = new StringBuilder(lineage.length * 18 + 1);
        str.append('[');

        for (long hash : lineage) {
            str.append(toHex(hash));
            str.append(", ");
        }

        str.replace(str.length() - 2, str.length(), "]");
        return str.toString();
    }

    public static boolean equals(long[] lineage1, long[] lineage2) {
        if (lineage1 == null) return lineage2 == null;
        else if (lineage2 == null) return false;
        if (lineage1.length != lineage2.length) return false;

        for (int i = 0; i < lineage1.length; i++) {
            if (lineage1[i] != lineage2[i]) return false;
        }
        return true;
    }

    /**
     * Returns a value between 0.0 and 1.0 indicating the degree of kinship between the
     * two lineages.  If one is a direct child of the other, or the lineages are identical
     * (aka they are siblings of the same parent) this should return 1.0
     *
     * @param sharedHash
     * @param lineage1
     * @param lineage2
     * @return
     */
    public static double collisionKinship(long sharedHash, long[] lineage1, long[] lineage2) {
        int sharedAncestors = 0;

        for (int i = lineage1.length - 1, j = lineage2.length - 1;
                i >= 0 || j >= 0;
                i--, j--) {

            if (i >= 0) {
                if (j >= 0) {
                    if (lineage1[i] == lineage2[j]) {
                        sharedAncestors++;
                        continue;

                    } else if (lineage1[i] == sharedHash || lineage2[j] == sharedHash) {
                        sharedAncestors++;
                    }

                } else if (lineage1[i] == sharedHash) {
                    sharedAncestors++;
                }


            } else if (lineage2[j] == sharedHash) { // 'j' must be >= 0 if 'i' is not
                sharedAncestors++;
            }
            break;
        }

        double midpointLen = (double)(lineage1.length + lineage2.length) / 2.0;
        if (sharedAncestors > midpointLen) return 1.0;
        else return (double)sharedAncestors / midpointLen;
    }

    public static boolean checkForHashCollision(long hash, NeuralNet<?, ?, ?> net1, NeuralNet<?, ?, ?> net2) {
        if (net1 == net2) return false;

        System.err.println("Possible hash collision: " + toHex(hash)
                + "\n\t" + net2 + "\n\t" + net1);

        long[] newLin = net2.getLineage(), oldLin = net1.getLineage();
        double kinship = NeuralHash.collisionKinship(hash, newLin, oldLin);
        System.err.print("Kinship score: " + kinship);
        double smallerSize = Math.max(1, Math.min(newLin.length, oldLin.length));
        if (kinship < (smallerSize - 1) / smallerSize) {
            System.err.println(" ... too small");
            System.err.println("Lineages appear too different.  Probable hash collision");
            return true;
        }

        System.err.println(" ... good.  Continuing to other checks");

        List nodes1 = net1.getDecisionNodes();
        List nodes2 = net2.getDecisionNodes();

        int decisionsEqual = 0;
        for (Object p : new DualIterable(nodes2, nodes1)) {
            DualIterable.Pair<DecisionNode<?, ?>> pair = (DualIterable.Pair<DecisionNode<?, ?>>)p;

            DecisionNode<?, ?> d1 = pair.value1(),
                        d2 = pair.value2();

            long hash1 = d1.getNeuralHash(), hash2 = d2.getNeuralHash();

            if (hash1 != hash2) {
                System.err.println("Decision nodes (" + d1.getDecisionId()
                        + "," + d2.getDecisionId()  + ") had different hashes: "
                        + NeuralHash.toHex(hash1) + "  " + NeuralHash.toHex(hash2));
                break;
            }

            SignalProvider input1 = d1.getInputs().get(0);
            SignalProvider input2 = d2.getInputs().get(0);

            hash1 = input1.getNeuralHash();
            hash2 = input2.getNeuralHash();

            if (input1.getClass() != input2.getClass() || hash1 != hash2) {
                System.err.println("Inputs for decision nodes (" + d1.getDecisionId()
                        + "," + d2.getDecisionId()  + ") don't match "
                        + input1 + "\n\t" + input2 + "\n\t" + hash1 + "\t" + hash2);
                break;
            }

            if (input1 instanceof CachingNeuronUsingFunction func1) {
                CachingNeuronUsingFunction func2 = (CachingNeuronUsingFunction) input2;
                hash1 = func1.outputFunction.getNeuralHash();
                hash2 = func2.outputFunction.getNeuralHash();

                if (func1.outputFunction.getClass() != func2.outputFunction.getClass()
                        || hash1 != hash2
                        || func1.outputFunction.hashHeader()    != func2.outputFunction.hashHeader()) {

                    System.err.println("Inputs' functions for decision nodes (" + d1.getDecisionId()
                            + "," + d2.getDecisionId()  + ") don't match "
                            + "\n\t" + func1.outputFunction + "\t" + func2.outputFunction
                            + "\n\t" + hash1 + "\t" + hash2
                            + "\n\t" + func1.outputFunction.hashHeader() + "\t" + func2.outputFunction.hashHeader());
                    break;
                }

                if (func1 instanceof CachingNeuronUsingTweakableFunction tweak1) {
                    CachingNeuronUsingTweakableFunction tweak2 = (CachingNeuronUsingTweakableFunction)input2;
                    short[] toAchieve = tweak1.getTweakingParams(tweak2);
                    for (short param : toAchieve) {
                        if (param != 0) {
                            System.err.println("toAchieve tweaks indicate the params are different\t"
                                    + Util.toString(toAchieve));
                            break;
                        }
                    }
                }
            }
            decisionsEqual++;
        }

        if (decisionsEqual == 5) {
            System.err.println("Decision nodes appear to have equivalent inputs.  Hash collision unlikely");
            return false;

        } else {
            System.err.println("Decision nodes appear to have different inputs.  Almost certain hash collision");
            return true;
        }
    }
}
