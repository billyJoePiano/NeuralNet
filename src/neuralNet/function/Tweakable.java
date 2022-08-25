package neuralNet.function;

import java.util.*;

import static neuralNet.util.Util.*;

/**
 * Tweakable is a generic interface that can applied to neurons, functions, or anything which can have its
 * behavior "tweaked" according to a set of bounded parameters.  The tweak() method should return an identical
 * clone of the original, except with the provided "tweaks" made.
 *
 *
 * @param <F> the implementing class, which is able to be tweaked
 */
public interface Tweakable<F extends Tweakable<F>> {
    /**
     * When paramsPerInput() == true, the returned array's length should be a multiple of one more than
     * the number of inputs, with the additional set of params being for any additional inputs.
     *
     * The exceptions to this are when:
     * -- The number of inputs is at (or above) the maximum, in which case the additional set of params for
     *      new inputs should not be included
     * -- The number of inputs is below the minimum, in which case the number of params provided should
     *      be a multiple of the minimum number of inputs
     *
     * @return the params for tweaking this tweakable object
     */
    public List<Param> getTweakingParams();

    public F tweak(short[] params);

    public short[] getTweakingParams(F toAchieve);

    default public boolean paramsPerInput() { return false; }



    public class Param {
        public static final Param DEFAULT = new Param(Short.MIN_VALUE, Short.MAX_VALUE);
        public static final Param CIRCULAR = new Param(Short.MIN_VALUE, Short.MAX_VALUE, true);

        public static final Param POSITIVE = new Param(0, Short.MAX_VALUE);
        public static final Param POSITIVE_NO_ZERO = new Param(1, Short.MAX_VALUE);

        public static final Param NEGATIVE = new Param(Short.MIN_VALUE, -1);
        public static final Param NEGATIVE_W_ZERO = new Param(Short.MIN_VALUE, 0);

        public static final Param BOOLEAN = new Param(0, 1);
        public static final Param BOOLEAN_NEG = new Param(-1, 0);

        public final short min;
        public final short max;
        public final boolean circular;

        public Param(short currentValue) {
            this(currentValue, false);
        }

        public Param(short currentValue, final boolean circular) {
            this.min = currentValue < 0 ? (short)(Short.MIN_VALUE - currentValue) : Short.MIN_VALUE;
            this.max = currentValue > 0 ? (short)(Short.MAX_VALUE - currentValue) : Short.MAX_VALUE;
            this.circular = circular;
        }

        public Param(final short min, final short max) {
            this(min, max, false);
        }

        public Param(final short min, final short max, final boolean circular) {
            this.min = min;
            this.max = max;
            this.circular = circular;
        }

        public Param(final int min, final int max) {
            this(min, max, false);
        }

        public Param(final int min, final int max, final boolean circular) {
            if (Math.min(min, max) < Short.MIN_VALUE || Math.max(min, max) > Short.MAX_VALUE) {
                throw new IllegalArgumentException();
            }
            this.min = (short)min;
            this.max = (short)max;
            this.circular = circular;
        }
    }

    public static final double LOG4 = Math.log(4);

    /**
     * Must have an even number of arguments.  First short is the current value, second
     * is the value to achieve, etc...
     *
     * @param values
     * @return
     * @throws ArrayIndexOutOfBoundsException if there are an odd number of arguments
     */
    public static short[] toAchieve(short ... values) throws ArrayIndexOutOfBoundsException {
        return toAchieve(new short[values.length / 2], values);
    }

    public static short[] toAchieve(short[] result, short ... values) throws ArrayIndexOutOfBoundsException {
        for (int i = 0; i < values.length; i += 2) {
            int diff = values[i + 1] - values[i];
            if (diff <= Short.MIN_VALUE) diff = Short.MIN_VALUE;
            else if (diff >= Short.MAX_VALUE) diff = Short.MAX_VALUE;
            result[i / 2] = (short)diff;
        }

        return result;
    }

    public static short[] toAchieve(int ... values) {
        return toAchieve(new short[values.length / 2], values);
    }

    public static short[] toAchieve(short[] result, int ... values) throws ArrayIndexOutOfBoundsException {
        for (int i = 0; i < values.length; i += 2) {
            int diff = values[i + 1] - values[i];
            if (diff <= Short.MIN_VALUE) diff = Short.MIN_VALUE;
            else if (diff >= Short.MAX_VALUE) diff = Short.MAX_VALUE;
            result[i / 2] = (short)diff;
        }

        return result;
    }

    public static double transformByMagnitudeOnly(double currentValue, short magnitudeParam) {
        if (currentValue == 0 || !Double.isFinite(currentValue)) throw new IllegalArgumentException(currentValue + "");

        double exponent = magnitudeParam;

        if (exponent > HALF_MAX_PLUS_ONE) {
            // wait until a larger positive value to offset by one, so small increments can be made at the lower values
            exponent++;
        }

        return currentValue * Math.pow(4.0, exponent /  MAX_PLUS_ONE);
    }

    public static short[] toAchieveByMagnitudeOnly(double ... values)
            throws ArrayIndexOutOfBoundsException {

        short[] result = new short[values.length / 2];

        for (int i = 0; i < values.length; i += 2) {
            double diff = Math.log(values[i + 1] / values[i]) / LOG4 * MAX_PLUS_ONE;

            if (diff > HALF_MAX_PLUS_ONE + 1) {
                diff--; //offset adjustment, reverse of transformByMagnitudeAndSign
            }

            result[i] = roundClip(diff);
        }

        return result;
    }


    public static double transformByMagnitudeAndSign(double currentValue, short magnitudeParam, short sign)
            throws IllegalArgumentException {

        if (currentValue == 0 || !Double.isFinite(currentValue)) throw new IllegalArgumentException(currentValue + "");

        double coefficient = magnitudeParam;

        if (coefficient > HALF_MAX_PLUS_ONE) {
            // wait until a larger positive value to offset by one, so small increments can be made at the lower values
            coefficient++;
        }

        if ((currentValue < 0 && sign == 0) || (currentValue > 0 && sign == -1)) coefficient = -coefficient;
        // negate the magnitude param when the result will be negative, so that LOWER param values produce
        // larger *magnitude* negative numbers (aka, cause the returned value to go down)

        coefficient = Math.pow(4.0, coefficient /  MAX_PLUS_ONE);

        if (sign != 0) {
            if ((sign == 1 && currentValue > 0) || (sign == -1 && currentValue < 0)) {
                currentValue = -currentValue;

            } else {
                throw new IllegalArgumentException();
            }
        }

        return coefficient * currentValue;
    }

    public static short[] toAchieveByMagnitudeAndSign(double ... values)
            throws ArrayIndexOutOfBoundsException {

        return toAchieveByMagnitudeAndSign(new short[values.length], values);
    }

    public static short[] toAchieveByMagnitudeAndSign(short[] result, double ... values)
            throws ArrayIndexOutOfBoundsException {

        for (int i = 0; i < values.length; i += 2) {
            double diff = values[i + 1] / values[i];

            if (diff > 0) {
                //sign param
                result[i + 1] = 0;

                //mag param
                diff = Math.round(Math.log(diff) / LOG4 * MAX_PLUS_ONE);

            } else if (diff < 0) {
                //sign param
                if (values[i] > 0) result[i + 1] = -1;
                else if (values[i] < 0) result[i + 1] = 1;
                else throw new IllegalArgumentException();

                //mag param
                diff = Math.round(Math.log(-diff) / LOG4 * Short.MIN_VALUE);

            } else {
                throw new IllegalArgumentException();
            }

            if (values[i + 1] < 0) {
                diff = -diff;
                // negate the magnitude param when the result will be negative, so that LOWER param values produce
                // larger *magnitude* negative numbers (aka, cause the returned value to go down)
            }

            if (diff > HALF_MAX_PLUS_ONE + 1) {
                diff--; //offset adjustment, reverse of transformByMagnitudeAndSign
            }

            result[i] = clip(diff);
        }

        return result;
    }
}
