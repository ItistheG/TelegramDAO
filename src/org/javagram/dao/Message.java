package org.javagram.dao;

import org.javagram.response.object.MessagesMessage;

import java.util.Date;

/**
 * Created by HerrSergio on 07.05.2016.
 */
public class Message {

    private int id;
    private Date date;
    private String text;
    private boolean isRead;
    private Person sender;
    private Person receiver;

    Message(int id, Date date, String text, boolean isRead, Person sender, Person receiver) {
        this.id = id;
        this.date = date;
        this.text = text;
        this.isRead = isRead;
        this.sender = sender;
        this.receiver = receiver;
    }

    public int getId() {
        return id;
    }

    public Date getDate() {
        return date;
    }

    public String getText() {
        return text;
    }

    public boolean isRead() {
        return isRead;
    }

    public Person getSender() {
        return sender;
    }

    public Person getReceiver() {
        return receiver;
    }

}
