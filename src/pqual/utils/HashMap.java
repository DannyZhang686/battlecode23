package pqual.utils;

public class HashMap<K, V> {
    private NewNode<K, V>[] buckets;
    private int numBuckets = 0;

    public HashMap(int capacity) {
        this.buckets = (NewNode<K, V>[]) (new NewNode[capacity]);
        numBuckets = capacity;
    }

    public void insert(K key, V value) {
        NewNode<K, V> entry = new NewNode<>(key, value, null);

        int bucket = Math.abs(key.hashCode()) % numBuckets;

        NewNode<K, V> existing = buckets[bucket];
        if (existing == null) {
            buckets[bucket] = entry;
        } else {
            while (existing.next != null) {
                if (existing.key.equals(key)) {
                    existing.value = value;
                    return;
                }
                existing = existing.next;
            }

            if (existing.key.equals(key)) {
                existing.value = value;
            } else {
                existing.next = entry;
            }
        }
    }

    public V getOrDefault(K key, V default_value) {
        NewNode<K, V> bucket = buckets[Math.abs(key.hashCode()) % numBuckets];

        while (bucket != null) {
            if (bucket.key.equals(key)) {
                return bucket.value;
            }
            bucket = bucket.next;
        }

        return default_value;
    }

    public V get(K key) {
        return this.getOrDefault(key, null);
    }

    public boolean contains(K key) {
        return this.get(key) != null;
    }

    // public void print() {
    // for (int i = 0; i < numBuckets; i++) {
    // NewNode<K, V> curr = this.buckets[i];

    // while (curr != null) {
    // System.out.println("k: " + curr.key + ", v: " + curr.value);
    // curr = curr.next;
    // }
    // }
    // }
}