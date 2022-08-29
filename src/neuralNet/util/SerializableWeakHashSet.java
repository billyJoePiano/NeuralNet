package neuralNet.util;

import java.io.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

public class SerializableWeakHashSet<T> implements Set<T>, Serializable {
    private enum Placeholder { PLACEHOLDER; }
    private static final Placeholder PLACEHOLDER = Placeholder.PLACEHOLDER;
        // value placeholder to indicate that a key has been set.
        // ... avoids unnecessary map.containsKey() calls during add(), remove(), etc


    private transient WeakHashMap<T, Placeholder> map = new WeakHashMap<>();
    private transient Set<T> set = map.keySet();
    private transient Set<T> view = Collections.unmodifiableSet(this.set);

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        stream.writeObject(new ArrayList<>(this.set));
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        this.map = new WeakHashMap<>();
        this.set = this.map.keySet();
        this.view = Collections.unmodifiableSet(this.set);

        this.addAll((List<T>)stream.readObject());
    }

    public Set<T> getView() {
        return this.view;
    }


    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return set.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return set.iterator();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        set.forEach(action);
    }

    @Override
    public Object[] toArray() {
        return set.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] t1s) {
        return set.toArray(t1s);
    }

    @Override
    public <T1> T1[] toArray(IntFunction<T1[]> generator) {
        return set.toArray(generator);
    }

    @Override
    public boolean add(T t) {
        return map.put(t, PLACEHOLDER) == null;
    }

    @Override
    public boolean remove(Object o) {
        return set.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return set.containsAll(collection);
    }

    @Override
    public boolean addAll(Collection<? extends T> collection) {
        boolean changed = false;
        for (T element : collection) {
            if (map.put(element, PLACEHOLDER) == null) changed = true;
        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        return set.retainAll(collection);
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        return set.removeAll(collection);
    }

    @Override
    public boolean removeIf(Predicate<? super T> filter) {
        return set.removeIf(filter);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Spliterator<T> spliterator() {
        return set.spliterator();
    }

    @Override
    public Stream<T> stream() {
        return set.stream();
    }

    @Override
    public Stream<T> parallelStream() {
        return set.parallelStream();
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null) return false;
        if (!(o instanceof Set set)) return false;
        return this.set.equals(set);
    }

    public int hashCode() {
        return this.set.hashCode();
    }
}
