package org.javagram.dao;

import org.javagram.TelegramApiBridge;
import org.javagram.response.*;
import org.javagram.response.object.*;
import org.javagram.response.object.inputs.InputUserOrPeerContact;
import org.javagram.response.object.inputs.InputUserOrPeerForeign;
import org.javagram.response.object.inputs.InputUserOrPeerSelf;
import org.javagram.response.object.updates.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

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

        bridge.processUpdates();//TODO decide

        UpdatesState updatesState = ((PrivateState) state).getUpdatesState();
        LinkedHashMap<Message, Long> newMessages = new LinkedHashMap<>();
        HashSet<Integer> readMessages = new HashSet<>();
        HashSet<Integer> deletedMessages = new HashSet<>();
        HashSet<Integer> restoredMessages = new HashSet<>();
        HashMap<Person, Date> statuses = new HashMap<>();
        HashMap<Person, Date> activities = new HashMap<>();
        LinkedHashSet<Person> updatedNames = new LinkedHashSet<>();
        LinkedHashSet<Person> updatedPhotos = new LinkedHashSet<>();

        while (true) {

            UpdatesAbsDifference difference = bridge.updatesGetDifference(updatesState);

            if (difference instanceof UpdatesDifferenceEmpty)
                return new Updates(newMessages, readMessages, deletedMessages, restoredMessages,
                        statuses, activities, updatedNames, updatedPhotos, new PrivateState(updatesState));
            if (!(difference instanceof UpdatesDifferenceOrSlice))
                throw new IllegalArgumentException();

            UpdatesDifferenceOrSlice updatesDifferenceOrSlice = (UpdatesDifferenceOrSlice) difference;

            for (MessagesMessage messagesMessage : updatesDifferenceOrSlice.getNewMessages()) {
                newMessages.put(createMessage(messagesMessage), null);
            }
            //TODO otherUpdates
            for(Update update : updatesDifferenceOrSlice.getOtherUpdates()) {
                if(update instanceof UpdateNewMessage) {
                    UpdateNewMessage updateNewMessage = (UpdateNewMessage) update;
                    Message message = createMessage(updateNewMessage.getMessage());
                    newMessages.putIfAbsent(message, null);
                } else if(update instanceof UpdateMessageID) {
                    UpdateMessageID updateMessageID = (UpdateMessageID) update;
                    Message message = createMessage(updateMessageID.getMessage());
                    if(newMessages.containsKey(message)) {
                        newMessages.put(message, updateMessageID.getRandomId());
                    }
                } else if(update instanceof UpdateReadMessage) {
                    UpdateReadMessage updateReadMessage = (UpdateReadMessage) update;
                    for(Integer id : updateReadMessage.getMessages()) {
                        readMessages.add(id);
                    }
                } else if(update instanceof UpdateDeleteMessages) {
                    UpdateDeleteMessages updateDeleteMessages = (UpdateDeleteMessages) update;
                    for(Integer id : updateDeleteMessages.getMessages()) {
                        deletedMessages.add(id);
                    }
                } else if(update instanceof UpdateRestoreMessages) {
                    UpdateRestoreMessages updateRestoreMessages = (UpdateRestoreMessages) update;
                    for(Integer id : updateRestoreMessages.getMessages()) {
                        restoredMessages.add(id);
                    }
                } else if(update instanceof UpdateUserName) {
                    UpdateUserName updateUserName = (UpdateUserName) update;
                    updatedNames.add(getPersonFor(updateUserName.getUser()));
                } else if(update instanceof UpdateUserPhoto) {
                    UpdateUserPhoto updateUserPhoto = (UpdateUserPhoto) update;
                    updatedPhotos.add(getPersonFor(updateUserPhoto.getUser()));
                } else if(update instanceof UpdateUserStatus) {
                    UpdateUserStatus updateUserStatus = (UpdateUserStatus) update;
                    statuses.put(getPersonFor(updateUserStatus.getUser()), updateUserStatus.getExpires());
                } else if(update instanceof UpdateUserTyping) {
                    UpdateUserTyping updateUserTyping = (UpdateUserTyping) update;
                    activities.put(getPersonFor(updateUserTyping.getUser()), updateUserTyping.getExpires());
                } else {

                }
            }

            updatesState = updatesDifferenceOrSlice.getState();
        }
    }


    private static int exclude(LinkedHashMap<Message, Long> messages, Set<Integer> ids) {
        int count = 0;
        for(Message message : new LinkedHashSet<>(messages.keySet())) {
            if(ids.contains(message.getId())) {
                messages.remove(message);
                count ++;
            }
        }
        return count;
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


    @Override
    protected Map<Integer, Date> getStatusesImpl(Collection<? extends Person> persons) throws IOException {
        List<InputUser> users = persons.stream().map(ApiBridgeTelegramDAO::getInputUserOrPeerFor).collect(Collectors.toList());
        List<ContactStatus> statuses = bridge.contactsGetStatuses();
        HashMap<Integer, Date> mapStatuses = new HashMap<>();
        for(ContactStatus status : statuses) {
            mapStatuses.put(status.getUserId(), status.getExpires());
        }
        Map<Integer, Date> result = new HashMap<>();
        Set<Person> absent = new HashSet<>(persons);
        for(Person person : persons){
            if(mapStatuses.containsKey(person.getId())) {
                result.put(person.getId(), mapStatuses.get(person.getId()));
                absent.remove(person);
            } else {

            }
        }
        //TODO fix the absents
        return result;
    }

    @Override
    protected BufferedImage[] getPhotosImpl(Person person, boolean small, boolean large) throws IOException {
        InputUser inputUser = getInputUserOrPeerFor(person);
        User user = bridge.usersGetUsers(new ArrayList<>(Arrays.asList(inputUser))).get(0);
        BufferedImage[] bufferedImages = new BufferedImage[2];
        if(small) {
            bufferedImages[0] = getPhoto(user.getPhoto(true));
        }
        if(large) {
            bufferedImages[1] = getPhoto(user.getPhoto(false));
        }
        return bufferedImages;
    }

    public static BufferedImage getPhoto(byte[] bytes) throws IOException {
        if(bytes == null)
            return null;

        try(InputStream stream = new ByteArrayInputStream(bytes)) {
            return ImageIO.read(stream);
        }
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

    protected static InputUser getInputUserOrPeerFor(Person person) {
        if(person instanceof Me) {
            return new InputUserOrPeerSelf();
        } else if(person instanceof Contact) {
            return new InputUserOrPeerContact(person.getId());
        } else if(person instanceof Foreign) {
            return new InputUserOrPeerForeign(person.getId(), ((Foreign) person).getAccessHash());
        } else {
            throw new IllegalArgumentException();
        }
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
