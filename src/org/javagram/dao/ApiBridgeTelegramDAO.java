package org.javagram.dao;

import org.javagram.TelegramApiBridge;
import org.javagram.response.*;
import org.javagram.response.object.*;
import org.javagram.response.object.inputs.InputUserOrPeerContact;
import org.javagram.response.object.inputs.InputUserOrPeerForeign;
import org.javagram.response.object.inputs.InputUserOrPeerSelf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Created by HerrSergio on 06.05.2016.
 */
public class ApiBridgeTelegramDAO extends AbstractTelegramDAO {

    private TelegramApiBridge bridge;


    public ApiBridgeTelegramDAO() throws IOException {
        bridge = new TelegramApiBridge("149.154.167.50:443", 23546, "02a2c6e304647e51f0ccfef791fdfb5e");
    }

    @Override
    protected Status acceptNumberImpl() throws IOException {

        AuthCheckedPhone authCheckedPhone = bridge.authCheckPhone(getPhoneNumber());
        if(authCheckedPhone.isRegistered())
            return Status.REGISTERED;
        else if(authCheckedPhone.isInvited())
            return Status.INVITED;
        else
            return Status.NOT_REGISTERED;
    }

    @Override
    protected void sendCodeImpl() throws IOException {
        bridge.authSendCode(getPhoneNumber());
    }

    @Override
    protected Me signInImpl(String code) throws IOException {
        AuthAuthorization authAuthorization = bridge.authSignIn(code);
        return new Me((UserSelf)authAuthorization.getUser());
    }

    @Override
    public Me signUpImpl(String code, String firstName, String lastName) throws IOException {
        AuthAuthorization authAuthorization = bridge.authSignUp(code, firstName, lastName);
        return new Me((UserSelf)authAuthorization.getUser());
    }

    @Override
    protected boolean logOutImpl() {
        try {
            if(bridge.authLogOut()) {
                return true;
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    private static class PrivateState implements State {
        private UpdatesState updatesState;

        public PrivateState(UpdatesState updatesState) {
            this.updatesState = updatesState;
        }

        public UpdatesState getUpdatesState() {
            return updatesState;
        }

        @Override
        public boolean isTheSameAs(State state) {
            if(!(state instanceof PrivateState))
                throw new IllegalArgumentException();
            UpdatesState update = ((PrivateState) state).getUpdatesState();
            return updatesState.getPts() == update.getPts() && updatesState.getQts() == update.getQts() &&
                    updatesState.getSeq() == update.getSeq();
        }
    };

    @Override
    protected State getStateImpl() throws IOException {
        return new PrivateState(bridge.updatesGetState());
    }

    @Override
    protected Updates getUpdatesImpl(State state) throws IOException {
        if (!(state instanceof PrivateState))
            throw new IllegalArgumentException();
        UpdatesState updatesState = ((PrivateState) state).getUpdatesState();
        ArrayList<Message> newMessages = new ArrayList<>();
        ArrayList<Message> readMessages = new ArrayList<>();
        while (true) {

            UpdatesAbsDifference difference = bridge.updatesGetDifference(updatesState);

            if (difference instanceof UpdatesDifferenceEmpty)
                return new Updates(newMessages, readMessages, new PrivateState(updatesState));
            if (!(difference instanceof UpdatesDifferenceOrSlice))
                throw new IllegalArgumentException();

            UpdatesDifferenceOrSlice updatesDifferenceOrSlice = (UpdatesDifferenceOrSlice) difference;

            for (MessagesMessage messagesMessage : updatesDifferenceOrSlice.getNewMessages()) {
                newMessages.add(createMessage(messagesMessage));
            }
            //TODO otherUpdates
            //for(UpdatesState updatesState : updatesDifferenceOrSlice)

            updatesState = updatesDifferenceOrSlice.getState();
        }
    }

    @Override
    protected void closeImpl() {
        try {
            bridge.close();
        } catch (IOException e) {

        }
    }

    @Override
    public ArrayList<Contact> getContacts() throws IOException {
        ArrayList<UserContact> userContacts = bridge.contactsGetContacts();
        ArrayList<Contact> contacts = new ArrayList<>();
        for(UserContact userContact : userContacts)
            contacts.add(new Contact(userContact));
        return contacts;
    }

    @Override
    public ArrayList<Dialog> getDialogs() throws IOException {
        ArrayList<MessagesDialog> messagesDialogs = bridge.messagesGetDialogs();
        ArrayList<Dialog> dialogs = new ArrayList<>();
        for(MessagesDialog messageDialog : messagesDialogs) {

            User peer = messageDialog.getPeerUser();
            Person buddy = null;

            if(peer instanceof UserContact) {
                buddy = new Contact((UserContact)peer);
            } else if(peer instanceof UserForeign) {
                buddy = new Foreign((UserForeign)peer);
            } else {
                continue;
            }

            Message message = createMessage(messageDialog.getTopMessage());
            Dialog dialog = new Dialog(buddy, messageDialog.getUnreadCount(), message);
            dialogs.add(dialog);
        }

        return dialogs;
    }


    @Override
    public ArrayList<Message> getMessagesOfContact(int id, int lastMessageId, int limit) throws IOException {

        MessagesMessages messagesMessages = bridge.messagesGetHistory(new InputUserOrPeerContact(id), 0, lastMessageId, limit);
        ArrayList<Message> messages = new ArrayList<>();
        for(MessagesMessage messagesMessage : messagesMessages.getMessages()) {
            messages.add(createMessage(messagesMessage));
        }
        return messages;
    }

    @Override
    public ArrayList<Message> getMessagesOfForeign(int id, long accessHash, int lastMessageId, int limit) throws IOException {
        MessagesMessages messagesMessages = bridge.messagesGetHistory(new InputUserOrPeerForeign(id, accessHash), 0, lastMessageId, limit);
        ArrayList<Message> messages = new ArrayList<>();
        for(MessagesMessage messagesMessage : messagesMessages.getMessages()) {
            messages.add(createMessage(messagesMessage));
        }
        return messages;
    }

    @Override
    public Me getMe() throws IOException {
        return new Me((UserSelf) bridge.usersGetUsers(Arrays.asList(new InputUserOrPeerSelf())).get(0));
    }


    protected static Person getPersonFor(User user) {
        if(user instanceof UserContact)
            return new Contact((UserContact) user);
        else if(user instanceof UserSelf)
            return new Me((UserSelf) user);
        else if(user instanceof UserForeign)
            return new Foreign((UserForeign)user);
        else
            throw new IllegalArgumentException();
    }

    protected static Person getPersonById(int id, Collection<? extends User> users) {
        for(User user : users) {
            if(user.getId() == id)
                return getPersonFor(user);
        }
        return null;
    }

    protected static Message createMessage(MessagesMessage messagesMessage) {
            Person sender = getPersonFor(messagesMessage.getFrom());
            Person receiver = getPersonFor(messagesMessage.getToPeerUser());
            String text = messagesMessage.getMessage();
            if(messagesMessage.isForwarded()) {
                text = messagesMessage.getFwdFrom() + " wrote on " + messagesMessage.getFwdData() + " :\n" + text;
            }
            return new Message(messagesMessage.getId(), messagesMessage.getDate(), text,
                    !messagesMessage.isUnread(), sender, receiver);
    }
}
