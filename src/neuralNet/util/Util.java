package neuralNet.util;

import net.openhft.affinity.*;

import java.util.*;
import java.util.concurrent.*;

public abstract class Util {
    private Util() { throw new IllegalStateException(); }

    public static final double RANGE = (double)Short.MAX_VALUE - (double)Short.MIN_VALUE;
    public static final int RANGE_INT = (int)Short.MAX_VALUE - (int)Short.MIN_VALUE + 1;
    public static final double RANGE_INV = 1 / RANGE;

    public static final double NORMALIZE = RANGE + 1;
    public static final int NORMALIZE_INT = RANGE_INT + 1;
    public static final double NORMALIZE_INV = 1 / NORMALIZE;
    public static final double TWICE_NORMALIZE = NORMALIZE * 2;

    public static final double ZEROIZE = -((double)Short.MIN_VALUE);
    public static final int ZEROIZE_INT = -((int)Short.MIN_VALUE);
    public static final double ONEIZE = ZEROIZE + 1;

    //ADD to a zeroized variable that had mod RANGE applied and is now negative, to get an in-range short (instead of subtracting ZEROIZE)
    public static final double DEZEROIZE_NEGATIVE = RANGE - ZEROIZE;
    public static final int DEZEROIZE_NEGATIVE_INT = RANGE_INT - ZEROIZE_INT;

    public static final double MAX_PLUS_ONE = (double)Short.MAX_VALUE + 1;
    public static final double HALF_MAX_PLUS_ONE = ((double)Short.MAX_VALUE + 1) / 2;
    public static final double QUARTER_MAX_PLUS_ONE = ((double)Short.MAX_VALUE + 1) / 4;

    public static final double HALF_MAX = (double)Short.MAX_VALUE / 2;
    public static final double HALF_MIN = (double)Short.MIN_VALUE / 2;
    public static final double QUARTER_MIN = (double)Short.MIN_VALUE / 4;

    public static final double TWICE_MAX = (double)Short.MAX_VALUE * 2.0;
    public static final double TWICE_MIN = (double)Short.MIN_VALUE * 2.0;
    public static final double TWICE_RANGE = TWICE_MAX - TWICE_MIN;

    public static final double PI = Math.PI;
    public static final double TWO_PI = Math.PI * 2;

    public static final double BILLION = 1_000_000_000;

    /**
     * A true modulo operation (NOT a remainder) where all results are >= 0
     *
     * @param dividend
     * @param divisor
     * @return the true modulus (positive or zero) of the dividend and divisor
     */
    public static double mod(double dividend, double divisor) {
        double result = dividend % divisor;
        if (result < 0) return result + (divisor > 0 ? divisor : -divisor);
        else return result;
    }

    /**
     * A true modulo operation (NOT a remainder) where all results are >= 0
     *
     * @param dividend
     * @param divisor
     * @return the true modulus (positive or zero) of the dividend and divisor
     */
    public static double mod(double dividend, int divisor) {
        double result = dividend % divisor;
        if (result < 0) return result + divisor;
        else return result;
    }

    public static short clip(int value) {
        if (value <= Short.MIN_VALUE) return Short.MIN_VALUE;
        else if (value >= Short.MAX_VALUE) return Short.MAX_VALUE;
        else return (short) value;
    }

    public static short clip(long value) {
        if (value <= Short.MIN_VALUE) return Short.MIN_VALUE;
        else if (value >= Short.MAX_VALUE) return Short.MAX_VALUE;
        else return (short) value;
    }

    public static short clip(float value) {
        if (value <= Short.MIN_VALUE) return Short.MIN_VALUE;
        else if (value >= Short.MAX_VALUE) return Short.MAX_VALUE;
        else return (short) value;
    }

    public static short clip(double value) {
        if (value <= Short.MIN_VALUE) return Short.MIN_VALUE;
        else if (value >= Short.MAX_VALUE) return Short.MAX_VALUE;
        else return (short) value;
    }

    public static short roundClip(float value) {
        int rounded = Math.round(value);
        if (rounded <= Short.MIN_VALUE) return Short.MIN_VALUE;
        else if (rounded >= Short.MAX_VALUE) return Short.MAX_VALUE;
        else return (short) rounded;
    }

    public static short roundClip(double value) {
        long rounded = Math.round(value);
        if (rounded <= Short.MIN_VALUE) return Short.MIN_VALUE;
        else if (rounded >= Short.MAX_VALUE) return Short.MAX_VALUE;
        else return (short) rounded;
    }

    public static double min(double ... values) throws IllegalArgumentException {
        if (values.length < 1) throw new IllegalArgumentException();
        double min = Double.MAX_VALUE;
        for (double value : values) {
            if (!Double.isFinite(value)) throw new IllegalArgumentException(value + "");
            if (value < min) min = value;
        }
        return min;
    }

    public static double max(double ... values) throws IllegalArgumentException {
        if (values.length < 1) throw new IllegalArgumentException();
        double max = Double.MIN_VALUE;
        for (double value : values) {
            if (!Double.isFinite(value)) throw new IllegalArgumentException(value + "");
            if (value > max) max = value;
        }
        return max;
    }

    public static int min(int ... values) throws IllegalArgumentException {
        if (values.length < 1) throw new IllegalArgumentException();
        int min = Integer.MAX_VALUE;
        for (int value : values) {
            if (value < min) min = value;
        }
        return min;
    }

    public static int max(int ... values) throws IllegalArgumentException {
        if (values.length < 1) throw new IllegalArgumentException();
        int max = Integer.MIN_VALUE;
        for (int value : values) {
            if (value > max) max = value;
        }
        return max;
    }

    public static <T> T pickRandomExcept(List<T> list, T except) {
        boolean nothingDifferent = true;
        for (T item : list) {
            if (item != except) {
                nothingDifferent = false;
                break;
            }
        }
        if (nothingDifferent) throw new IllegalArgumentException();

        ThreadLocalRandom rand = ThreadLocalRandom.current();
        while (true) {
            T result = list.get(rand.nextInt(list.size()));
            if (result != except) return result;
        }
    }



    private static Map<Integer, Thread> affinities = new TreeMap();
    private static final int MAX_ATTEMPTS = 8;
    private static final long WAIT_BETWEEN_ATTEMPTS = 512;
    private static final int ITERATIONS_TO_TEST_UNEXPECTED_AFFINITY = 256;

    public static AffinityLock obtainUniqueLock() throws RuntimeException {
        Thread current = Thread.currentThread();

        AffinityLock lock = null;
        boolean keepLock = false;

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) try {
            int cpuId;
            int affCpu;
            boolean testOtherId;
            synchronized (affinities) {
                int i = 0;
                while (affinities.containsKey(i)) {
                    i++;
                }

                lock = AffinityLock.acquireLock(i);

                cpuId = lock.cpuId();
                affCpu = Affinity.getCpu();

                if (cpuId == i && affCpu == i) {
                    affinities.put(cpuId, Thread.currentThread());
                    keepLock = true;
                    return lock;
                }

                testOtherId = cpuId == affCpu && cpuId >= 0 && !affinities.containsKey(cpuId);

                if (testOtherId) {
                    affinities.put(cpuId, Thread.currentThread());

                } else {
                    lock.release();
                    lock = null;
                }
            }

            if (testOtherId) {
                for (int i = 0; i < ITERATIONS_TO_TEST_UNEXPECTED_AFFINITY; i++) {
                    try {
                        Thread.sleep(WAIT_BETWEEN_ATTEMPTS / ITERATIONS_TO_TEST_UNEXPECTED_AFFINITY);

                    } catch (InterruptedException e) {
                        System.err.println(e);
                    }

                    if (cpuId != lock.cpuId() || Affinity.getCpu() != affCpu) {
                        lock.release();
                        lock = null;
                        break;
                    }
                }

                if (lock != null) {
                    keepLock = true;
                    return lock;
                }
            }

            try {
                Thread.sleep(WAIT_BETWEEN_ATTEMPTS);

            } catch (InterruptedException e) {
                System.err.println(e);
            }


        } finally {
            if (lock != null && !keepLock) {
                lock.release();
            }
        }
        throw new RuntimeException("Could not obtain unique affinity lock");
    }
}
