package org.javagram.dao.proxy.changes;

import org.javagram.dao.Dialog;
import org.javagram.dao.Message;
import org.javagram.dao.Person;

import java.util.*;

/**
 * Created by HerrSergio on 20.05.2016.
 */
public class UpdateChanges {
    private PersonsChanged personsChanged;
    private DialogsChanged dialogsChanged;
    private boolean listChanged;
    private Set<Dialog> dialogsToReset;
    private Set<Person> statusesChanged;
    private Set<Person> smallPhotosChanged;
    private Set<Person> largePhotosChanged;
    //private Map<Dialog, ArrayList<Message>> newMessages;

    public UpdateChanges(PersonsChanged personsChanged, DialogsChanged dialogsChanged, boolean listChanged,
                         Set<Dialog> dialogsToReset,
                         Set<Person> statusesChanged,
                         Set<Person> smallPhotosChanged,
                         Set<Person> largePhotosChanged
                         /*Map<Dialog, ArrayList<Message>> newMessages*/) {
        this.personsChanged = personsChanged;
        this.dialogsChanged = dialogsChanged;
        this.listChanged = listChanged;
        this.dialogsToReset = Collections.unmodifiableSet(dialogsToReset);
        this.statusesChanged = Collections.unmodifiableSet(statusesChanged);
        this.smallPhotosChanged = Collections.unmodifiableSet(smallPhotosChanged);
        this.largePhotosChanged = Collections.unmodifiableSet(largePhotosChanged);

       // this.newMessages = Collections.unmodifiableMap(newMessages);
    }

    public PersonsChanged getPersonsChanged() {
        return personsChanged;
    }

    public DialogsChanged getDialogsChanged() {
        return dialogsChanged;
    }

    public boolean getListChanged() {
        return listChanged;
    }

   /* public Map<Dialog, ArrayList<Message>> getNewMessages() {
        return newMessages;
    }*/

    public Set<Dialog> getDialogsToReset() {
        return dialogsToReset;
    }

    public Set<Person> getStatusesChanged() {
        return statusesChanged;
    }

    public Set<Person> getSmallPhotosChanged() {
        return smallPhotosChanged;
    }

    public Set<Person> getLargePhotosChanged() {
        return largePhotosChanged;
    }
}
