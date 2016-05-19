package org.javagram.dao;

import org.javagram.response.object.UserSelf;

/**
 * Created by HerrSergio on 06.05.2016.
 */
public class Me extends KnownPerson {

    public Me(String lastName, String firstName, String phoneNumber, int id) {
        setId(id);
        setFirstName(firstName);
        setLastName(lastName);
        setPhoneNumber(phoneNumber);
    }

    Me(UserSelf userSelf) {
        this(userSelf.getLastName(), userSelf.getFirstName(), userSelf.getPhone(), userSelf.getId());
    }

    @Override
    public String toString() {
        return "Me{} " + super.toString();
    }
}
