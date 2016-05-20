package org.javagram.dao.proxy.changes;

import org.javagram.dao.Dialog;
import org.javagram.dao.Message;

import java.util.Collections;
import java.util.List;

/**
 * Created by HerrSergio on 20.05.2016.
 */
public class MessagesDownloaded {
    private Dialog dialog;
    private List<Message> messages;

    public MessagesDownloaded(Dialog dialog, List<Message> messages) {
        this.dialog = dialog;
        this.messages = Collections.unmodifiableList(messages);
    }

    public Dialog getDialog() {
        return dialog;
    }

    public List<Message> getMessages() {
        return messages;
    }
}
