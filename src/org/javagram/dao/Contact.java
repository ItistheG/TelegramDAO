package org.javagram.dao;

import org.javagram.response.object.UserContact;

/**
 * Created by HerrSergio on 06.05.2016.
 */
public class Contact extends KnownPerson {

    public Contact(String lastName, String firstName, String phoneNumber, int id) {
        super(id, firstName, lastName, phoneNumber);
    }

    Contact(UserContact userContact) {
        this(userContact.getLastName(), userContact.getFirstName(), userContact.getPhone(), userContact.getId());
    }

    @Override
    public String toString() {
        return "Contact{} " + super.toString();
    }
}
