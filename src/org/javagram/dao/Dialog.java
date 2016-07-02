package org.javagram.dao;

/**
 * Created by HerrSergio on 07.05.2016.
 */
public class Dialog {

    private Person buddy;
    private int unreadCount;
    private Message lastMessage;


    Dialog(Person buddy, int unreadCount, Message lastMessage) {
        this.buddy = buddy;
        this.lastMessage = lastMessage;
        this.unreadCount = unreadCount;
    }

    public Person getBuddy() {
        return buddy;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public Message getLastMessage() {
        return lastMessage;
    }

    public Dialog update(Message lastMessage) {
        return new Dialog(buddy, unreadCount, lastMessage);
    }

    @Override
    public boolean equals(Object o) {

        if (this == o)
            return true;
        if (!(o instanceof Dialog))
            return false;

        Dialog dialog = (Dialog) o;

        return buddy.equals(dialog.buddy);

    }

    @Override
    public int hashCode() {
        return buddy.hashCode();
    }
}
