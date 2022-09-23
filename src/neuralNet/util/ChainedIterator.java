package neuralNet.util;

import java.util.*;

public class ChainedIterator<T> implements Iterator<T> {
    private final Iterator<T>[] iterators;
    private int index = 0;
    Boolean hasNext;

    public ChainedIterator(Iterator<T> ... iterators) {
        if (iterators == null) throw new NullPointerException();
        this.iterators = iterators;
    }

    @Override
    public boolean hasNext() {
        while (this.index < this.iterators.length) {
            if (this.iterators[this.index].hasNext()) {
                return this.hasNext = Boolean.TRUE;
            }
            this.index++;
        }
        return this.hasNext = Boolean.FALSE;
    }

    @Override
    public T next() {
        if (this.hasNext == null) this.hasNext();
        if (!this.hasNext) throw new NoSuchElementException();
        return this.iterators[this.index].next();
    }
}
