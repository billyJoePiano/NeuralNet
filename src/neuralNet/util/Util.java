package neuralNet.util;

import neuralNet.neuron.*;

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
    public static final int MAX_PLUS_ONE_INT = (int)Short.MAX_VALUE + 1;
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

    public static final double BILLION = 1_000_000_000.0;
    public static final double MILLION = 1_000_000.0;
    public static final long BILLION_LONG = 1_000_000_000L;

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

    public static void syncMaps(Map<SignalProvider, SignalProvider> providersMap,
                         Map<SignalConsumer, SignalConsumer> consumersMap) {

        for (SignalProvider provider : providersMap.keySet()) {
            if (!(provider instanceof SignalConsumer oldConsumer)) continue;
            if (consumersMap.containsKey(oldConsumer)) {
                if (consumersMap.get(oldConsumer) != providersMap.get(provider))
                    throw new IllegalArgumentException("providersMap and consumersMap must map the same key to the same value");

            } else {
                SignalProvider newProvider = providersMap.get(provider);
                if (!(newProvider instanceof SignalConsumer newConsumer)) throw new IllegalStateException();
                consumersMap.put(oldConsumer, newConsumer);
            }
        }

        for (SignalConsumer consumer : consumersMap.keySet()) {
            if (!(consumer instanceof SignalProvider oldProvider)) continue;
            if (providersMap.containsKey(oldProvider))
                continue; //we already verified they are the same on the above loop, no need to check again
            SignalConsumer newConsumer = consumersMap.get(consumer);
            if (!(newConsumer instanceof SignalProvider newProvider)) throw new IllegalStateException();
            providersMap.put(oldProvider, newProvider);
        }
    }

    public static Map<SignalConsumer, SignalConsumer> convertProvidersMap(Map<SignalProvider, SignalProvider> providersMap) {
        Map<SignalConsumer, SignalConsumer> consumersMap = new HashMap<>();
        for (SignalProvider provider : providersMap.keySet()) {
            if (!(provider instanceof SignalConsumer oldConsumer)) continue;
            SignalProvider newProvider = providersMap.get(provider);
            if (!(newProvider instanceof SignalConsumer newConsumer)) throw new IllegalStateException();
            consumersMap.put(oldConsumer, newConsumer);
        }
        return consumersMap;
    }

    public static Map<SignalProvider, SignalProvider> convertConsumersMap(Map<SignalConsumer, SignalConsumer> consumersMap) {
        Map<SignalProvider, SignalProvider> providersMap = new HashMap<>();
        for (SignalConsumer consumer : consumersMap.keySet()) {
            if (!(consumer instanceof SignalProvider oldProvider)) continue;
            SignalConsumer newConsumer = consumersMap.get(consumer);
            if (!(newConsumer instanceof SignalProvider newProvider)) throw new IllegalStateException();
            providersMap.put(oldProvider, newProvider);
        }
        return providersMap;
    }

    /**
     * Removes all elements from the two sets of consumers and providers if they are not also found in the other set,
     * EXCEPT when the element is not instanceof both SignalConsumer and SignalProvider
     */
    public static void intersectionFilterTypeAware(Set<SignalProvider> providers, Set<SignalConsumer> consumers) {
        for (Iterator<SignalConsumer> iterator = consumers.iterator();
                iterator.hasNext();) {

            SignalConsumer consumer = iterator.next();
            if (consumer instanceof SignalProvider provider) {
                if (!providers.contains(provider)) iterator.remove();
            }
        }

        for (Iterator<SignalProvider> iterator = providers.iterator();
                iterator.hasNext();) {

            SignalProvider provider = iterator.next();
            if (provider instanceof SignalConsumer consumer) {
                if (!consumers.contains(consumer)) iterator.remove();
            }
        }
    }

    public static boolean containsOne(Collection<?> c1, Collection<?> c2) {
        for (Object obj : c1) {
            if (c2.contains(obj)) return true;
        }
        return false;
    }

    public static String leftFill(String string, char fill, int length) {
        int len = string.length();
        if (len >= length) return string;
        return String.valueOf(fill).repeat(length - len) + string;
    }

    public static String rightFill(String string, char fill, int length) {
        int len = string.length();
        if (len >= length) return string;
        return string + String.valueOf(fill).repeat(length - len);
    }

    public static String leftFill(String string, String fill, int length) {
        int len = string.length();
        if (len >= length) return string;
        len = length - len;
        int repeat = len / fill.length();
        int mod = len % fill.length();

        if (mod == 0) return fill.repeat(repeat) + string;
        else return fill.repeat(repeat) + fill.substring(0, mod) + string;
    }

    public static String rightFill(String string, String fill, int length) {
        int len = string.length();
        if (len >= length) return string;
        len = length - len;
        int repeat = len / fill.length();
        int mod = len % fill.length();

        if (mod == 0) return string + fill.repeat(repeat);
        else return string + fill.substring(0, mod) + fill.repeat(repeat);
    }

    public static Map mapOf(Object ... keysValues) {
        if ((keysValues.length & 0b1) == 1) throw new IllegalArgumentException("must have even number of args");

        Map map = new LinkedHashMap<>();
        for (int i = 0; i < keysValues.length; i += 2) {
            map.put(keysValues[i], keysValues[i + 1]);
        }
        return Collections.unmodifiableMap(map);
    }

    public static <K, V> Map<K, V> mapOf(Class<K> keyType, Class<V> valueType, Object ... keysValues) {
        if ((keysValues.length & 0b1) == 1) throw new IllegalArgumentException("must have even number of args");

        Map<K, V> map = new LinkedHashMap<>();
        for (int i = 0; i < keysValues.length; i += 2) {
            Object key = keysValues[i];
            Object value = keysValues[i + 1];
            assert keyType.isInstance(key) && valueType.isInstance(value);
            map.put((K)key, (V)value);
        }
        return Collections.unmodifiableMap(map);
    }

    public static String toString(short[] arr) {
        StringBuilder str = new StringBuilder();
        str.append('[');
        boolean first = true;
        for (short val : arr) {
            if (first) first = false;
            else str.append(", ");
            str.append(val);
        }

        str.append(']');
        return str.toString();
    }

    public static String toString(int[] arr) {
        StringBuilder str = new StringBuilder();
        str.append('[');
        boolean first = true;
        for (int val : arr) {
            if (first) first = false;
            else str.append(", ");
            str.append(val);
        }

        str.append(']');
        return str.toString();
    }

    public static <T> String toString(T[] arr) {
        StringBuilder str = new StringBuilder();
        str.append('[');
        boolean first = true;
        for (T val : arr) {
            if (first) first = false;
            else str.append(", ");
            str.append(val.toString());
        }

        str.append(']');
        return str.toString();
    }

    public static boolean equals(long[] arr1, long[] arr2) {
        if (arr1 == null) return arr2 == null;
        else if (arr2 == null) return false;

        if (arr1.length != arr2.length) return false;

        for (int i = 0; i < arr1.length; i++) {
            if (arr1[i] != arr2[i]) return false;
        }
        return true;
    }


    // NOT FINISHED!
    public static long[] makeBitmask(boolean ... values) {
        if (values == null ) return null;
        else if (values.length == 0) return new long[0];

        int lastSize = (values.length - 1) % 64 + 1;
        int size = (values.length - lastSize) / 64 + 1;

        long[] result = new long[size];

        for (int r = size - 1; r != -1; r--) {
            int i = values.length - 1 - 64 * (size - r) + lastSize;

        }

        for (int i = values.length - 1, r = size - 1, c = 64;;) {
            result[r] |= values[i--] ? 0b1 : 0b0;

            if (i == -1) break;
            result[r] <<= 1;
    //TODO

        }
        return null;
    }

    public static int count(boolean[] arr) {
        int count = 0;
        for (int i = 0; i < arr.length; i ++) {
            if (arr[i]) count++;
        }
        return count;
    }

    public static int pow2(int exponent) {
        if (exponent > 30 || exponent < 0) throw new ArithmeticException();
        return 0b1 << exponent;
    }
}
