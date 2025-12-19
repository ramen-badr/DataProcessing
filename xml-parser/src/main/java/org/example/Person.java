package org.example;

import java.util.*;

public class Person {
    private String id;
    private String firstName;
    private String lastName;
    private String gender;
    private String spouse;
    private List<String> parents = new ArrayList<>();
    private List<Child> children = new ArrayList<>();
    private List<String> siblings = new ArrayList<>();
    private List<String> brothers = new ArrayList<>();
    private List<String> sisters = new ArrayList<>();

    // Вспомогательные поля для валидации
    private Integer childrenNumber;
    private Integer siblingsNumber;

    // Конструкторы, геттеры и сеттеры
    public Person() {}

    public Person(String id) {
        this.id = id;
    }

    // Методы для добавления данных
    public void addParent(String parent) {
        if (parent != null && !parent.isEmpty() && !parent.equals("UNKNOWN")
                && !parent.equals("NONE") && !parents.contains(parent)) {
            parents.add(parent);
        }
    }

    public void addChild(Child child) {
        if (child != null) {
            children.add(child);
        }
    }

    public void addSibling(String sibling) {
        if (sibling != null && !sibling.isEmpty() && !siblings.contains(sibling)) {
            siblings.add(sibling);
        }
    }

    public void addBrother(String brother) {
        if (brother != null && !brother.isEmpty() && !brothers.contains(brother)) {
            brothers.add(brother);
        }
    }

    public void addSister(String sister) {
        if (sister != null && !sister.isEmpty() && !sisters.contains(sister)) {
            sisters.add(sister);
        }
    }

    // Валидация согласованности данных
    public List<String> validate() {
        List<String> issues = new ArrayList<>();

        // Проверка количества детей
        if (childrenNumber != null && children.size() != childrenNumber) {
            issues.add(String.format("Children count mismatch: expected %d, found %d",
                    childrenNumber, children.size()));
        }

        // Проверка количества братьев/сестер
        if (siblingsNumber != null) {
            int totalSiblings = brothers.size() + sisters.size() + siblings.size();
            if (totalSiblings != siblingsNumber) {
                issues.add(String.format("Siblings count mismatch: expected %d, found %d",
                        siblingsNumber, totalSiblings));
            }
        }

        return issues;
    }

    // Геттеры и сеттеры
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getSpouse() { return spouse; }
    public void setSpouse(String spouse) { this.spouse = spouse; }

    public List<String> getParents() { return parents; }
    public void setParents(List<String> parents) { this.parents = parents; }

    public List<Child> getChildren() { return children; }
    public void setChildren(List<Child> children) { this.children = children; }

    public List<String> getSiblings() { return siblings; }
    public void setSiblings(List<String> siblings) { this.siblings = siblings; }

    public List<String> getBrothers() { return brothers; }
    public void setBrothers(List<String> brothers) { this.brothers = brothers; }

    public List<String> getSisters() { return sisters; }
    public void setSisters(List<String> sisters) { this.sisters = sisters; }

    public Integer getChildrenNumber() { return childrenNumber; }
    public void setChildrenNumber(Integer childrenNumber) { this.childrenNumber = childrenNumber; }

    public Integer getSiblingsNumber() { return siblingsNumber; }
    public void setSiblingsNumber(Integer siblingsNumber) { this.siblingsNumber = siblingsNumber; }
}
