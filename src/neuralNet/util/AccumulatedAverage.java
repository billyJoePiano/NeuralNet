package neuralNet.util;

import java.util.*;
/**
 * Each successive index in the accumulated array represents a power of two, in terms of
 * the number of rounds of memories it represents as an accumulated average.  This makes it
 * easy to combine accumulated averages over time, as the number of memories grows.  Doing this
 * avoids creating an excessively long array with time-consuming calculations each time the weighted
 * average needs to be calculated.
 *
 * EXAMPLE: 1 new memory to add to a small established array...
 *
 * 1 -> [ 1 , 2 , - , 8 , 16 , - ]
 *      ...becomes...
 * ---> [1+1, 2 , - , 8 , 16 , - ] <--- combining through powers of two
 *      [ 2 + 2 , - , 8 , 16 , - ] <--- keep combining...
 *      [ - , - , 4 , 8 , 16 , - ] <--- ...until an empty slot is reached
 *
 * (values are the number of memories represented at each index ... dash indicates NaN, aka nothing represented)
 *
 * In the resulting example, only 3 values are needed to calculate the final average representing 28 memories!
 *
 *
 */
public class AccumulatedAverage {
    public static final double NaN = Double.NaN;

    private double[] accumulated;
    private Double cachedAvg = null;
    private long size = 0;

    public AccumulatedAverage() {
        this.accumulated = new double[10];
        Arrays.fill(this.accumulated, NaN);
    }

    public AccumulatedAverage(int startingCapacity) {
        this.accumulated = new double[startingCapacity];
        Arrays.fill(this.accumulated, NaN);
    }

    public void add(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException(value + "");
        this.size++;
        this.cachedAvg = null;

        double addToStore = value;

        for (int i = 0; i < this.accumulated.length; i++) {

            double stored = this.accumulated[i];

            if ( /* NaN */ stored != stored) {
                this.accumulated[i] = addToStore;
                return;

            } else  {
                addToStore = (addToStore + stored) / 2;
                this.accumulated[i] = NaN;
            }
        }

        this.expandAccumulated(addToStore);
    }

    public double addAndGetAverage(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException(value + "");

        this.size++;

        double addToStore = value;
        double weightSum = 0;
        double currentWeight = 1;
        double avg = 0;

        for (int i = 0; i < this.accumulated.length; i++) {

            double stored = this.accumulated[i];

            if (/* NaN */ stored != stored) {
                if (/* not NaN */ addToStore == addToStore) {
                    avg = avg * weightSum + addToStore * currentWeight;
                    weightSum += currentWeight;
                    avg /= weightSum;

                    this.accumulated[i] = addToStore;
                    addToStore = NaN;
                }

            } else if (/* not NaN */ addToStore == addToStore) {
                addToStore = (addToStore + stored) / 2;
                this.accumulated[i] = NaN;

            } else {
                avg = avg * weightSum + stored * currentWeight;
                weightSum += currentWeight;
                avg /= weightSum;
            }

            currentWeight *= 2;
        }

        if (/* not NaN */ addToStore == addToStore) {
            this.expandAccumulated(addToStore);

            avg = avg * weightSum + addToStore * currentWeight;
            weightSum += currentWeight;
            avg /= weightSum;
        }

        return this.cachedAvg = avg;
    }

    private void expandAccumulated(double addToStore) {
        double[] oldArr = this.accumulated;
        this.accumulated = new double[this.accumulated.length + 10];
        int i;
        for (i = 0; i < oldArr.length; i++) this.accumulated[i] = oldArr[i];
        this.accumulated[i++] = addToStore;
        for (; i < this.accumulated.length; i++) this.accumulated[i] = NaN;
    }

    public double getAverage() {
        if (this.cachedAvg != null) return cachedAvg;

        double avg = 0;
        double weightSum = 0;
        double currentWeight = 1;

        for (double stored : this.accumulated) {
            if (/* not NaN */ stored == stored) {
                avg = avg * weightSum + stored * currentWeight;
                weightSum += currentWeight;
                avg /= weightSum;
            }

            currentWeight *= 2;
        }

        return this.cachedAvg = avg;
    }

    public void clear() {
        Arrays.fill(this.accumulated, NaN);
        this.size = 0;
    }

    public long size() {
        return this.size;
    }
}
