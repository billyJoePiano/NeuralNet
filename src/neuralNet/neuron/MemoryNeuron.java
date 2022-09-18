package neuralNet.neuron;

import java.util.*;

/**
 * Handles the issues surrounding hash calculation and recursion
 * @param <M>
 */
public abstract class MemoryNeuron<M extends MemoryNeuron<M>> extends CachingNeuron
        implements SignalProvider.Tweakable<M>, LoopingNeuron {

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

    private transient Long hashCache;
    private transient Map<LoopingNeuron, Long> hashesForOriginalInvoker;

    private transient Boolean loopsBack;
    private transient Set<LoopingNeuron> gettingHashFor;
    private transient Set<LoopingNeuron> checkingForLoops;


    public synchronized long getNeuralHashFor(LoopingNeuron looper) {
        if (looper == this) return 0;
        else if (looper == null && this.loopsBack()) {
            if (this.hashCache != null) return this.hashCache;
            else return this.hashCache = this.calcNeuralHashFor(this);
        }

        if (this.gettingHashFor == null) {
            if (this.loopsBack()) this.gettingHashFor = new HashSet<>();
            else return super.getNeuralHashFor(looper);

        } else if (this.gettingHashFor.contains(looper)) return 0;

        try {
            this.gettingHashFor.add(looper);
            return super.getNeuralHashFor(looper);

        } catch(StackOverflowError e) {
            e.printStackTrace(System.err);
            return 0;

        } finally {
            this.gettingHashFor.remove(looper);
            if (this.gettingHashFor.size() == 0) this.gettingHashFor = null;
        }
    }

    @Override
    public void clearHashCache() {
        this.loopsBack = null;
        super.clearHashCache();
    }

    @Override
    public synchronized boolean loopsBack() {
        if (this.loopsBack != null) return this.loopsBack;
        return this.loopsBack = super.checkForLoops(this);
    }

    @Override
    public synchronized boolean checkForLoops(LoopingNeuron looper) {
        if (looper == this) return true;
        if (this.noLoops != null && this.noLoops.contains(looper)) return false;
        if (this.loops != null && this.loops.contains(looper)) return true;


        if (this.checkingForLoops != null) {
            if (this.checkingForLoops.contains(looper)) return false;

        } else this.checkingForLoops = new HashSet<>();

        try {
            this.checkingForLoops.add(looper);

            for (SignalProvider provider : this.inputs) {
                if (provider.checkForLoops(looper)) {
                    if (this.loops == null) this.loops = new HashSet<>();
                    this.loops.add(looper);
                    return true;
                }
            }

            if (this.noLoops == null) this.noLoops = new HashSet<>();
            this.noLoops.add(looper);
            return false;


        } finally {
            this.checkingForLoops.remove(looper);
            if (this.checkingForLoops.size() == 0) this.checkingForLoops = null;
        }
    }
}
