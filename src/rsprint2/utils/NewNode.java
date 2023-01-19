package rsprint2.utils;

public class NewNode<K, V> {
    final K key;
    V value;
    NewNode<K, V> next;

    public NewNode(K key, V value, NewNode<K, V> next) {
        this.key = key;
        this.value = value;
        this.next = next;
    }
}