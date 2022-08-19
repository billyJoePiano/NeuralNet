package test;

public class TestDecisionNodeCompareTo {} /* implements DecisionConsumer<TestDecisionNodeCompareTo> {
    public static final int TESTERS_COUNT = 1000000;   //make sure this is always a multiple of 4
    public static final int TEST_SEGMENT = TESTERS_COUNT / 4;
    private static int counter = 0;

    private static final Map<String, Integer> ascended = new HashMap<>();
    private static final Map<String, Integer> descended = new HashMap<>();
    private static final Map<String, SegmentAvgs> avgId = new HashMap<>();

    List<Tester> testers = new ArrayList<>(TESTERS_COUNT);

    public class Tester implements DecisionNode<TestDecisionNodeCompareTo, Tester> {
        public final int id = counter++;
        public final double rand = Math.random();

        @Override
        public short getWeight() {
            return 0;
        }

        @Override
        public boolean executeDecision() {
            return true;
        }

        @Override
        public TestDecisionNodeCompareTo getDecisionConsumer() {
            return TestDecisionNodeCompareTo.this;
        }

        @Override
        public List<SignalProvider> getInputs() {
            return null;
        }

        @Override
        public void setInputs(List<SignalProvider> inputs) { }

    }

    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            System.out.println();
            new TestDecisionNodeCompareTo().run();
            System.out.println();
            System.out.println();

            if (i < 9) doStuffWithJVM();
        }
        System.out.println(ascended);
        System.out.println(descended);

        for (SegmentAvgs segAvgs : avgId.values()) {
            for (int i = 0; i < 4; i++) {
                segAvgs.avgs[i] /= 10;
            }
        }

        System.out.println(avgId.toString().replace("], ", "],\n"));
    }

    private static Runnable[] randActions = new Runnable[] {
            TestDecisionNodeCompareTo::sleepRandom,
            TestDecisionNodeCompareTo::instantiateObjects,
            TestDecisionNodeCompareTo::throwExceptions
    };

    public static void doStuffWithJVM() {
        ThreadLocalRandom tlr = ThreadLocalRandom.current();
        for (int i = tlr.nextInt(1, 5); i > 0; i--) {
            randActions[tlr.nextInt(0, randActions.length)].run();
        }
    }

    @Override
    public List<DecisionNode<TestDecisionNodeCompareTo, ? extends DecisionNode>> getDecisionNodes() {
        return null;
    }

    public void run() {
        counter = 0;
        for (int i = 0; i < TESTERS_COUNT; i++) {
            testers.add(new Tester());
        }
        performRandomnessCheck("Creation order");

        Collections.sort(testers, Comparator.comparingDouble(a -> a.rand));
        performRandomnessCheck("Sorted by random");

        Collections.sort(testers, Comparator.comparingInt(Object::hashCode));
        performRandomnessCheck("Sorted by hash code");

        Collections.sort(testers, Comparator.naturalOrder());
        performRandomnessCheck("Sorted by ActionNode compareTo function");



    }

    public void performRandomnessCheck(String desc) {
        System.out.println(desc);

        for (int segment = 0; segment < 4; segment++) {
            long sum = 0;
            int gtCount = 0;
            int ltCount = 0;

            Integer lastId = null;

            for (int i = segment * TEST_SEGMENT, end = i + TEST_SEGMENT; i < end; i++) {
                Tester tester = testers.get(i);
                if (lastId != null) {
                    if (lastId < tester.id) gtCount++;
                    else ltCount++;
                }
                lastId = tester.id;

                sum += tester.id;
            }

            double avg = (double)sum / TEST_SEGMENT;

            ascended.put(desc, ascended.getOrDefault(desc, 0) + gtCount);
            descended.put(desc, descended.getOrDefault(desc, 0) + ltCount);

            SegmentAvgs segAvgs = avgId.get(desc);
            if (segAvgs == null) avgId.put(desc, segAvgs = new SegmentAvgs());
            segAvgs.avgs[segment] += avg;

            String avgStr = String.format("%.2f", avg);
            String steps = gtCount + " ascended (" + String.format("%.2f", (double)gtCount * 100 / (TEST_SEGMENT - 1)) + "%)\t"
                    + ltCount + " descended (" + String.format("%.2f", (double)ltCount * 100 / (TEST_SEGMENT - 1)) + "%)";

            System.out.println("Segment " + (segment + 1) + ":    Avg Id: " + avgStr + "\t\t" + steps);

        }
        System.out.println();
    }

    private class SegmentAvgs {
        private final double[] avgs = new double[] {0, 0, 0, 0};

        public String toString() {
            return "[" + avgs[0] + ", " + avgs[1] + ", " + avgs[2] + ", " + avgs[3] + "]";
        }
    }


    public static void sleepRandom() {
        try {
            Thread.sleep((long)(Math.random() * 1000));

        } catch (InterruptedException e) {
            System.err.println(e);
        }
    }

    private static final Class[] INSTANTIATABLES = new Class[] {
            Average.class, CircularSoftSwitch.class, Closeness.class, Decrease.class, Deviation.class, Difference.class,
            Equals.class, Farness.class, FixedValue.class, HardSwitch.class, Increase.class, Invert.class, Max.class,
            Min.class, Narrow.class, NotEquals.class, SoftSwitch.class, Uniformity.class, Widen.class, Board.class,
            BoardNet.class
        };

    private static final List<List<Object>> keptObjects = new LinkedList<>();

    public static void instantiateObjects() {
        int number = (int)Math.ceil(Math.pow(2, Math.random() * 8 + 2));
        ThreadLocalRandom tlr = ThreadLocalRandom.current();

        List<Object> list = Math.random() < 0.5 ? new ArrayList<>() : new LinkedList<>();

        while (number-- > 0) {
            Class type = INSTANTIATABLES[tlr.nextInt(0, INSTANTIATABLES.length)];

            Object obj = null;
            try {
                obj = type.getDeclaredConstructor().newInstance();

            } catch (Exception e) {
                System.err.println(e);
            }

            list.add(obj);
        }

        if (Math.random() < 0.3) keptObjects.add(list);
        else if (Math.random() < 0.5) {
            int sleeps = tlr.nextInt(5, 20);

            new Thread(() -> {
                for (int i = 0; i < sleeps; i++) {
                    sleepRandom();
                }
                list.clear(); //remove strong references so it can be garbage collected

                if (Math.random() < 0.5)  System.gc();

            }).start();
        }
    }

    public static void throwExceptions() {
        for (int i = ThreadLocalRandom.current().nextInt(2, 10); i > 0; i--) {
            try {
                exceptionMachineGun();


            } catch (Throwable e) { }
        }
    }

    public static void exceptionMachineGun() throws Throwable {
        Throwable end = null;
        try {
            exceptionMachineGun();

        } catch (Throwable e) {
            end = e;

            int rand = ThreadLocalRandom.current().nextInt();

            if (rand >= -6660 && rand <= 6660) {
                exceptionMachineGun();
            }

        } finally {
            if (end != null) {
                throw end;
            }
        }
    }
}
*/