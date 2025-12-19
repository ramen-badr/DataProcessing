package org.example;

import javax.xml.stream.*;
import java.io.*;
import java.util.*;

public class XMLParser {
    private Map<String, Person> persons = new HashMap<>();
    private List<String> validationIssues = new ArrayList<>();

    public void parse(String filePath) throws Exception {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLStreamReader reader = factory.createXMLStreamReader(new FileInputStream(filePath));

        Person currentPerson = null;
        StringBuilder textContent = new StringBuilder();

        while (reader.hasNext()) {
            int event = reader.next();

            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    String elementName = reader.getLocalName();

                    if ("person".equals(elementName)) {
                        currentPerson = new Person();

                        // Получаем id из атрибута
                        String id = reader.getAttributeValue(null, "id");
                        if (id != null) {
                            currentPerson.setId(id);
                        }

                        // Получаем name из атрибута
                        String name = reader.getAttributeValue(null, "name");
                        if (name != null) {
                            // Пытаемся извлечь имя из атрибута name
                            String[] nameParts = name.split(" ");
                            if (nameParts.length > 0) {
                                currentPerson.setFirstName(nameParts[0]);
                                if (nameParts.length > 1) {
                                    currentPerson.setLastName(nameParts[nameParts.length - 1]);
                                }
                            }
                        }

                        // Обрабатываем атрибуты, которые могут быть у элемента person
                        processAttributes(currentPerson, elementName, reader);
                    } else if (currentPerson != null) {
                        // Обрабатываем атрибуты для других элементов
                        processAttributes(currentPerson, elementName, reader);
                    }
                    break;

                case XMLStreamConstants.CHARACTERS:
                    textContent.append(reader.getText());
                    break;

                case XMLStreamConstants.END_ELEMENT:
                    elementName = reader.getLocalName();
                    String text = textContent.toString().trim();
                    textContent.setLength(0);

                    if ("person".equals(elementName) && currentPerson != null) {
                        savePerson(currentPerson);
                        currentPerson = null;
                    } else if (currentPerson != null) {
                        processElementText(currentPerson, elementName, text);
                    }
                    break;
            }
        }
        reader.close();
    }

    private void processAttributes(Person person, String elementName, XMLStreamReader reader) {
        switch (elementName) {
            case "person":
                // Уже обработано выше
                break;

            case "siblings":
                String siblingsVal = reader.getAttributeValue(null, "val");
                if (siblingsVal != null) {
                    String[] siblingIds = siblingsVal.split(" ");
                    for (String siblingId : siblingIds) {
                        if (!siblingId.isEmpty()) {
                            person.addSibling(siblingId);
                        }
                    }
                }
                break;

            case "child":
            case "son":
            case "daughter":
                String childId = reader.getAttributeValue(null, "id");
                if (childId != null && !childId.isEmpty()) {
                    Child child = new Child();
                    child.setId(childId);
                    child.setType(elementName);
                    person.addChild(child);
                }
                break;

            case "id":
                String idValue = reader.getAttributeValue(null, "value");
                if (idValue != null && !idValue.isEmpty()) {
                    person.setId(idValue);
                }
                break;

            case "firstname":
                String firstnameValue = reader.getAttributeValue(null, "value");
                if (firstnameValue != null && !firstnameValue.isEmpty()) {
                    person.setFirstName(firstnameValue);
                }
                break;

            case "surname":
                String surnameValue = reader.getAttributeValue(null, "value");
                if (surnameValue != null && !surnameValue.isEmpty()) {
                    person.setLastName(surnameValue);
                }
                break;

            case "gender":
                String genderValue = reader.getAttributeValue(null, "value");
                if (genderValue != null && !genderValue.isEmpty()) {
                    person.setGender(genderValue);
                }
                break;

            case "spouce":
            case "wife":
            case "husband":
                String spouseValue = reader.getAttributeValue(null, "value");
                if (spouseValue != null && !spouseValue.isEmpty() && !spouseValue.equals("NONE")) {
                    person.setSpouse(spouseValue);
                }
                break;

            case "parent":
                String parentValue = reader.getAttributeValue(null, "value");
                if (parentValue != null && !parentValue.isEmpty() && !parentValue.equals("UNKNOWN")) {
                    person.addParent(parentValue);
                }
                break;
        }
    }

    private void processElementText(Person person, String elementName, String text) {
        if (text.isEmpty()) {
            return;
        }

        switch (elementName) {
            case "id":
                person.setId(text);
                break;

            case "firstname":
            case "first":
                person.setFirstName(text);
                break;

            case "surname":
            case "family":
            case "family-name":
                person.setLastName(text);
                break;

            case "gender":
                String gender = text.equals("M") ? "male" :
                        text.equals("F") ? "female" : text;
                person.setGender(gender);
                break;

            case "spouce":
            case "wife":
            case "husband":
                if (!text.equals("NONE")) {
                    person.setSpouse(text);
                }
                break;

            case "mother":
            case "father":
            case "parent":
                if (!text.equals("UNKNOWN")) {
                    person.addParent(text);
                }
                break;

            case "children-number":
                try {
                    person.setChildrenNumber(Integer.parseInt(text));
                } catch (NumberFormatException e) {
                    // Игнорируем некорректные числа
                }
                break;

            case "siblings-number":
                try {
                    person.setSiblingsNumber(Integer.parseInt(text));
                } catch (NumberFormatException e) {
                    // Игнорируем некорректные числа
                }
                break;

            case "brother":
                person.addBrother(text);
                break;

            case "sister":
                person.addSister(text);
                break;

            case "child":
            case "son":
            case "daughter":
                // Проверяем, не был ли уже добавлен через атрибут
                boolean alreadyAdded = false;
                for (Child child : person.getChildren()) {
                    if (child.getId() == null && child.getName() != null && child.getName().equals(text)) {
                        alreadyAdded = true;
                        break;
                    }
                }

                if (!alreadyAdded) {
                    Child child = new Child();
                    child.setType(elementName);
                    child.setName(text);
                    person.addChild(child);
                }
                break;
        }
    }

    private void savePerson(Person person) {
        String key = person.getId();

        if (key == null || key.isEmpty()) {
            // Если нет ID, создаем ключ из имени
            key = (person.getFirstName() != null ? person.getFirstName() : "") +
                    (person.getLastName() != null ? person.getLastName() : "");
            if (key.isEmpty()) {
                key = "unknown_" + UUID.randomUUID().toString().substring(0, 8);
            }
        }

        if (persons.containsKey(key)) {
            // Объединяем данные
            Person existing = persons.get(key);
            mergePersons(existing, person);
        } else {
            persons.put(key, person);
        }

        // Валидация
        List<String> issues = person.validate();
        if (!issues.isEmpty()) {
            validationIssues.add("Person " + key + ": " + String.join("; ", issues));
        }
    }

    private void mergePersons(Person target, Person source) {
        if (source.getFirstName() != null && !source.getFirstName().isEmpty()) {
            target.setFirstName(source.getFirstName());
        }

        if (source.getLastName() != null && !source.getLastName().isEmpty()) {
            target.setLastName(source.getLastName());
        }

        if (source.getGender() != null && !source.getGender().isEmpty()) {
            target.setGender(source.getGender());
        }

        if (source.getSpouse() != null && !source.getSpouse().isEmpty()) {
            target.setSpouse(source.getSpouse());
        }

        // Объединение родителей
        for (String parent : source.getParents()) {
            target.addParent(parent);
        }

        // Объединение детей
        for (Child child : source.getChildren()) {
            target.addChild(child);
        }

        // Объединение братьев/сестер
        for (String sibling : source.getSiblings()) {
            target.addSibling(sibling);
        }
        for (String brother : source.getBrothers()) {
            target.addBrother(brother);
        }
        for (String sister : source.getSisters()) {
            target.addSister(sister);
        }

        // Объединение числовых полей
        if (source.getChildrenNumber() != null) {
            target.setChildrenNumber(source.getChildrenNumber());
        }

        if (source.getSiblingsNumber() != null) {
            target.setSiblingsNumber(source.getSiblingsNumber());
        }
    }

    public void writeUnifiedXML(String outputPath) throws Exception {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        XMLStreamWriter writer = factory.createXMLStreamWriter(
                new FileWriter(outputPath));

        writer.writeStartDocument("UTF-8", "1.0");
        writer.writeStartElement("people");
        writer.writeAttribute("count", String.valueOf(persons.size()));

        for (Person person : persons.values()) {
            writer.writeStartElement("person");

            if (person.getId() != null && !person.getId().isEmpty()) {
                writer.writeAttribute("id", person.getId());
            }

            // Основная информация
            if (person.getFirstName() != null && !person.getFirstName().isEmpty()) {
                writer.writeStartElement("firstname");
                writer.writeCharacters(person.getFirstName());
                writer.writeEndElement();
            }

            if (person.getLastName() != null && !person.getLastName().isEmpty()) {
                writer.writeStartElement("lastname");
                writer.writeCharacters(person.getLastName());
                writer.writeEndElement();
            }

            if (person.getGender() != null && !person.getGender().isEmpty()) {
                writer.writeStartElement("gender");
                writer.writeCharacters(person.getGender());
                writer.writeEndElement();
            }

            if (person.getSpouse() != null && !person.getSpouse().isEmpty()) {
                writer.writeStartElement("spouse");
                writer.writeCharacters(person.getSpouse());
                writer.writeEndElement();
            }

            // Родители
            if (!person.getParents().isEmpty()) {
                writer.writeStartElement("parents");
                for (String parent : person.getParents()) {
                    writer.writeStartElement("parent");
                    writer.writeCharacters(parent);
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }

            // Дети (структурированные)
            if (!person.getChildren().isEmpty()) {
                writer.writeStartElement("children");

                for (Child child : person.getChildren()) {
                    writer.writeStartElement("child");

                    if (child.getType() != null) {
                        writer.writeAttribute("type", child.getType());
                    }

                    if (child.getId() != null && !child.getId().isEmpty()) {
                        writer.writeAttribute("id", child.getId());
                    }

                    if (child.getName() != null && !child.getName().isEmpty()) {
                        writer.writeCharacters(child.getName());
                    }

                    writer.writeEndElement();
                }

                writer.writeEndElement();
            }

            // Братья и сестры (структурированные)
            if (!person.getBrothers().isEmpty() || !person.getSisters().isEmpty() || !person.getSiblings().isEmpty()) {
                writer.writeStartElement("siblings");

                for (String brother : person.getBrothers()) {
                    writer.writeStartElement("brother");
                    writer.writeCharacters(brother);
                    writer.writeEndElement();
                }

                for (String sister : person.getSisters()) {
                    writer.writeStartElement("sister");
                    writer.writeCharacters(sister);
                    writer.writeEndElement();
                }

                for (String sibling : person.getSiblings()) {
                    writer.writeStartElement("sibling");
                    writer.writeCharacters(sibling);
                    writer.writeEndElement();
                }

                writer.writeEndElement();
            }

            // Вспомогательная информация
            if (person.getChildrenNumber() != null) {
                writer.writeStartElement("children-number");
                writer.writeCharacters(String.valueOf(person.getChildrenNumber()));
                writer.writeEndElement();
            }

            if (person.getSiblingsNumber() != null) {
                writer.writeStartElement("siblings-number");
                writer.writeCharacters(String.valueOf(person.getSiblingsNumber()));
                writer.writeEndElement();
            }

            writer.writeEndElement(); // закрываем person
        }

        writer.writeEndElement(); // закрываем people
        writer.writeEndDocument();
        writer.close();
    }

    public Map<String, Person> getPersons() {
        return persons;
    }

    public List<String> getValidationIssues() {
        return validationIssues;
    }
}