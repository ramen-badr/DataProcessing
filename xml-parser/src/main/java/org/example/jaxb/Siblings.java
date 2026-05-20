package org.example.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class Siblings {
    @XmlElement(name = "brother")
    private List<PersonReference> brothers = new ArrayList<>();

    @XmlElement(name = "sister")
    private List<PersonReference> sisters = new ArrayList<>();

    @XmlElement(name = "sibling")
    private List<PersonReference> siblings = new ArrayList<>();

    public Siblings() {
    }

    public List<PersonReference> getBrothers() {
        return brothers;
    }

    public void setBrothers(List<PersonReference> brothers) {
        this.brothers = brothers;
    }

    public List<PersonReference> getSisters() {
        return sisters;
    }

    public void setSisters(List<PersonReference> sisters) {
        this.sisters = sisters;
    }

    public List<PersonReference> getSiblings() {
        return siblings;
    }

    public void setSiblings(List<PersonReference> siblings) {
        this.siblings = siblings;
    }
}
