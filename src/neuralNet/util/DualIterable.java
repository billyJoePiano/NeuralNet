package neuralNet.util;

public class DualIterable<T> implements Iterable<DualIterable.Pair<T>> {
    public final Iterable<T> iterable1;
    public final Iterable<T> iterable2;

    public DualIterable(Iterable<T> iterable1, Iterable<T> iterable2) {
        this.iterable1 = iterable1;
        this.iterable2 = iterable2;
    }

    @Override
    public Iterator iterator() {
        return new Iterator();
    }

    public class Iterator implements java.util.Iterator<Pair<T>> {
        public final java.util.Iterator<T> iterator1 = DualIterable.this.iterable1.iterator();
        public final java.util.Iterator<T> iterator2 = DualIterable.this.iterable2.iterator();

        private int index = 0;

        @Override
        public boolean hasNext() {
            return iterator1.hasNext() || iterator2.hasNext();
        }

        @Override
        public Pair next() {
            T val1 = iterator1.hasNext() ? iterator1.next() : null;
            T val2 = iterator2.hasNext() ? iterator2.next() : null;
            return new Pair(this.index++, val1, val2);
        }
    }

    public record Pair<T>(int index, T value1, T value2) { }
}
