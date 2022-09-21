package neuralNet.util;

/**
 * BiFunction with return of int primitive.
 *
 * @param <A1>
 * @param <A2>
 */
public interface IntBiFunction<A1, A2> {
    public int apply(A1 arg1, A2 arg2);

    /**
     * IntBiFunction where both arguments are of the same type.  Similar to a Comparator&lt;A&gt;
     *
     * @param <A>
     */
    public interface SameTypes<A> extends IntBiFunction<A, A> { }
}
