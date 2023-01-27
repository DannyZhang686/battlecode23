package pqual2.utils;

public class Node<T> {
    public T val;

    public Node<T> next;

    public Node(T obj) {
        val = obj;
        next = null;
    }
}