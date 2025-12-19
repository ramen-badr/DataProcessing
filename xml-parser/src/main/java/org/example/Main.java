package org.example;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            XMLParser parser = new XMLParser();

            // Парсинг XML файла
            System.out.println("Парсинг XML файла...");
            parser.parse("people.xml");

            // Вывод статистики
            System.out.println("\nСтатистика:");
            System.out.println("Количество уникальных людей: " + parser.getPersons().size());

            // Проверка валидации
            List<String> issues = parser.getValidationIssues();
            if (!issues.isEmpty()) {
                System.out.println("\nПроблемы с согласованностью данных:");
                for (String issue : issues) {
                    System.out.println("  - " + issue);
                }
            } else {
                System.out.println("\nВсе данные согласованы.");
            }

            // Запись объединенного XML
            System.out.println("\nЗапись объединенного XML...");
            parser.writeUnifiedXML("unified_people.xml");

            System.out.println("Готово! Результат записан в unified_people.xml");

            // Пример вывода информации о нескольких людях
            System.out.println("\nПример информации о людях:");
            int count = 0;
            for (Map.Entry<String, Person> entry : parser.getPersons().entrySet()) {
                if (count++ >= 30) break; // Показать только 5 записей

                Person p = entry.getValue();
                System.out.println("\nID: " + (p.getId() != null ? p.getId() : "N/A"));
                System.out.println("Имя: " + (p.getFirstName() != null ? p.getFirstName() : "N/A") +
                        " " + (p.getLastName() != null ? p.getLastName() : ""));
                System.out.println("Детей: " + p.getChildren().size() +
                        (p.getChildrenNumber() != null ? " (ожидается " + p.getChildrenNumber() + ")" : ""));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}