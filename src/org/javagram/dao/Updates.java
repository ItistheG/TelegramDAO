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
    private HashMap<Person, Date> statuses;
    private HashMap<Person, Date> activities;
    private LinkedHashSet<Person> updatedNames = new LinkedHashSet<>();
    private LinkedHashSet<Person> updatedPhotos = new LinkedHashSet<>();
    private State state;

    public Updates(LinkedHashMap<Message, Long>  messages,
                   HashSet<Integer> readMessages,
                   ArrayList<Map.Entry<Integer, Boolean>> deletedAndRestoredMessages,
                   HashMap<Person, Date> statuses,
                   HashMap<Person, Date> activities,
                   LinkedHashSet<Person> updatedNames,
                   LinkedHashSet<Person> updatedPhotos,
                   State state) {
        this.messages = messages;
        this.readMessages = readMessages;
        this.state = state;
        this.deletedAndRestoredMessages = deletedAndRestoredMessages;
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

    public ArrayList<Map.Entry<Integer, Boolean>> getDeletedAndRestoredMessages() {
        return deletedAndRestoredMessages;
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
