package neuralNet.util;

import net.openhft.affinity.*;

import java.util.*;

public class UniqueAffinityLock implements AutoCloseable {
    private static final Map<Integer, UniqueAffinityLock> AFFINITIES = new TreeMap<>();
    private static final Map<Thread, UniqueAffinityLock> THREADS = new HashMap<>();

    private static final int MAX_ATTEMPTS = 8;
    private static final long WAIT_BETWEEN_ATTEMPTS = 512;
    private static final int ITERATIONS_TO_TEST_UNEXPECTED_AFFINITY = 256;

    public static UniqueAffinityLock obtain() throws RuntimeException {
        Thread current = Thread.currentThread();
        if (THREADS.containsKey(current)) return THREADS.get(current).incrementRequestedCount();

        AffinityLock lock = null;
        boolean keepLock = false;

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) try {
            int cpuId;
            int affCpu;
            boolean testOtherId;
            synchronized (AFFINITIES) {
                int i = 0;
                while (AFFINITIES.containsKey(i)) {
                    i++;
                }

                lock = AffinityLock.acquireLock(i);

                cpuId = lock.cpuId();
                affCpu = Affinity.getCpu();

                if (cpuId == i && affCpu == i) {
                    keepLock = true;
                    return new UniqueAffinityLock(lock, cpuId);
                }

                testOtherId = cpuId == affCpu && cpuId >= 0 && !AFFINITIES.containsKey(cpuId);

                if (testOtherId) {
                    //AFFINITIES.put(cpuId, current);
                    keepLock = true;
                    return new UniqueAffinityLock(lock, cpuId);

                } else {
                    lock.close();
                    lock = null;
                }
            }

            /*
            if (testOtherId) {
                for (int i = 0; i < ITERATIONS_TO_TEST_UNEXPECTED_AFFINITY; i++) {
                    try {
                        Thread.sleep(WAIT_BETWEEN_ATTEMPTS / ITERATIONS_TO_TEST_UNEXPECTED_AFFINITY);

                    } catch (InterruptedException e) {
                        System.err.println(e);
                    }

                    if (cpuId != lock.cpuId() || Affinity.getCpu() != affCpu) {
                        lock.release();
                        lock = null;
                        break;
                    }
                }

                if (lock != null) {
                    keepLock = true;
                    return uniq;
                }
            }

            try {
                Thread.sleep(WAIT_BETWEEN_ATTEMPTS);

            } catch (InterruptedException e) {
                System.err.println(e);
            }
             */


        } finally {
            if (lock != null && !keepLock) {
                lock.release();
            }
        }
        throw new RuntimeException("Could not obtain unique affinity lock");
    }

    public final Thread thread = Thread.currentThread();
    public final int cpuId;

    private final AffinityLock lock;
    private int requestedCount = 1;
    private boolean closed = false;

    private UniqueAffinityLock(AffinityLock lock, int cpuId) {
        synchronized (AFFINITIES) {
            if (AFFINITIES.containsKey(cpuId) || THREADS.containsKey(this.thread)) {
                this.closed = true;
                lock.close();
                throw new IllegalStateException();
            }
            AFFINITIES.put(cpuId, this);
            THREADS.put(this.thread, this);
        }

        this.lock = lock;
        this.cpuId = cpuId;
    }

    private UniqueAffinityLock incrementRequestedCount() {
        this.requestedCount++;
        return this;
    }

    public int getRequestedCount() {
        return this.requestedCount;
    }

    public boolean isClosed() {
        return this.closed;
    }

    public boolean resetAffinity() {
        return lock.resetAffinity();
    }

    public AffinityLock resetAffinity(boolean resetAffinity) {
        return lock.resetAffinity(resetAffinity);
    }

    public void bind() {
        lock.bind();
    }

    public void bind(boolean wholeCore) {
        lock.bind(wholeCore);
    }

    public AffinityLock acquireLock(AffinityStrategy... strategies) {
        return lock.acquireLock(strategies);
    }

    public void forceRelease() {
        try {
            lock.close();

        } finally {
            AFFINITIES.remove(this.cpuId);
            THREADS.remove(this.thread);
        }
    }

    public void release() {
        if (this.requestedCount == 0) return;
        if (--this.requestedCount == 0) {
            this.forceRelease();
        }
    }

    @Override
    public void close() {
        this.release();
    }

    public int cpuId() {
        return lock.cpuId();
    }

    public boolean isAllocated() {
        return lock.isAllocated();
    }

    public boolean isBound() {
        return lock.isBound();
    }

    @Override
    public String toString() {
        return "UniqueAffinityLock(" + lock.toString() + ")";
    }


}