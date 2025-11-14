package org.example;

public class BubbleSorter extends Thread {
    private final MyLinkedList list;
    private final int delayMs;

    public BubbleSorter(MyLinkedList list, int delayMs) {
        this.list = list;
        this.delayMs = delayMs;
    }

    @Override
    public void run() {
        while (true) {
            MyLinkedList.Node prev = null;
            MyLinkedList.Node current = list.getHead();

            while (current != null && current.next != null) {
                MyLinkedList.Node next = current.next;
                boolean swappedHere = false;
                boolean structureChanged = false;

                if (prev != null) {
                    synchronized (prev.lock) {
                        synchronized (current.lock) {
                            synchronized (next.lock) {
                                if (prev.next != current || current.next != next) {
                                    structureChanged = true;
                                } else {
                                    Stats.steps.incrementAndGet();
                                    if (current.value.compareTo(next.value) > 0) {
                                        current.next = next.next;
                                        next.next = current;
                                        prev.next = next;
                                        swappedHere = true;
                                    }
                                }
                            }
                        }
                    }
                    if (structureChanged) {
                        prev = null;
                        current = list.getHead();
                        continue;
                    }
                    if (swappedHere) {
                        prev = prev.next;
                        current = prev.next;
                    } else {
                        prev = current;
                        current = current.next;
                    }
                } else {
                    synchronized (current.lock) {
                        synchronized (next.lock) {
                            if (list.getHead() != current || current.next != next) {
                                structureChanged = true;
                            } else {
                                Stats.steps.incrementAndGet();
                                if (current.value.compareTo(next.value) > 0) {
                                    current.next = next.next;
                                    next.next = current;
                                    list.setHead(next);
                                    swappedHere = true;
                                }
                            }
                        }
                    }
                    if (structureChanged) {
                        current = list.getHead();
                        continue;
                    }
                    if (swappedHere) {
                        prev = list.getHead();
                        current = (prev == null) ? null : prev.next;
                    } else {
                        prev = current;
                        current = current.next;
                    }
                }

                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }
}