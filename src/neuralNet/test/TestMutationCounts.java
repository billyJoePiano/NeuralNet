package neuralNet.test;

import neuralNet.evolve.*;
import neuralNet.util.*;

import java.util.*;

import static neuralNet.util.Util.*;

public class TestMutationCounts {
    public static void main(String[] args) {
        for (int netsMultiplier = 8; netsMultiplier < 512; netsMultiplier *= 2){
            int nets = netsMultiplier * 3;
            System.out.println("NETS_PER_GENERATION == " + nets);
            for (int i = 1; i < nets / 2 + 3; i++) {
                long start = System.nanoTime();
                int[] counts = MutationCounts.calc(i, nets - 1);
                long end = System.nanoTime();
                System.out.println(i + " " + Util.toString(counts) + "\t" + (end - start) / MILLION);
            }

            nets = netsMultiplier * 4;
            System.out.println("\n\nNETS_PER_GENERATION == " + nets);
            for (int i = 1; i < nets / 2 + 3; i++) {
                long start = System.nanoTime();
                int[] counts = MutationCounts.calc(i, nets - 1);
                long end = System.nanoTime();
                System.out.println(i + " " + Util.toString(counts) + "\t" + (end - start) / MILLION);
            }

            System.out.println("\n");
        }
    }


    public static void test1(int NETS_PER_GENERATION, int fittestSize) {
        int makeTotal = NETS_PER_GENERATION - fittestSize;

        int mutationsPerNet = (int)Math.round((double)makeTotal / (double)fittestSize  / 2.0);
        int remainder = makeTotal - mutationsPerNet * fittestSize;
        assert remainder >= 0;

        int[] makeMutations = new int[fittestSize];
        Arrays.fill(makeMutations, mutationsPerNet);

        if (mutationsPerNet == 0) {
            // 1 for each until exhausted, in priority of fitness
            assert remainder <= fittestSize;
            for (int i = 0; remainder > 0; i++, remainder--) {
                makeMutations[i]++;
            }
        } else {
            //distribute remainders biased towards higher-performers
            int incUpto;
            for (incUpto = 0; remainder > incUpto; incUpto = (incUpto + 1) % fittestSize) {
                for (int i = 0; i <= incUpto && i < fittestSize; i++) {
                    makeMutations[i]++;
                    remainder--;
                }
            }

            if (incUpto == fittestSize) incUpto = 0;

            assert remainder <= makeTotal - incUpto;
            assert remainder + incUpto < fittestSize;

            for (int i = incUpto; remainder > 0; i = (i + 1) % fittestSize) {
                makeMutations[i]++;
                remainder--;
            }
        }

        assert remainder == 0;
        assert Arrays.stream(makeMutations).sum() == makeTotal;

        System.out.println(fittestSize + "\t" + Util.toString(makeMutations));
    }
}
