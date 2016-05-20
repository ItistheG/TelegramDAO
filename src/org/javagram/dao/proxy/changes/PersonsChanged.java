package org.javagram.dao.proxy.changes;

import org.javagram.dao.Person;

import java.util.*;

/**
 * Created by HerrSergio on 20.05.2016.
 */
public class PersonsChanged {
    private List<Person> deleted;
    private List<Person> added;
    private Map<Person, Person> changed;

    public PersonsChanged(ArrayList<Person> added, LinkedHashMap<Person, Person> changed, ArrayList<Person> deleted) {
        this.deleted = Collections.unmodifiableList(deleted);
        this.added = Collections.unmodifiableList(added);
        this.changed = Collections.unmodifiableMap(changed);
    }

    public List<Person> getDeleted() {
        return deleted;
    }

    public List<Person> getAdded() {
        return added;
    }

    public Map<Person, Person> getChanged() {
        return changed;
    }
}
