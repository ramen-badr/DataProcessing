package org.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        int numThreads = args.length > 0 ? Integer.parseInt(args[0]) : 2;
        int delayMs = args.length > 1 ? Integer.parseInt(args[1]) : 1000;
        boolean useArray = args.length > 2 && "array".equalsIgnoreCase(args[2]);

        if (useArray) {
            List<String> list = Collections.synchronizedList(new ArrayList<>());
            for (int i = 0; i < numThreads; i++) new BubbleSorterForArray(list, delayMs).start();
            runConsoleForArray(list);
        } else {
            MyLinkedList list = new MyLinkedList();
            for (int i = 0; i < numThreads; i++) new BubbleSorter(list, delayMs).start();
            runConsoleForLinked(list);
        }
    }

    private static void runConsoleForArray(List<String> list) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Введите строки (пустая строка выводит список и статистику):");
        while (true) {
            String line = sc.nextLine();
            if (line.isEmpty()) {
                List<String> snap;
                synchronized (list) {
                    snap = new ArrayList<>(list);
                }
                for (String s : snap) System.out.println(s);
                System.out.println("Количество шагов: " + Stats.steps.get());
                continue;
            }
            List<String> parts = splitToParts(line);
            synchronized (list) {
                for (int i = parts.size() - 1; i >= 0; i--) {
                    list.addFirst(parts.get(i));
                }
            }
        }
    }

    private static void runConsoleForLinked(MyLinkedList list) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Введите строки (пустая строка выводит список и статистику):");
        while (true) {
            String line = sc.nextLine();
            if (line.isEmpty()) {
                list.printList();
                System.out.println("Количество шагов: " + Stats.steps.get());
                continue;
            }
            List<String> parts = splitToParts(line);
            for (int i = parts.size() - 1; i >= 0; i--) {
                list.addFirst(parts.get(i));
            }
        }
    }

    private static java.util.List<String> splitToParts(String line) {
        java.util.List<String> parts = new ArrayList<>();
        int idx = 0;
        while (idx < line.length()) {
            int end = Math.min(idx + 80, line.length());
            parts.add(line.substring(idx, end));
            idx = end;
        }
        return parts;
    }
}