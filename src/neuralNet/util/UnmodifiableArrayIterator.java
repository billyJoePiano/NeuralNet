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

}
