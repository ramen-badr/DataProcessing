package org.example;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import org.example.jaxb.*;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

public class XMLParser {
    private final Map<String, Person> persons = new HashMap<>();
    private final List<String> validationIssues = new ArrayList<>();
    private static final Pattern XML_ID_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_.-]*");

    public void parse(String filePath) throws Exception {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLStreamReader reader = factory.createXMLStreamReader(new FileInputStream(filePath));

        Person currentPerson = null;
        StringBuilder textContent = new StringBuilder();

        while (reader.hasNext()) {
            int event = reader.next();
            String elementName;

            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    elementName = reader.getLocalName();

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

            case "children-number":
                String childrenNumberValue = reader.getAttributeValue(null, "value");
                if (childrenNumberValue != null) {
                    try {
                        person.setChildrenNumber(Integer.parseInt(childrenNumberValue.trim()));
                    } catch (NumberFormatException e) {
                        // Игнорируем некорректные числа
                    }
                }
                break;

            case "siblings-number":
                String siblingsNumberValue = reader.getAttributeValue(null, "value");
                if (siblingsNumberValue != null) {
                    try {
                        person.setSiblingsNumber(Integer.parseInt(siblingsNumberValue.trim()));
                    } catch (NumberFormatException e) {
                        // Игнорируем некорректные числа
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
        People people = buildPeopleModel();
        JAXBContext context = JAXBContext.newInstance(People.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setSchema(loadSchema());
        marshaller.marshal(people, new File(outputPath));
    }

    public Map<String, Person> getPersons() {
        return persons;
    }

    public List<String> getValidationIssues() {
        return validationIssues;
    }

    private Schema loadSchema() throws Exception {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        URL schemaUrl = XMLParser.class.getClassLoader().getResource("people.xsd");
        if (schemaUrl == null) {
            throw new FileNotFoundException("people.xsd not found in resources");
        }
        return schemaFactory.newSchema(schemaUrl);
    }

    private People buildPeopleModel() {
        Map<Person, PersonRecord> recordByPerson = new HashMap<>();
        Map<String, PersonRecord> recordById = new HashMap<>();
        Map<String, PersonRecord> recordByName = new HashMap<>();
        Set<String> usedIds = new HashSet<>();
        List<PersonRecord> records = new ArrayList<>();
        int index = 1;

        for (Person person : persons.values()) {
            String rawId = trimToNull(person.getId());
            String xmlId = normalizeXmlId(rawId, index++, usedIds);
            PersonRecord record = new PersonRecord();
            record.setId(xmlId);
            record.setFirstName(trimToNull(person.getFirstName()));
            record.setLastName(trimToNull(person.getLastName()));
            record.setGender(trimToNull(person.getGender()));
            record.setChildrenNumber(person.getChildrenNumber());
            record.setSiblingsNumber(person.getSiblingsNumber());

            records.add(record);
            recordByPerson.put(person, record);
            if (rawId != null) {
                recordById.put(rawId, record);
            }

            String fullName = buildFullName(person);
            if (!fullName.isEmpty()) {
                recordByName.put(fullName, record);
            }
        }

        for (Person person : persons.values()) {
            PersonRecord record = recordByPerson.get(person);
            record.setSpouse(buildReference(person.getSpouse(), recordById, recordByName));

            List<PersonReference> parents = buildReferences(person.getParents(), recordById, recordByName);
            if (!parents.isEmpty()) {
                record.setParents(parents);
            }

            List<ChildReference> children = buildChildReferences(person.getChildren(), recordById, recordByName);
            if (!children.isEmpty()) {
                record.setChildren(children);
            }

            Siblings siblings = buildSiblings(person, recordById, recordByName);
            if (siblings != null) {
                record.setSiblings(siblings);
            }
        }

        People people = new People();
        people.setPersons(records);
        return people;
    }

    private String buildFullName(Person person) {
        String firstName = trimToNull(person.getFirstName());
        String lastName = trimToNull(person.getLastName());
        if (firstName == null && lastName == null) {
            return "";
        }
        if (firstName == null) {
            return lastName;
        }
        if (lastName == null) {
            return firstName;
        }
        return firstName + " " + lastName;
    }

    private PersonReference buildReference(String value, Map<String, PersonRecord> recordById,
                                           Map<String, PersonRecord> recordByName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        PersonRecord ref = recordById.get(trimmed);
        if (ref == null) {
            ref = recordByName.get(trimmed);
        }
        PersonReference reference = new PersonReference();
        if (ref != null) {
            reference.setRef(ref);
        } else {
            reference.setName(trimmed);
        }
        return reference;
    }

    private List<PersonReference> buildReferences(List<String> values, Map<String, PersonRecord> recordById,
                                                  Map<String, PersonRecord> recordByName) {
        List<PersonReference> references = new ArrayList<>();
        for (String value : values) {
            PersonReference reference = buildReference(value, recordById, recordByName);
            if (reference != null) {
                references.add(reference);
            }
        }
        return references;
    }

    private List<ChildReference> buildChildReferences(List<Child> children, Map<String, PersonRecord> recordById,
                                                      Map<String, PersonRecord> recordByName) {
        List<ChildReference> references = new ArrayList<>();
        for (Child child : children) {
            ChildReference reference = new ChildReference();
            reference.setType(trimToNull(child.getType()) == null ? "child" : trimToNull(child.getType()));

            String childId = trimToNull(child.getId());
            String childName = trimToNull(child.getName());
            PersonRecord ref = null;
            if (childId != null) {
                ref = recordById.get(childId);
            }
            if (ref == null && childName != null) {
                ref = recordByName.get(childName);
            }
            if (ref != null) {
                reference.setRef(ref);
            } else if (childName != null) {
                reference.setName(childName);
            } else if (childId != null) {
                reference.setName(childId);
            }
            references.add(reference);
        }
        return references;
    }

    private Siblings buildSiblings(Person person, Map<String, PersonRecord> recordById,
                                   Map<String, PersonRecord> recordByName) {
        List<PersonReference> brothers = buildReferences(person.getBrothers(), recordById, recordByName);
        List<PersonReference> sisters = buildReferences(person.getSisters(), recordById, recordByName);
        List<PersonReference> siblings = buildReferences(person.getSiblings(), recordById, recordByName);
        if (brothers.isEmpty() && sisters.isEmpty() && siblings.isEmpty()) {
            return null;
        }
        Siblings siblingsGroup = new Siblings();
        siblingsGroup.setBrothers(brothers);
        siblingsGroup.setSisters(sisters);
        siblingsGroup.setSiblings(siblings);
        return siblingsGroup;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeXmlId(String rawId, int index, Set<String> usedIds) {
        String candidate = rawId != null ? rawId.trim() : "";
        if (candidate.isEmpty() || !XML_ID_PATTERN.matcher(candidate).matches()) {
            candidate = "P" + index;
        }
        while (usedIds.contains(candidate)) {
            candidate = candidate + "_" + index;
        }
        usedIds.add(candidate);
        return candidate;
    }
}
