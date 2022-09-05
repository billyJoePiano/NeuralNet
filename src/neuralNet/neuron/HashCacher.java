package neuralNet.neuron;

import java.util.*;

/**
 * Use extreme caution in implementing this class.  Only memory neurons and completely self-contained neural
 * networks (aka, not ComplexNeurons) should store a cache of their hash, and this cache should be cleared
 * anytime there is a possibility of the neural pathways changing.
 */
public interface HashCacher extends Neuron {
    public long getNeuralHash();
    public void clearHashCache();

    /**
     * For dealing with circular references in memory neurons, and the "return 0" state, which could cause
     * an incorrect cache to be stored
     * @param otherCachers
     */
    public void notifyWhenCalculating(Set<HashCacher> otherCachers);

    /**
     * Indicates that the recurser is calculating its hash, so the invoked neuron can
     * check for circular references when determining whether to cache the calculated hash
     */
    public void notifyCalculating(HashCacher calculator);

    /**
     * Indicates that the recurser is finished, and subsequent hashes calculated could always be cached
     * (assuming there are no other recursers active that would cause a circular reference)
     */
    public void notifyDoneCalculating(HashCacher calculator);

    /**
     * Tells the invoked neuron not to cache the hash it is currently calculating, because it is within
     * a circular reference
     */
    public void notifyOfCircularReference(HashCacher calculator);

    /**
     * Clear the set of recursers and the stored set of other cachers to notify
     */
    public void clearCalculatingNotifications();
}
