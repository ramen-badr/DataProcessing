package org.example;

import java.util.Iterator;

public class MyLinkedList implements Iterable<String> {
    public static class Node {
        String value;
        Node next;
        final Object lock = new Object();

        Node(String value) {
            this.value = value;
        }
    }

    private Node head;

    public void addFirst(String value) {
        Node node = new Node(value);
        synchronized (this) {
            node.next = head;
            head = node;
        }
    }

    public Node getHead() {
        synchronized (this) {
            return head;
        }
    }

    public void setHead(Node newHead) {
        synchronized (this) {
            head = newHead;
        }
    }

    public void printList() {
        Node cur;
        synchronized (this) {
            cur = head;
        }
        while (cur != null) {
            synchronized (cur.lock) {
                System.out.println(cur.value);
                cur = cur.next;
            }
        }
    }

    @Override
    public Iterator<String> iterator() {
        Node start;
        synchronized (this) {
            start = head;
        }
        return new Iterator<>() {
            Node node = start;

            @Override
            public boolean hasNext() {
                return node != null;
            }

            @Override
            public String next() {
                String v = node.value;
                node = node.next;
                return v;
            }
        };
    }
}