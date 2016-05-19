package org.javagram.dao;

/**
 * Created by HerrSergio on 15.05.2016.
 */
public class KnownPerson extends Person {

    private String phoneNumber = "";

    KnownPerson() {

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
