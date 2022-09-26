package neuralNet.util;

import java.util.*;

public class UnmodifiableArrayIterator<T> implements Iterator<T> {
    private final T[] arr;
    private int index = 0;

    public UnmodifiableArrayIterator(T[] array) {
        if (array == null) throw new NullPointerException();
        this.arr = array;
    }

    @Override
    public boolean hasNext() {
        return this.index < this.arr.length;
    }

    @Override
    public T next() {
        if (this.index == this.arr.length) throw new NoSuchElementException();
        return this.arr[this.index++];
    }

    public static class Long implements Iterator<java.lang.Long> {
        private final long[] arr;
        private int index = 0;

        public Long(long[] array) {
            if (array == null) throw new NullPointerException();
            this.arr = array;
        }

        @Override
        public boolean hasNext() {
            return this.index < this.arr.length;
        }

        @Override
        public java.lang.Long next() {
            if (this.index == this.arr.length) throw new NoSuchElementException();
            return this.arr[this.index++];
        }
    }

}
