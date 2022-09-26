package neuralNet.util;

public class CannotResolveComparisonException extends RuntimeException {
    public final Comparable<?> item1, item2;

    public CannotResolveComparisonException(Comparable<?> item1, Comparable<?> item2) {
        super("Cannot resolve comparison between "
                + typeName((Class<? extends Comparable<?>>) item1.getClass(), (Class<? extends Comparable<?>>) item2.getClass())
                + ":\n\t" + item1 + "\n\t" + item2);

        this.item1 = item1;
        this.item2 = item2;
    }

    private static String typeName(Class<? extends Comparable<?>> type1, Class<? extends Comparable<?>> type2) {
        if (type1 == type2) return "two " + type1.getSimpleName();
        else return type1.getSimpleName() + " and " + type2.getSimpleName();
    }
}
