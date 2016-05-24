package org.javagram.dao;

import java.util.*;

/**
 * Created by HerrSergio on 15.05.2016.
 */
public class Updates {
    private LinkedHashMap<Message, Long> messages;
    private HashSet<Integer> readMessages;
    private HashSet<Integer> deletedMessages;
    private HashSet<Integer> restoredMessages;
    private HashMap<Person, Date> statuses;
    private HashMap<Person, Date> activities;
    private LinkedHashSet<Person> updatedNames = new LinkedHashSet<>();
    private LinkedHashSet<Person> updatedPhotos = new LinkedHashSet<>();
    private State state;

    public Updates(LinkedHashMap<Message, Long>  messages,
                   HashSet<Integer> readMessages,
                   HashSet<Integer> deletedMessages,
                   HashSet<Integer> restoredMessages,
                   HashMap<Person, Date> statuses,
                   HashMap<Person, Date> activities,
                   LinkedHashSet<Person> updatedNames,
                   LinkedHashSet<Person> updatedPhotos,
                   State state) {
        this.messages = messages;
        this.readMessages = readMessages;
        this.state = state;
        this.deletedMessages = deletedMessages;
        this.restoredMessages = restoredMessages;
        this.statuses = statuses;
        this.activities = activities;
        this.updatedNames = updatedNames;
        this.updatedPhotos = updatedPhotos;
    }

    public LinkedHashMap<Message, Long> getMessages() {
        return messages;
    }

    public HashSet<Integer> getReadMessages() {
        return readMessages;
    }

    public HashSet<Integer> getDeletedMessages() {
        return deletedMessages;
    }

    public HashSet<Integer> getRestoredMessages() {
        return restoredMessages;
    }

    public HashMap<Person, Date> getStatuses() {
        return statuses;
    }

    public HashMap<Person, Date> getActivities() {
        return activities;
    }

    public LinkedHashSet<Person> getUpdatedNames() {
        return updatedNames;
    }

    public LinkedHashSet<Person> getUpdatedPhotos() {
        return updatedPhotos;
    }

    public State getState() {
        return state;
    }
}
