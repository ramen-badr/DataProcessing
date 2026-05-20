package org.example.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlIDREF;

@XmlAccessorType(XmlAccessType.FIELD)
public class PersonReference {
    @XmlAttribute(name = "ref")
    @XmlIDREF
    private PersonRecord ref;

    @XmlAttribute(name = "name")
    private String name;

    public PersonReference() {
    }

    public PersonReference(PersonRecord ref, String name) {
        this.ref = ref;
        this.name = name;
    }

    public PersonRecord getRef() {
        return ref;
    }

    public void setRef(PersonRecord ref) {
        this.ref = ref;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
