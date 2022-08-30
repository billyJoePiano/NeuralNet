package neuralNet.util;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import static neuralNet.util.Util.*;

/**
 * Iterable itself is thread-safe and can produce any number of iterators concurrently, which will
 * all produce different random values.  However, the iterator is NOT THREAD SAFE and should only be
 * used on the thread where the iterator was created
 *
 * @param <N> Type of the number to return
 */
public class RandomNumberIterable<N extends Number> implements Iterable<N> {
    public static final Map<Class<? extends Number>, Function<ThreadLocalRandom, ? extends Number>>
        GENERATORS = makeGenerators();

    private static Map<Class<? extends Number>, Function<ThreadLocalRandom, ? extends Number>> makeGenerators() {
        Function<ThreadLocalRandom, Short>
                sh = rand -> (short)rand.nextInt(Short.MIN_VALUE, MAX_PLUS_ONE_INT);

        Function<ThreadLocalRandom, Integer>
                in = ThreadLocalRandom::nextInt;

        Function<ThreadLocalRandom, Long>
                lg = ThreadLocalRandom::nextLong;

        Function<ThreadLocalRandom, Double>
                db = ThreadLocalRandom::nextDouble;

        Function<ThreadLocalRandom, Float>
                fl = ThreadLocalRandom::nextFloat;

        Function<ThreadLocalRandom, Byte>
                 by = rand -> (byte)rand.nextInt(Byte.MIN_VALUE, 128);


        return Map.ofEntries(
                Map.entry(Short.class, sh), Map.entry(short.class, sh),
                Map.entry(Integer.class, in), Map.entry(int.class, in),
                Map.entry(Long.class, lg), Map.entry(long.class, lg),
                Map.entry(Double.class, db), Map.entry(double.class, db),
                Map.entry(Float.class, fl), Map.entry(float.class, fl),
                Map.entry(Byte.class, by), Map.entry(byte.class, by));
    }

    public static <N extends Number> Function<ThreadLocalRandom, N> makeGenerator(Class<N> type, N min, N max) {
        if (type == Integer.class || type == int.class) {
            int mn = min.intValue(), mx = max.intValue();
            return rand -> (N) Integer.valueOf(rand.nextInt(mn, mx));

        } else if (type == Short.class || type == short.class) {
            int mn = min.intValue(), mx = max.intValue();
            return rand -> (N) Short.valueOf((short)rand.nextInt(mn, mx));

        } else if (type == Long.class || type == long.class) {
            long mn = min.longValue(), mx = max.longValue();
            return rand -> (N) Long.valueOf(rand.nextLong(mn, mx));

        } else if (type == Double.class || type == double.class) {
            double mn = min.doubleValue(), mx = max.doubleValue();
            return rand -> (N) Double.valueOf(rand.nextDouble(mn, mx));

        } else if (type == Float.class || type == float.class) {
            double mn = min.doubleValue(), mx = max.doubleValue();
            return rand -> (N) Float.valueOf((float) rand.nextDouble(mn, mx));

        } else if (type == Byte.class || type == byte.class) {
            int mn = min.intValue(), mx = max.intValue();
            return rand -> (N) Byte.valueOf((byte) rand.nextInt(mn, mx));

        } else throw new IllegalArgumentException();
    }



    public final int count;
    public final Class<N> type;
    Function<ThreadLocalRandom, N> generator;

    public RandomNumberIterable(Class<N> type, int count) {
        if (type == null || !(Number.class.isAssignableFrom(type)) || count <= 0) {
            throw new IllegalArgumentException();
        }
        this.count = count;
        this.type = type;
        this.generator = (Function<ThreadLocalRandom, N>)GENERATORS.get(type);
    }

    public RandomNumberIterable(Class<N> type, int count, N min, N max) {
        if (type == null || !(Number.class.isAssignableFrom(type)) || count <= 0) {
            throw new IllegalArgumentException();
        }
        this.count = count;
        this.type = type;
        this.generator = makeGenerator(type, min, max);
    }



    @Override
    public Iterator iterator() {
        return new Iterator();
    }

    public class Iterator implements java.util.Iterator<N> {
        public final ThreadLocalRandom rand = ThreadLocalRandom.current();
        private int remaining = RandomNumberIterable.this.count;

        private Iterator() { }

        @Override
        public boolean hasNext() {
            return this.remaining >  0;
        }

        @Override
        public N next() {
            if (--this.remaining < 0) throw new NoSuchElementException();
            return RandomNumberIterable.this.generator.apply(rand);
        }
    }
}
