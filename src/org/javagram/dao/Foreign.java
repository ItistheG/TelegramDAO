package org.javagram.dao;

import org.javagram.response.object.UserForeign;
import org.javagram.response.object.UserSelf;

/**
 * Created by HerrSergio on 15.05.2016.
 */
public class Foreign extends Person {
    private long accessHash;

    public Foreign(String lastName, String firstName, int id, long accessHash) {
        super(id, firstName, lastName);
        setAccessHash(accessHash);
    }

    Foreign(UserForeign userForeign) {
        this(userForeign.getLastName(), userForeign.getFirstName(), userForeign.getId(), userForeign.getAccessHash());
    }

    public long getAccessHash() {
        return accessHash;
    }

    void setAccessHash(long accessHash) {
        this.accessHash = accessHash;
    }

    @Override
    public String toString() {
        return "Foreign{" +
                "accessHash=" + accessHash +
                "} " + super.toString();
    }
}
