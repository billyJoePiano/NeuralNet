package neuralNet.util;

import java.io.*;
import java.lang.ref.*;

public class SerializableWeakRef<T> extends WeakReference<T> implements Serializable {
    public SerializableWeakRef(T referent) {
        super(referent);
    }

    private Object writeReplace() throws ObjectStreamException {
        return new StrongRef<>(this.get());
    }

    private static class StrongRef<T> implements Serializable { //for serialization purposes only
        private transient boolean used = false;
        private T referent;

        private StrongRef(T referent) {
            this.referent = referent;
        }

        private Object readResolve() throws ObjectStreamException {
            if (this.used) throw new NotActiveException(); //concrete implementation of ObjectStream
            this.used = true;

            SerializableWeakRef<T> weakRef = new SerializableWeakRef<>(this.referent);
            this.referent = null; //this is assuming the ObjectInputStream is maintaining a strong reference to the referent until it is done constructing the complete object graph ...???
            return weakRef;
        }

        private void writeObject(ObjectOutputStream stream) throws IOException {
            if (this.used) throw new NotActiveException(); //concrete implementation of ObjectStreamException, which is a type of IOException
            this.used = true;

            stream.defaultWriteObject();
            this.referent = null;
        }
    }
}
