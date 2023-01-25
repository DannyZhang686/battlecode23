package qual.utils;

public class LinkedList<T> {
    public int size;
    public Node<T> head;
    public Node<T> end;

    public LinkedList() {
        size = 0;
        head = null;
        end = null;
    }

    public void add(T obj) {
        if (end != null) {
            Node<T> newNode = new Node<T>(obj);
            end.next = newNode;
            end = newNode;
        } else {
            head = new Node<T>(obj);
            end = head;
        }

        size++;
    }

    public Node<T> dequeue() {
        if (this.size == 0) {
            return null;
        }

        Node<T> removed = head;
        if (end == head) {
            head = end = null;
        } else {
            head = head.next;
        }

        size--;

        return removed;
    }

    public boolean contains(T obj) {
        Node<T> node = head;

        while (node != null) {
            if (node.val.equals(obj)) {
                return true;
            }
            node = node.next;
        }

        return false;
    }

    public boolean remove(T obj) {
        Node<T> node = head;
        Node<T> prev = null;

        while (node != null) {
            if (node.val.equals(obj)) {
                // remove
                if (prev != null) {
                    prev.next = node.next;
                } else if (end == head) {
                    head = end = null;
                } else {
                    head = node.next;
                }

                size--;

                return true;
            }
            prev = node;
            node = node.next;
        }

        return false;
    }
}
