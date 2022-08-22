package neuralNet.util;

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

    //ADD to a zeroized variable that had mod RANGE applied and is now negative, to get an in-range short (instead of subtracting ZEROIZE)
    public static final double DEZEROIZE_NEGATIVE = RANGE - ZEROIZE;
    public static final int DEZEROIZE_NEGATIVE_INT = RANGE_INT - ZEROIZE_INT;

    public static final double MAX_PLUS_ONE = (double)Short.MAX_VALUE + 1;
    public static final double HALF_MAX_PLUS_ONE = ((double)Short.MAX_VALUE + 1) / 2;

    public static final double HALF_MAX = (double)Short.MAX_VALUE / 2;
    public static final double HALF_MIN = (double)Short.MIN_VALUE / 2;

    public static final double TWICE_MAX = (double)Short.MAX_VALUE * 2.0;
    public static final double TWICE_MIN = (double)Short.MIN_VALUE * 2.0;
    public static final double TWICE_RANGE = TWICE_MAX - TWICE_MIN;

    public static final double PI = Math.PI;
    public static final double TWO_PI = Math.PI * 2;

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
}
