package org.example.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlID;
import jakarta.xml.bind.annotation.XmlType;

import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {
        "firstName",
        "lastName",
        "gender",
        "spouse",
        "parents",
        "children",
        "siblings",
        "childrenNumber",
        "siblingsNumber"
})
public class PersonRecord {
    @XmlAttribute(name = "id", required = true)
    @XmlID
    private String id;

    @XmlElement(name = "firstname")
    private String firstName;

    @XmlElement(name = "lastname")
    private String lastName;

    @XmlElement(name = "gender")
    private String gender;

    @XmlElement(name = "spouse")
    private PersonReference spouse;

    @XmlElementWrapper(name = "parents")
    @XmlElement(name = "parent")
    private List<PersonReference> parents;

    @XmlElementWrapper(name = "children")
    @XmlElement(name = "child")
    private List<ChildReference> children;

    @XmlElement(name = "siblings")
    private Siblings siblings;

    @XmlElement(name = "children-number")
    private Integer childrenNumber;

    @XmlElement(name = "siblings-number")
    private Integer siblingsNumber;

    public PersonRecord() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public PersonReference getSpouse() {
        return spouse;
    }

    public void setSpouse(PersonReference spouse) {
        this.spouse = spouse;
    }

    public List<PersonReference> getParents() {
        return parents;
    }

    public void setParents(List<PersonReference> parents) {
        this.parents = parents;
    }

    public List<ChildReference> getChildren() {
        return children;
    }

    public void setChildren(List<ChildReference> children) {
        this.children = children;
    }

    public Siblings getSiblings() {
        return siblings;
    }

    public void setSiblings(Siblings siblings) {
        this.siblings = siblings;
    }

    public Integer getChildrenNumber() {
        return childrenNumber;
    }

    public void setChildrenNumber(Integer childrenNumber) {
        this.childrenNumber = childrenNumber;
    }

    public Integer getSiblingsNumber() {
        return siblingsNumber;
    }

    public void setSiblingsNumber(Integer siblingsNumber) {
        this.siblingsNumber = siblingsNumber;
    }
}
