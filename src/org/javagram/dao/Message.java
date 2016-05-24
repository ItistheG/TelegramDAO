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

    public Message readIt() {
        if(isRead())
            return this;
        else
            return new Message(getId(), getDate(), getText(), true,
                getSender(), getReceiver());
    }

    @Override
    public boolean equals(Object o) {

        if (this == o)
            return true;

        if (!(o instanceof Message))
            return false;

        Message message = (Message) o;

        return id == message.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
