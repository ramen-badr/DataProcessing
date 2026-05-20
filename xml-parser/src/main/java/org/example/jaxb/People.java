package org.example.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "people")
@XmlAccessorType(XmlAccessType.FIELD)
public class People {
    @XmlAttribute(name = "count", required = true)
    private int count;

    @XmlElement(name = "person")
    private List<PersonRecord> persons = new ArrayList<>();

    public People() {
    }

    public People(List<PersonRecord> persons) {
        setPersons(persons);
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<PersonRecord> getPersons() {
        return persons;
    }

    public void setPersons(List<PersonRecord> persons) {
        this.persons = persons != null ? persons : new ArrayList<>();
        this.count = this.persons.size();
    }
}
