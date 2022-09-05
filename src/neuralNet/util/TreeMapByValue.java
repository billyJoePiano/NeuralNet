package neuralNet.util;

import java.util.*;

public class TreeMapByValue<K, V extends Comparable<? super V>> extends TreeMap<K, V> {
    private static class ValueComparator<K, V extends Comparable<? super V>> implements Comparator<K> {
        private TreeMapByValue<K, V> map;

        @Override
        public int compare(K k1, K k2) {
            V v1 = map.get(k1), v2 = map.get(k2);
            if (v1 == null) return v2 == null ? 0 : 1;
            if (v2 == null) return -1;

            return v1.compareTo(v2);
        }

        private void setMap(TreeMapByValue<K, V> map) {
            assert this.map == null && map != null;
            this.map = map;
        }
    }


    public TreeMapByValue() {
        this(new ValueComparator<K, V>());
    }

    //TODO subclass ValueComparator and as a wrapper for a custom comparator, then make a constructor for TreeMapByValue with custom comparator

    private TreeMapByValue(ValueComparator<K, V> comparator) {
        super(comparator);
        comparator.setMap(this);
    }
}
