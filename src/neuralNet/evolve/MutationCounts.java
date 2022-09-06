package neuralNet.evolve;

import java.util.*;

public class MutationCounts {
    private MutationCounts() { throw new UnsupportedOperationException(); }

    public static int[] calc(int fittestCount, int netsToMake) {
        int[] makeMutations = new int[fittestCount];

        if (netsToMake <= fittestCount) {
            if (netsToMake > 0) {
                Arrays.fill(makeMutations, 0, netsToMake, 1);

            } else {
                netsToMake = 0;
            }

        } else {

            int evenlyDistribute = Math.min(netsToMake, Math.max(fittestCount, (int) Math.ceil((double) netsToMake * 2.0 / 3.0)));
            int baseline = (int) Math.floor((double) evenlyDistribute / (double) fittestCount);

            if (baseline * fittestCount != evenlyDistribute) {
                if (netsToMake - baseline * fittestCount > fittestCount) {
                    baseline++;
                }
                evenlyDistribute = baseline * fittestCount;
            }

            double[] mmFloat = new double[fittestCount];
            int geoDistribute = netsToMake - evenlyDistribute;

            double offset = baseline + 2.0 * (double)geoDistribute / (double) fittestCount;
            double slope = offset / fittestCount;
            offset += baseline;

            int sum = 0;

            for (int i = 0; i < fittestCount; i++) {
                double flt = offset - slope * i;
                assert flt >= 0;
                int intg = (int)Math.round(flt);
                mmFloat[i] = flt;
                makeMutations[i] = intg;
                sum += intg;
            }

            assert sum >= netsToMake;

            if (sum > netsToMake) {
                List<Map.Entry<Integer, Double>> diffs = new ArrayList<>(fittestCount);
                //key = index, value = difference between floating and actual

                for (int i = 0; i < fittestCount; i++) {
                    diffs.add(new AbstractMap.SimpleEntry<>(i, mmFloat[i] - makeMutations[i]));
                }

                while (sum > netsToMake) {
                    diffs.sort((e1, e2) -> {
                        double diff1 = e1.getValue(), diff2 =  e2.getValue();
                        if (diff1 < diff2) return -1;
                        if (diff2 < diff1) return 1;
                        if (diff1 == diff2) return 0;
                        throw new IllegalStateException(diff1 + "\n" + diff2);
                    });

                    for (Map.Entry<Integer, Double> entry : diffs) {
                        int index = entry.getKey();
                        if (makeMutations[index] == 1) continue;
                        makeMutations[index]--;
                        entry.setValue(mmFloat[index] - makeMutations[index]);
                        if (--sum <= netsToMake) break;
                    }
                }
            }
        }

        assert Arrays.stream(makeMutations).sum() == netsToMake;
        return makeMutations;
    }
}
