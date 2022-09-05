package neuralNet.test;

import neuralNet.util.*;

import java.util.concurrent.*;

/**
 * Benchmarked 8-31-2022, Ubuntu desktop 20.04 using IntelliJ with Java17 OpenJDK amd64,
 * on a 2012 MacBook Pro with Intel(R) Core(TM) i7-3615QM CPU @ 2.30GHz
 *
 * Trials suggest that the non-branching approach is very slightly faster.  This result was consistent across
 * multiple trials.  Also discovered that using + was slightly faster than using |
 *
 * Typical times were on the order of 30-33 nanoseconds per invocation for all methods.
 *
 * using |
 *     BRANCHING: 33.163464333862066	31.825913851601804	721.2250760601877
 * NON-BRANCHING: 33.467612550904356	32.09898072981253	776.096821402762
 *
 *     BRANCHING: 32.5585749472181		31.117745142666447	1339.9929224198324
 * NON-BRANCHING: 32.80731062839428		31.368856031116213	1238.1021187788654
 *
 *     BRANCHING: 32.895129611094795	31.457496477355846	1262.9244785731207
 * NON-BRANCHING: 33.07992282013098		31.711987342530627	910.6521202699525
 *
 *
 * using +
 *     BRANCHING: 31.699487461398046	30.27662926916046	1097.9098044507614
 * NON-BRANCHING: 31.709604000051815	30.358159488113717	838.1554936743604
 *
 *     BRANCHING: 31.561817161738873	30.276314625876964	944.1539572443479
 * NON-BRANCHING: 31.665933615217607	30.353516371476854	975.3298558291856
 *
 *     BRANCHING: 32.276594718297325	30.89752806584677	741.8794978729111
 * NON-BRANCHING: 32.295123256742954	30.876603994336666	1091.9892944311725
 *
 *
 * UPDATE 9-1 , testing pre-assignment of 'value' and starting at iteration index 1
 * showed no improvement.  However, these trials indicated NON-BRANCHING was slightly
 * faster... ???
 *
 *
 * Without pre-assignment of 'value'
 *     BRANCHING: 31.19619110102455		29.913511725894374	687.2515955389609
 * NON-BRANCHING: 31.115998941163223	29.811911015167443	1113.0275744949045
 *
 *     BRANCHING: 31.39556411281228		30.062263834796397	1124.088261689398
 * NON-BRANCHING: 31.254821452001732	29.962331134445204	1562.5992796598464
 *
 *     BRANCHING: 31.350997654100258	29.994100060643433	1069.8691246834824
 * NON-BRANCHING: 31.274483125656843	29.98756195563522	885.0810032364112
 *
 *
 * With pre-assignment of 'value'
 *     BRANCHING: 31.784578781574965	30.46142739995273	995.2232828625554
 * NON-BRANCHING: 31.69765009606878		30.303779531958156	1305.044513880356
 *
 *     BRANCHING: 31.89588984971245		30.666448426279874	622.5622238522528
 * NON-BRANCHING: 31.654278005162876	30.327609341104598	1235.7406158786134
 *
 *     BRANCHING: 31.957181381682556	30.655175328448067	636.3745594573129
 * NON-BRANCHING: 31.74881501744191		30.474244176502996	669.0420407997359
 *
 *
 */
public class TestBytesToLong extends Thread {

    public static final int THREADS = 6;
    public static final int CACHE_ARRAY_LEN = 2048;

    public static final int ITERATIONS = CACHE_ARRAY_LEN * THREADS * 65536;

    /**
     * branching approach
     * @param bytes
     * @return
     */
    public static long bytesToLong1(byte[] bytes) {
        if (bytes.length != 8) throw new IllegalArgumentException();

        long value = bytes[0];
        if (value < 0) value += 256;
        //long value = 0;

        for (int i = 1; i < 8; i++) {
            long b = bytes[i];
            if (b < 0) b += 256; //branching approach
            value = (value << 8) + b;
            //value = (value << 8) | b;
        }
        return value;
    }

    /**
     * Non-branching approach
     * @param bytes
     * @return
     */
    public static long bytesToLong2(byte[] bytes) {
        if (bytes.length != 8) throw new IllegalArgumentException();

        long value = bytes[0] & 0xff;
        //long value = 0;

        for (int i = 1; i < 8; i++) {
            value = (value << 8) + (bytes[i] & 0xff);
            //value = (value << 8) | (bytes[i] & 0xff);
        }
        return value;
    }


    private static final Var.Int remaining = new Var.Int(ITERATIONS);
    private static final Var.Int finished = new Var.Int();
    private static final Var<long[][]>[] readyToAdd = new Var[THREADS];
    // each Var serves as a holder for exchanging time arrays between worker thread and main thread, and as the sync monitor

    private static final AccumulatedAverage
            branchingArth = new AccumulatedAverage(),
            nonbranchArth = new AccumulatedAverage(),
            branchingGeo = new AccumulatedAverage(),
            nonbranchGeo = new AccumulatedAverage(),
            branchingRms = new AccumulatedAverage(),
            nonbranchRms = new AccumulatedAverage();

    private static Thread SHUTDOWN_HOOK = new Thread() {
        public void run() {
            System.out.println("    BRANCHING: " + branchingArth.getAverage()
                    + "\t" + Math.exp(branchingGeo.getAverage())
                    + "\t" + Math.sqrt(branchingRms.getAverage()));

            System.out.println("NON-BRANCHING: " + nonbranchArth.getAverage()
                    + "\t" + Math.exp(nonbranchGeo.getAverage())
                    + "\t" + Math.sqrt(nonbranchRms.getAverage()));
        }
    };

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(SHUTDOWN_HOOK);

        bytesToLong1(new byte[8]); //do any class loading that could impact performance?
        bytesToLong2(new byte[8]);
        try { throw new IllegalArgumentException(); }
        catch(IllegalArgumentException e) { }

        int previousFinishedCount = finished.value;
        AvgsCalculator calc = new AvgsCalculator();

        for (int i = 0; i < THREADS; i++) {
            TestBytesToLong thread = new TestBytesToLong();
            readyToAdd[i] = thread.ready;
            thread.start();
        }

        while (previousFinishedCount < ITERATIONS) {

            synchronized (finished) {
                while (finished.value == previousFinishedCount) {
                    try { finished.wait(100); }
                    catch (InterruptedException e) { e.printStackTrace(System.err); }
                }
                if (previousFinishedCount / 1_000_000 != finished.value / 1_000_000) {
                    System.out.println(finished.value + "\t" + remaining.value);
                }
                previousFinishedCount = finished.value;
            }

            for (Var<long[][]> ready : readyToAdd) {
                long[][] cache;

                synchronized (ready) {
                    if (ready.value == null) continue;
                    cache = ready.value;
                    ready.value = null;
                }

                calc.calc(cache);
            }
        }

    }

    private final Var<long[][]> ready = new Var<>();

    private long[][] currentCache = new long[2][CACHE_ARRAY_LEN]; // the one to use for the current iteration
    private long[][] processingByMain = new long[2][CACHE_ARRAY_LEN]; // the last one taken out of the ready Var by main
    private long[][] passedToMain = new long[2][CACHE_ARRAY_LEN]; //the last one put into the ready Var

    private AvgsCalculator calc;

    private TestBytesToLong() { }

    public synchronized void run() {
        try(UniqueAffinityLock af = UniqueAffinityLock.obtain()) {

            ThreadLocalRandom rand = ThreadLocalRandom.current();
            byte[] bytes = new byte[8];

            int c = 0;
            while (checkRemaining()) {
                c++;
                for (int i = 0; i < CACHE_ARRAY_LEN; i++) {
                    rand.nextBytes(bytes);

                    long b, n, startB, endB, startN, endN;

                    if (rand.nextBoolean()) {
                        startB = System.nanoTime();
                        b = bytesToLong1(bytes);
                        endB = System.nanoTime();

                        startN = System.nanoTime();
                        n = bytesToLong2(bytes);
                        endN = System.nanoTime();

                    } else {
                        startN = System.nanoTime();
                        n = bytesToLong2(bytes);
                        endN = System.nanoTime();

                        startB = System.nanoTime();
                        b = bytesToLong1(bytes);
                        endB = System.nanoTime();
                    }

                    if (b != n) throw new IllegalStateException(b + "\tvs\t" + n);

                    this.currentCache[0][i] = endB - startB;
                    this.currentCache[1][i] = endN - startN;
                }

                exchangeCurrentCache();
            }
        }
    }

    private boolean checkRemaining() {
        synchronized (remaining) {
            if (remaining.value > 0) {
                remaining.value -= CACHE_ARRAY_LEN;
                return true;

            } else return false;
        }
    }

    private void exchangeCurrentCache() {
        boolean ready;
        synchronized (this.ready) {
            ready = this.ready.value == null;
            if (ready) {
                this.ready.value = this.currentCache;
                this.currentCache = this.processingByMain;
                this.processingByMain = this.passedToMain;
                this.passedToMain = this.ready.value;
            }
        }

        if (!ready && this.currentCache != null) {
            if (this.calc == null) this.calc = new AvgsCalculator();
            this.calc.calc(this.currentCache);
        }

        synchronized (finished) {
            finished.value += CACHE_ARRAY_LEN;
            finished.notifyAll();
        }
    }

    private static class AvgsCalculator {
        private final AccumulatedAverage arth = new AccumulatedAverage();
        private final AccumulatedAverage geo = new AccumulatedAverage();
        private final AccumulatedAverage rms = new AccumulatedAverage();

        private void reset() {
            this.arth.clear();
            this.geo.clear();
            this.rms.clear();
        }

        private void calc(long[][] currentCache) {

            byte loop = 0;

            for (long[] times : currentCache) {
                this.reset();

                for (long time : times) {
                    if (time < 1)
                        throw new IllegalStateException(time + "");
                    arth.add(time);
                    geo.add(Math.log(time));
                    rms.add(time * time);
                }

                switch (loop++) {
                    case 0 -> addToBranching();
                    case 1 -> addToNonbranch();
                    default -> throw new IllegalArgumentException();
                }
            }
        }

        private void addToBranching() {
            double a = arth.getAverage(), g = geo.getAverage(), r = rms.getAverage();
            synchronized (branchingArth) { branchingArth.add(a); }
            synchronized (branchingGeo) { branchingGeo.add(g); }
            synchronized (branchingRms) { branchingRms.add(r); }
        }

        private void addToNonbranch() {
            double a = arth.getAverage(), g = geo.getAverage(), r = rms.getAverage();
            synchronized (nonbranchArth) { nonbranchArth.add(a); }
            synchronized (nonbranchGeo) { nonbranchGeo.add(g); }
            synchronized (nonbranchRms) { nonbranchRms.add(r); }
        }

    }
}
