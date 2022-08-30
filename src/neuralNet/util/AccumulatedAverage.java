package neuralNet.util;

import java.util.*;

public class AccumulatedAverage {
    private final List<Double> accumulated = new ArrayList<>();
    private Double cachedAvg = null;

    public AccumulatedAverage() { }

    public void add(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException(value + "");

        Double addToStore = value;

        for (ListIterator<Double> iterator = this.accumulated.listIterator();
                iterator.hasNext();) {

            Double stored = iterator.next();

            if (stored == null) {
                iterator.set(addToStore);
                addToStore = null;
                break;

            } else  {
                addToStore = (addToStore + stored) / 2;
                iterator.set(null);
            }
        }

        if (addToStore != null) {
            this.accumulated.add(addToStore);
        }
        this.cachedAvg = null;
    }

    public double addAndGetAverage(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException(value + "");

        Double addToStore = value;
        double weightSum = 0;
        double currentWeight = 1;
        double avg = 0;

        for (ListIterator<Double> iterator = this.accumulated.listIterator();
             iterator.hasNext();) {

            Double stored = iterator.next();

            if (stored == null) {
                if (addToStore != null) {
                    avg = avg * weightSum + addToStore * currentWeight;
                    weightSum += currentWeight;
                    avg /= weightSum;

                    iterator.set(addToStore);
                    addToStore = null;
                }

            } else if (addToStore != null) {
                addToStore = (addToStore + stored) / 2;
                iterator.set(null);

            } else {
                avg = avg * weightSum + stored * currentWeight;
                weightSum += currentWeight;
                avg /= weightSum;
            }

            currentWeight *= 2;
        }

        if (addToStore != null) {
            avg = avg * weightSum + addToStore * currentWeight;
            weightSum += currentWeight;
            avg /= weightSum;

            this.accumulated.add(addToStore);
        }

        return this.cachedAvg = avg;
    }

    public double getAverage() {
        if (this.cachedAvg != null) return cachedAvg;

        double avg = 0;
        double weightSum = 0;
        double currentWeight = 1;

        for (Double stored : this.accumulated) {
            if (stored != null) {
                avg = avg * weightSum + stored * currentWeight;
                weightSum += currentWeight;
                avg /= currentWeight;
            }

            currentWeight *= 2;
        }

        return this.cachedAvg = avg;
    }
}
