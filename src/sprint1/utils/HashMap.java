package sprint1.utils;

public class HashMap<K, V> {
    private NewNode<K, V>[] buckets;
    private int numBuckets = 0;
    private int mapSize = 0;

    public HashMap(int capacity) {
        this.buckets = (NewNode<K, V>[]) (new NewNode[capacity]);
        numBuckets = capacity;
    }

    public void insert(K key, V value) {
        NewNode<K, V> entry = new NewNode<>(key, value, null);

        int bucket = key.hashCode() % numBuckets;

        NewNode<K, V> existing = buckets[bucket];
        if (existing == null) {
            buckets[bucket] = entry;
            mapSize++;
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
                mapSize++;
            }
        }
    }

    public V getOrDefault(K key, V default_value) {
        NewNode<K, V> bucket = buckets[key.hashCode() % numBuckets];

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
}