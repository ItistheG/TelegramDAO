package org.javagram.dao.proxy.changes;

import org.javagram.dao.Dialog;
import org.javagram.dao.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

/**
 * Created by HerrSergio on 20.05.2016.
 */
public class UpdateChanges {
    private PersonsChanged personsChanged;
    private DialogsChanged dialogsChanged;
    private boolean listChanged;
    private Map<Dialog, ArrayList<Message>> newMessages;

    public UpdateChanges(PersonsChanged personsChanged, DialogsChanged dialogsChanged, boolean listChanged, Map<Dialog, ArrayList<Message>> newMessages) {
        this.personsChanged = personsChanged;
        this.dialogsChanged = dialogsChanged;
        this.listChanged = listChanged;
        this.newMessages = Collections.unmodifiableMap(newMessages);
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

    public Map<Dialog, ArrayList<Message>> getNewMessages() {
        return newMessages;
    }
}
