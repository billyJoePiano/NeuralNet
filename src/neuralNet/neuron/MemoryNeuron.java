package neuralNet.neuron;

import java.util.*;

/**
 * Handles the issues surrounding hash calculation and recursion
 * @param <M>
 */
public abstract class MemoryNeuron<M extends MemoryNeuron<M>> extends CachingNeuron
        implements SignalProvider.Tweakable<M>, HashCacher {

    public static final long serialVersionUID = -2703258302368792678L;

    /*
    protected MemoryNeuron(M deserializedFrom, Void v) {
        super(deserializedFrom);
    }
     */

    protected MemoryNeuron() {
        super();
    }

    protected MemoryNeuron(CachingNeuron cloneFrom) {
        super(cloneFrom);
    }

    protected MemoryNeuron(List<SignalProvider> inputs) {
        super(inputs);
    }


    protected abstract long calcHash();

    private transient Long hashCache;
    private transient Map<HashCacher, Long> hashesForOriginalInvoker;

    private transient boolean returnZeroHash = false;
    private transient Set<HashCacher> notifyWhenCalculating;
    private transient Set<HashCacher> activelyCalculating;
    private transient Set<HashCacher> downstream;
    private transient HashCacher originalInvoker;
    private transient LinkedHashSet<HashCacher> circularRefs;


    @Override
    public synchronized long getNeuralHash() {
        if (this.returnZeroHash) {
            if (this.notifyWhenCalculating != null) {
                if (this.activelyCalculating == null || this.downstream == null) throw new IllegalStateException();
                this.downstream.forEach(hc -> hc.notifyOfCircularReference(this));
            }

            return 0;
        }

        if (this.hashCache != null
                && (this.activelyCalculating == null || this.originalInvoker == null))
            return this.hashCache;

        if (this.originalInvoker != null && this.hashesForOriginalInvoker != null) {
            if (this.hashesForOriginalInvoker.containsKey(this.originalInvoker)) {
                return this.hashesForOriginalInvoker.get(this.originalInvoker);
            }
        }

        try {
            if (this.notifyWhenCalculating != null) {
                this.notifyWhenCalculating.forEach(hc -> hc.notifyCalculating(this));
                this.downstream = new LinkedHashSet<>();
            }

            this.returnZeroHash = true;
            this.circularRefs = null;

            long hash = this.calcHash();

            if (this.notifyWhenCalculating == null) return hash;
            if (this.originalInvoker == null) throw new IllegalStateException();

            if (this.circularRefs == null || this.originalInvoker == this) return this.hashCache = hash;

            if (this.circularRefs.size() != 1) return hash;
            // if there was more than one neuron finding circular references,
            // the web is too complex to untangle... :-(  We can't cache the hash without risking
            // undefined/faulty behavior
            // (e.g. due to order of upstream invocations for inputs, if inputOrderMatters == false)

            for (HashCacher hc : this.circularRefs) {
                if (hc == this) return this.hashCache = hash;

                if (this.hashesForOriginalInvoker == null) this.hashesForOriginalInvoker = new HashMap<>();
                this.hashesForOriginalInvoker.put(hc, hash);
                break;
                //if hc != originalInvoker, it means that the originalInvoker was just incidental
                // to this the circular reference, and is somewhere upstream of it.  Cache the hash
                // for the neuron actually responsible for the circular reference
            }


            return hash;

        } finally {
            this.returnZeroHash = false;
            this.downstream = null;
            this.circularRefs = null;

            if (this.notifyWhenCalculating != null) {
                this.notifyWhenCalculating.forEach(hc -> hc.notifyDoneCalculating(this));
            }
        }
    }

    @Override
    public void clearHashCache() {
        this.hashCache = null;
        this.clearCalculatingNotifications();
    }

    @Override
    public void notifyWhenCalculating(Set<HashCacher> otherCachers) {
        this.clearHashCache();
        this.notifyWhenCalculating = otherCachers;
        this.activelyCalculating = new LinkedHashSet<>();
    }

    @Override
    public void notifyCalculating(HashCacher calculator) {
        if (this.activelyCalculating == null) throw new IllegalStateException();
        if (this.activelyCalculating.size() == 0) {
            if (this.originalInvoker != null) throw new IllegalStateException();
            this.originalInvoker = calculator;
        }
        this.activelyCalculating.add(calculator);

        if (this.downstream != null) {
            this.downstream.add(calculator);
        }
    }

    @Override
    public void notifyDoneCalculating(HashCacher calculator) {
        this.activelyCalculating.remove(calculator);
        if (this.originalInvoker == null) {
            throw new IllegalStateException();
        }

        boolean sizeZero = this.activelyCalculating.size() == 0;
        boolean isOrig = calculator == this.originalInvoker;

        if (sizeZero != isOrig) { //!!!! should never happen
            for (HashCacher hc : this.notifyWhenCalculating) hc.clearHashCache();
            this.clearHashCache(); //just in case this wasn't in the set...
            throw new IllegalStateException();
        }

        if (sizeZero) this.originalInvoker = null;
    }

    @Override
    public void notifyOfCircularReference(HashCacher calculator) {
        if (this.circularRefs == null) this.circularRefs = new LinkedHashSet<>();
        this.circularRefs.add(calculator);
    }

    @Override
    public void clearCalculatingNotifications() {
        this.hashesForOriginalInvoker = null;
        this.returnZeroHash = false;
        this.notifyWhenCalculating = null;
        this.activelyCalculating = null;
        this.downstream = null;
        this.circularRefs = null;
        this.originalInvoker = null;
    }
}
