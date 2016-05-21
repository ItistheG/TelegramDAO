package org.javagram.dao;

import org.javagram.dao.proxy.changes.LazyObject;

import java.awt.image.BufferedImage;

/**
 * Created by HerrSergio on 15.05.2016.
 */
public class KnownPerson extends Person {

    private String phoneNumber = "";

    KnownPerson(int id, String firstName, String lastName, String phoneNumber) {
        super(id, firstName, lastName);
        setPhoneNumber(phoneNumber);
    }

    void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    @Override
    public String toString() {
        return "KnownPerson{" +
                "phoneNumber='" + phoneNumber + '\'' +
                "} " + super.toString();
    }
}
