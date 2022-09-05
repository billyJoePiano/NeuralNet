package neuralNet.test;

import java.util.concurrent.*;

import static neuralNet.util.Util.*;

public class TestRotateBits {
    public static final int ITERATIONS = 256;

    public static void main(String[] args) {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        for (int i = 0; i < ITERATIONS; i++) {
            long val = rand.nextLong();
            String valHex = leftFill(Long.toHexString(val), '0', 16);
            String valBin = leftFill(Long.toBinaryString(val), '0', 64);


            int bits = rand.nextInt(0, 257);
            if (rand.nextBoolean()) bits = -bits;

            long rotated = Long.rotateRight(val, bits);
            long restored = Long.rotateRight(rotated, -bits);

            String rotHex = leftFill(Long.toHexString(rotated), '0', 16);
            String rotBin = leftFill(Long.toBinaryString(rotated), '0', 64);

            String resHex = leftFill(Long.toHexString(restored), '0', 16);

            System.out.println(valHex);
            System.out.println(rotHex);
            System.out.println(resHex);

            System.out.println(valBin + "  " + valBin);

            int modBits = bits % 64;
            if (modBits <= 0) modBits += 64;

            System.out.println(leftFill(rotBin.substring(0, modBits), ' ', 64) + "  "
                            + rightFill(rotBin.substring(modBits), ' ', 64)
                            + "  (" + bits + ((bits != modBits && bits != 0) ? " aka " + modBits : "" ) + ")\n");

            assert val == rotated;
        }
    }


}
