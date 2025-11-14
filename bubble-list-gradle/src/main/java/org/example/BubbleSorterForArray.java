package org.example;

import java.util.List;

public class BubbleSorterForArray extends Thread {
    private final List<String> list;
    private final int delayMs;

    public BubbleSorterForArray(List<String> list, int delayMs) {
        this.list = list;
        this.delayMs = delayMs;
    }

    @Override
    public void run() {
        while (true) {
            int i = 0;
            while (true) {
                String left, right;

                synchronized (list) {
                    int n = list.size();
                    if (n < 2 || i >= n - 1) break;
                    Stats.steps.incrementAndGet();
                    left = list.get(i);
                    right = list.get(i + 1);

                    if (left.compareTo(right) > 0) {
                        list.set(i, right);
                        list.set(i + 1, left);
                    }
                }

                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ignored) {
                }

                i++;
            }
        }
    }
}