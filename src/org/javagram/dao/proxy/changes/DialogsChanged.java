package org.javagram.dao.proxy.changes;

import org.javagram.dao.Dialog;
import org.javagram.dao.Person;

import java.util.*;

/**
 * Created by HerrSergio on 20.05.2016.
 */
public class DialogsChanged {
    private List<Dialog> added;
    private Map<Dialog, Dialog> changed;
    private List<Dialog> deleted;
    //private boolean listChanged;

    public DialogsChanged(ArrayList<Dialog> added, LinkedHashMap<Dialog, Dialog> changed, ArrayList<Dialog> deleted/*, boolean listChanged*/) {
        this.added = Collections.unmodifiableList(added);
        this.changed = Collections.unmodifiableMap(changed);
        this.deleted = Collections.unmodifiableList(deleted);
        //this.listChanged = listChanged;
    }

    public List<Dialog> getAdded() {
        return Collections.unmodifiableList(added);
    }

    public Map<Dialog, Dialog> getChanged() {
        return Collections.unmodifiableMap(changed);
    }

    public List<Dialog> getDeleted() {
        return Collections.unmodifiableList(deleted);
    }

 /*   public boolean isListChanged() {
        return listChanged;
    }*/
}
