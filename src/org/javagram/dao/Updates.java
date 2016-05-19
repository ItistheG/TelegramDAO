package org.javagram.dao;

import java.util.ArrayList;

/**
 * Created by HerrSergio on 15.05.2016.
 */
public class Updates {
    private ArrayList<Message> messages;
    private ArrayList<Message> readMessages;
    private State state;

    public Updates(ArrayList<Message> messages, ArrayList<Message> readMessages, State state) {
        this.messages = messages;
        this.readMessages = readMessages;
        this.state = state;
    }

    public ArrayList<Message> getMessages() {
        return messages;
    }

    public ArrayList<Message> getReadMessages() {
        return readMessages;
    }

    public State getState() {
        return state;
    }
}
