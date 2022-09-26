package neuralNet.util;

/**
 * "Fuzzy" in the sense that the result does not need to be strictly true or false, but should return a value
 * between 0.0 and 1.0 indicating relative truth or falsity.
 *
 * @param <T> argument type
 */
public interface FuzzyPredicate<T> {
    public double test(T arg);
}
