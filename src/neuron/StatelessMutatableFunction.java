package neuron;

import java.util.*;

public interface StatelessMutatableFunction<F extends StatelessMutatableFunction<F>> extends StatelessFunction {
    public List<Param> getMutationParams();
    public F mutate(short[] params);
    public short[] getMutationParams(F toAchieve);



    public class Param {
        public static final Param DEFAULT = new Param(Short.MIN_VALUE, Short.MAX_VALUE);

        public static final Param POSITIVE = new Param((short)0, Short.MAX_VALUE);
        public static final Param POSITIVE_NO_ZERO = new Param((short)1, Short.MAX_VALUE);

        public static final Param NEGATIVE = new Param(Short.MIN_VALUE, (short)-1);
        public static final Param NEGATIVE_W_ZERO = new Param(Short.MIN_VALUE, (short)0);

        public static final Param BOOLEAN = new Param((short)0, (short)1);
        public static final Param BOOLEAN_NEG = new Param((short)-1, (short)0);

        public final short min;
        public final short max;

        public Param(short currentValue) {
            this.min = currentValue < 0 ? (short)(Short.MIN_VALUE - currentValue) : Short.MIN_VALUE;
            this.max = currentValue > 0 ? (short)(Short.MAX_VALUE - currentValue) : Short.MAX_VALUE;
        }

        public Param(final short min, final short max) {
            this.min = min;
            this.max = max;
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
        short[] result = new short[values.length / 2];

        for (int i = 0; i < values.length; i += 2) {
            int diff = values[i + 1] - values[i];
            if (diff <= Short.MIN_VALUE) diff = Short.MIN_VALUE;
            else if (diff >= Short.MAX_VALUE) diff = Short.MAX_VALUE;
            result[i / 2] = (short)diff;
        }

        return result;
    }

    public static short[] toAchieve(int ... values) throws ArrayIndexOutOfBoundsException {
        short[] result = new short[values.length / 2];

        for (int i = 0; i < values.length; i += 2) {
            int diff = values[i + 1] - values[i];
            if (diff <= Short.MIN_VALUE) diff = Short.MIN_VALUE;
            else if (diff >= Short.MAX_VALUE) diff = Short.MAX_VALUE;
            result[i / 2] = (short)diff;
        }

        return result;
    }


    public static double transformByMagnitudeAndSign(double currentValue, short magnitudeParam, short sign)
            throws IllegalArgumentException {

        if (currentValue == 0) throw new IllegalArgumentException();

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

    public static short[] toAchieveByMagnitudeAndSign(double ... values) {
        short[] result = new short[values.length / 2];

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

            if (diff > HALF_MAX_PLUS_ONE) {
                diff--; //offset adjustment, reverse of transformByMagnitudeAndSign
            }

            if (diff > Short.MAX_VALUE) result[i] = Short.MAX_VALUE;
            else if (diff < Short.MIN_VALUE) result[i] = Short.MIN_VALUE;
            else result[i] = (short)diff;
        }

        return result;
    }
}
