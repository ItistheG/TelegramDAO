package org.javagram.dao;

import com.sun.org.apache.xpath.internal.operations.Bool;

import java.util.*;

/**
 * Created by HerrSergio on 15.05.2016.
 */
public class Updates {
    private LinkedHashMap<Message, Long> messages;
    private HashSet<Integer> readMessages;
    private ArrayList<Map.Entry<Integer, Boolean>> deletedAndRestoredMessages;
    private HashMap<Integer, Date> statuses;
    private HashMap<Integer, Date> activities;
    private LinkedHashSet<Integer> updatedNames = new LinkedHashSet<>();
    private LinkedHashSet<Integer> updatedPhotos = new LinkedHashSet<>();
    private boolean contactListChanged;
    private State state;

    public Updates(LinkedHashMap<Message, Long>  messages,
                   HashSet<Integer> readMessages,
                   ArrayList<Map.Entry<Integer, Boolean>> deletedAndRestoredMessages,
                   HashMap<Integer, Date> statuses,
                   HashMap<Integer, Date> activities,
                   LinkedHashSet<Integer> updatedNames,
                   LinkedHashSet<Integer> updatedPhotos,
                   boolean contactListChanged,
                   State state) {
        this.messages = messages;
        this.readMessages = readMessages;
        this.state = state;
        this.deletedAndRestoredMessages = deletedAndRestoredMessages;
        this.statuses = statuses;
        this.activities = activities;
        this.updatedNames = updatedNames;
        this.updatedPhotos = updatedPhotos;
        this.contactListChanged = contactListChanged;
    }

    public LinkedHashMap<Message, Long> getMessages() {
        return messages;
    }

    public HashSet<Integer> getReadMessages() {
        return readMessages;
    }

    public ArrayList<Map.Entry<Integer, Boolean>> getDeletedAndRestoredMessages() {
        return deletedAndRestoredMessages;
    }

    public HashMap<Integer, Date> getStatuses() {
        return statuses;
    }

    public HashMap<Integer, Date> getActivities() {
        return activities;
    }

    public LinkedHashSet<Integer> getUpdatedNames() {
        return updatedNames;
    }

    public LinkedHashSet<Integer> getUpdatedPhotos() {
        return updatedPhotos;
    }

    public boolean isContactListChanged() {
        return contactListChanged;
    }

    public State getState() {
        return state;
    }
}
