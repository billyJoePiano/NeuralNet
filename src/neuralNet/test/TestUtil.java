package neuralNet.test;

import neuralNet.function.*;
import neuralNet.neuron.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

public class TestUtil {
    private TestUtil() { throw new UnsupportedOperationException(); }

    public static boolean compareObjects(Object obj1, Object obj2) {
        return compareObjects(obj1, obj2, 0, new HashMap<>());
    }

    private static final String NONEXISTENT_INDEX = "<nonexistent index>";

    public static boolean compareObjects(Object obj1, Object obj2, int depth, Map<Object, Set<Object>> alreadyCompared) {

        Consumer<Object> write = str -> System.out.print("\t".repeat(depth + 1) + str);
        Consumer<Object> writeln = str -> System.out.println("\t".repeat(depth + 1) + str);

        Class type1 = obj1 != null ? obj1.getClass() : null;
        Class type2 = obj2 != null ? obj2.getClass() : null;

        if (type1 != type2) {
            System.out.println("MIS-MATCHED TYPE : \t\t" + type1 + "\tvs\t" + type2); // finishes line started by the caller when recursive
            return false;
        }

        System.out.println(type1); // finishes line started by the caller when recursive
        if (type1 == null) return true;

        writeln.accept(obj1);
        writeln.accept(obj2);


        // prevent infinite recursion before iterating over fields
        Set<Object> obj1Set = alreadyCompared.computeIfAbsent(obj1, o -> new HashSet<>());
        Set<Object> obj2Set = alreadyCompared.computeIfAbsent(obj2, o -> new HashSet<>());

        if (obj1Set.contains(obj2) && obj2Set.contains(obj1)) {
            writeln.accept("<already compared>");
            return true;
        }
        obj1Set.add(obj2);
        obj2Set.add(obj1);

        boolean equals = true;

        for (Field field : getFields(type1)) {
            Object val1, val2;

            try {
                val1 = field.get(obj1);
                val2 = field.get(obj2);


            } catch (IllegalAccessException e) {
                e.printStackTrace(System.err);
                continue;
            }

            if (field.getType().isPrimitive()) {
                writeln.accept(field.getName() + ":\t" + val1 + "\t" + val2);
                equals = equals && Objects.equals(val1, val2);

            } else {
                write.accept(field.getName() + ":\t");
                equals = compareObjects(val1, val2, depth + 2, alreadyCompared) && equals;
            }
        }

        boolean wasPrimitiveArray;

        if (type1.isArray()) {
            obj1 = arrayToList(obj1);
            obj2 = arrayToList(obj2);
            wasPrimitiveArray = type1.getComponentType().isPrimitive();

        }
        else if (obj1 instanceof Iterable) wasPrimitiveArray = false;
        else return equals;

        Iterator i1 = ((Iterable) obj1).iterator();
        Iterator i2 = ((Iterable) obj2).iterator();
        int i = 0;

        while (true) {
            Object val1, val2;
            if (i1.hasNext()) {
                val1 = i1.next();
                if (i2.hasNext()) val2 = i2.next();
                else {
                    val2 = NONEXISTENT_INDEX;
                    equals = false;
                }

            } else if (i2.hasNext()) {
                val1 = NONEXISTENT_INDEX;
                val2 = i2.next();
                equals = false;

            } else break;

            write.accept("[" + (i++) + "]\t");

            if (wasPrimitiveArray) {
                System.out.println(val1 + "\t" + val2);
                equals = equals && Objects.equals(val1, val2);

            } else if (val1 == NONEXISTENT_INDEX) {
                System.out.println();
                writeln.accept("\t" + val1);
                writeln.accept("\t" + val2.getClass());
                writeln.accept("\t" + val2);

            } else if (val2 == NONEXISTENT_INDEX) {
                System.out.println();
                writeln.accept("\t" + val1.getClass());
                writeln.accept("\t" + val1);
                writeln.accept("\t" + val2);

            } else {
                equals = compareObjects(val1, val2, depth + 2, alreadyCompared) && equals;
            }
        }
        return equals;
    }

    private static final List<Class<?>> RECURSE_FOR_PRIVATE_PARENT_FIELDS = List.of(
            SignalConsumer.class,
            FunctionWithInputs.class,
            FunctionNoInputs.class,
            StaticWaveProvider.class //VariableWaveProvider is already covered by SignalConsumer
    );

    public static List<Field> getFields(Class<?> startClass) {
        for (Class<?> parentType : RECURSE_FOR_PRIVATE_PARENT_FIELDS) {
            if (parentType.isAssignableFrom(startClass)) {
                return getFieldsUpTo(startClass, parentType);
            }
        }

        return Arrays.stream(startClass.getFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .collect(Collectors.toList());
    }

    // credit: https://stackoverflow.com/questions/16966629/what-is-the-difference-between-getfields-and-getdeclaredfields-in-java-reflectio
    public static List<Field> getFieldsUpTo(Class<?> startClass, Class<?> inclusiveParent) {

        List<Field> currentClassFields = Arrays.stream(startClass.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .collect(Collectors.toCollection(LinkedList::new));

        for (Field field : currentClassFields) {
            try {
                field.setAccessible(true);

            } catch(Exception e) {
                e.printStackTrace(System.err);
            }
        }

        Class<?> parentClass = startClass.getSuperclass();

        if (parentClass != null && inclusiveParent.isAssignableFrom(parentClass)) {
            currentClassFields.addAll(getFieldsUpTo(parentClass, inclusiveParent));
        }

        return currentClassFields;
    }

    /**
     * Takes an array of any type, including primitives, and converts it into a list.  Arrays of
     * primitives will have the values boxed
     *
     * @param arr
     * @return
     * @throws IllegalArgumentException
     */
    public static List<?> arrayToList(Object arr) throws IllegalArgumentException {
        Class<?> arrType = arr.getClass();

        if (!arrType.isArray()) throw new IllegalArgumentException();

        if (!arrType.getComponentType().isPrimitive()) {
            return Arrays.stream((Object[]) arr).collect(Collectors.toList());
        }

        Class<?> type = arrType.getComponentType();

        if (type == int.class) {
            return Arrays.stream((int[]) arr).boxed().collect(Collectors.toList());

        } else if (type == double.class) {
            return Arrays.stream((double[]) arr).boxed().collect(Collectors.toList());

        } else if (type == long.class) {
            return Arrays.stream((long[]) arr).boxed().collect(Collectors.toList());

        } else if (type == boolean.class) {
            boolean[] a = (boolean[]) arr;
            List<Boolean> list = new ArrayList<>(a.length);
            for (boolean val : a) list.add(val);
            return list;

        } else if (type == short.class) {
            short[] a = (short[]) arr;
            List<Short> list = new ArrayList<>(a.length);
            for (short val : a) list.add(val);
            return list;

        } else if (type == byte.class) {
            byte[] a = (byte[]) arr;
            List<Byte> list = new ArrayList<>(a.length);
            for (byte val : a) list.add(val);
            return list;

        } else if (type == char.class) {
            char[] a = (char[]) arr;
            List<Character> list = new ArrayList<>(a.length);
            for (char val : a) list.add(val);
            return list;

        } else if (type == float.class) {
            float[] a = (float[]) arr;
            List<Float> list = new ArrayList<>(a.length);
            for (float val : a) list.add(val);
            return list;

        } else throw new IllegalStateException();
    }
}
