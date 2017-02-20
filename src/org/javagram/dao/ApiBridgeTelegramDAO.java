package org.javagram.dao;

import org.javagram.TelegramApiBridge;
import org.javagram.response.*;
import org.javagram.response.object.*;
import org.javagram.response.object.inputs.*;
import org.javagram.response.object.updates.*;
import org.telegram.api.engine.RpcException;

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


    public ApiBridgeTelegramDAO(String server, int appId, String appHash) throws IOException {
        bridge = new TelegramApiBridge(server, appId, appHash);
    }

    @Override
    protected Status acceptNumberImpl() throws IOException, ApiException {
    try {
        AuthCheckedPhone authCheckedPhone = bridge.authCheckPhone(getPhoneNumber());
        if (authCheckedPhone.isRegistered())
            return Status.REGISTERED;
        else if (authCheckedPhone.isInvited())
            return Status.INVITED;
        else
            return Status.NOT_REGISTERED;
    } catch (RpcException e) {
        throw getApiException(e);
    }
    }

    @Override
    protected void sendCodeImpl() throws IOException, ApiException {
        try {
            bridge.authSendCode(getPhoneNumber());
        } catch (RpcException e) {
            throw getApiException(e);
        }
    }

    @Override
    protected Me signInImpl(String code) throws IOException, ApiException {
        try {
            AuthAuthorization authAuthorization = bridge.authSignIn(code);
            return new Me((UserSelf) authAuthorization.getUser());
        } catch (RpcException e) {
            throw getApiException(e);
        }
    }

    @Override
    public Me signUpImpl(String code, String firstName, String lastName) throws IOException, ApiException {
        try {
            AuthAuthorization authAuthorization = bridge.authSignUp(code, firstName, lastName);
            return new Me((UserSelf) authAuthorization.getUser());
        } catch (RpcException e) {
            throw getApiException(e);
        }
    }

    @Override
    protected void logOutImpl() throws ApiException, IOException {
        try {
            if(!bridge.authLogOut()) {
                throw new ApiException();
            }
        } catch (RpcException e) {
            throw getApiException(e);
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
    protected State getStateImpl() throws IOException, ApiException {
        try {
            return new PrivateState(bridge.updatesGetState());
        } catch (RpcException e) {
            throw getApiException(e);
        }
    }

    @Override
    protected Updates getUpdatesImpl(State state) throws IOException, ApiException {
        try {
            if (!(state instanceof PrivateState))
                throw new IllegalArgumentException();

            UpdatesState updatesState = ((PrivateState) state).getUpdatesState();
            LinkedHashMap<Message, Long> newMessages = new LinkedHashMap<>();
            HashSet<Integer> readMessages = new HashSet<>();
            ArrayList<Map.Entry<Integer, Boolean>> deletedAndRestoredMessages = new ArrayList<>();
            HashMap<Integer, Date> statuses = new HashMap<>();
            HashMap<Integer, Date> activities = new HashMap<>();
            LinkedHashSet<Integer> updatedNames = new LinkedHashSet<>();
            LinkedHashSet<Integer> updatedPhotos = new LinkedHashSet<>();
            boolean contactListChanged = false;

            while (true) {

                UpdatesAbsDifference difference = bridge.updatesGetDifference(updatesState);

                if (difference instanceof UpdatesDifferenceEmpty)
                    return new Updates(newMessages, readMessages, deletedAndRestoredMessages, statuses, activities,
                            updatedNames, updatedPhotos, contactListChanged, new PrivateState(updatesState));
                if (!(difference instanceof UpdatesDifferenceOrSlice))
                    throw new IllegalArgumentException();

                UpdatesDifferenceOrSlice updatesDifferenceOrSlice = (UpdatesDifferenceOrSlice) difference;

                for (MessagesMessage messagesMessage : updatesDifferenceOrSlice.getNewMessages()) {
                    newMessages.put(createMessage(messagesMessage), null);
                }
                //TODO otherUpdates
                for (Update update : updatesDifferenceOrSlice.getOtherUpdates()) {
                    if (update instanceof UpdateNewMessage) {
                        UpdateNewMessage updateNewMessage = (UpdateNewMessage) update;
                        Message message = createMessage((MessagesMessage) updateNewMessage.getMessage());
                        newMessages.putIfAbsent(message, null);
                    } else if (update instanceof UpdateMessageID) {
                        UpdateMessageID updateMessageID = (UpdateMessageID) update;
                        Message message = createMessage(((UpdateMessageIDExt) updateMessageID).getMessage());
                        if (newMessages.containsKey(message)) {
                            newMessages.put(message, updateMessageID.getRandomId());
                        }
                    } else
                        contactListChanged = nonMessageChanges(readMessages, deletedAndRestoredMessages, statuses,
                                activities, updatedNames, updatedPhotos, contactListChanged, update);
                }

                updatesState = updatesDifferenceOrSlice.getState();
            }
        } catch (RpcException e) {
            throw getApiException(e);
        }
    }

    @Override
    protected Updates getAsyncUpdatesImpl(State state, Collection<?extends Person> persons, Me me) throws IOException, ApiException {

            if (!(state instanceof PrivateState))
                throw new IllegalArgumentException();

            UpdatesState updatesState = ((PrivateState) state).getUpdatesState();

            UpdatesAsyncDifference asyncDifference = bridge.processAsyncUpdates(updatesState, null, me.getId());

            LinkedHashMap<Message, Long> newMessages = new LinkedHashMap<>();
            HashSet<Integer> readMessages = new HashSet<>();
            ArrayList<Map.Entry<Integer, Boolean>> deletedAndRestoredMessages = new ArrayList<>();
            HashMap<Integer, Date> statuses = new HashMap<>();
            HashMap<Integer, Date> activities = new HashMap<>();
            LinkedHashSet<Integer> updatedNames = new LinkedHashSet<>();
            LinkedHashSet<Integer> updatedPhotos = new LinkedHashSet<>();
            boolean contactListChanged = false;

            for (org.javagram.response.object.Message message : asyncDifference.getNewMessages()) {
                newMessages.put(createMessage(message, persons, me), null);
            }
            //TODO otherUpdates
            for (Update update : asyncDifference.getOtherUpdates()) {
                if (update instanceof UpdateNewMessage) {
                    UpdateNewMessage updateNewMessage = (UpdateNewMessage) update;
                    Message message = createMessage(updateNewMessage.getMessage(), persons, me);
                    newMessages.putIfAbsent(message, null);
                } else if (update instanceof UpdateMessageID) {
                    UpdateMessageID updateMessageID = (UpdateMessageID) update;
                    //FIXME
                    for (Map.Entry<Message, Long> m : newMessages.entrySet()) {
                        if (m.getKey().getId() == updateMessageID.getMessageId()) {
                            m.setValue(updateMessageID.getRandomId());
                            break;
                        }
                    }
                } else
                    contactListChanged = nonMessageChanges(readMessages, deletedAndRestoredMessages, statuses,
                            activities, updatedNames, updatedPhotos, contactListChanged, update);
            }

            updatesState = asyncDifference.getState();
            PrivateState privateState = updatesState == null ? null : new PrivateState(updatesState);

            return new Updates(newMessages, readMessages, deletedAndRestoredMessages, statuses, activities,
                    updatedNames, updatedPhotos, contactListChanged, privateState);

    }

    private boolean nonMessageChanges(HashSet<Integer> readMessages, ArrayList<Map.Entry<Integer, Boolean>> deletedAndRestoredMessages, HashMap<Integer, Date> statuses, HashMap<Integer, Date> activities, LinkedHashSet<Integer> updatedNames, LinkedHashSet<Integer> updatedPhotos, boolean contactListChanged, Update update) throws ApiException {

         if (update instanceof UpdateReadMessage) {
             UpdateReadMessage updateReadMessage = (UpdateReadMessage) update;
             for (Integer id : updateReadMessage.getMessages()) {
                 readMessages.add(id);
             }
         } else if (update instanceof UpdateDeleteMessages) {
             UpdateDeleteMessages updateDeleteMessages = (UpdateDeleteMessages) update;
             for (Integer id : updateDeleteMessages.getMessages()) {
                 deletedAndRestoredMessages.add(new Entry(id, true));
             }
         } else if (update instanceof UpdateRestoreMessages) {
             UpdateRestoreMessages updateRestoreMessages = (UpdateRestoreMessages) update;
             for (Integer id : updateRestoreMessages.getMessages()) {
                 deletedAndRestoredMessages.add(new Entry(id, false));
             }
         } else if (update instanceof UpdateUserName) {
             UpdateUserName updateUserName = (UpdateUserName) update;
             updatedNames.add(updateUserName.getUser());
         } else if (update instanceof UpdateUserPhoto) {
             UpdateUserPhoto updateUserPhoto = (UpdateUserPhoto) update;
             updatedPhotos.add(updateUserPhoto.getUser());
         } else if (update instanceof UpdateUserStatus) {
             UpdateUserStatus updateUserStatus = (UpdateUserStatus) update;
             statuses.put(updateUserStatus.getUser(), updateUserStatus.getExpires());
         } else if (update instanceof UpdateUserTyping) {
             UpdateUserTyping updateUserTyping = (UpdateUserTyping) update;
             activities.put(updateUserTyping.getUser(), updateUserTyping.getExpires());
         } else {
             contactListChanged = true;
         }
         return contactListChanged;

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
    protected void closeImpl() throws IOException, ApiException {
        try {
            bridge.close();
        } catch (RpcException e) {
            throw getApiException(e);
        }
    }

    @Override
    public ArrayList<Contact> getContacts() throws IOException, ApiException {
        try {
            ArrayList<UserContact> userContacts = bridge.contactsGetContacts();
            ArrayList<Contact> contacts = new ArrayList<>();
            for (UserContact userContact : userContacts)
                contacts.add(new Contact(userContact));
            return contacts;
        } catch (RpcException e) {
            throw getApiException(e);
        }
    }

    @Override
    public ArrayList<Dialog> getDialogs() throws IOException, ApiException {
        try {
            ArrayList<MessagesDialog> messagesDialogs = bridge.messagesGetDialogs();
            ArrayList<Dialog> dialogs = new ArrayList<>();
            for (MessagesDialog messageDialog : messagesDialogs) {

                User peer = messageDialog.getPeerUser();
                Person buddy = null;

                if (peer instanceof UserContact) {
                    buddy = new Contact((UserContact) peer);
                } else if (peer instanceof UserForeign) {
                    buddy = new Foreign((UserForeign) peer);
                } else {
                    continue;
                }

                Message message = createMessage(messageDialog.getTopMessage());
                Dialog dialog = new Dialog(buddy, messageDialog.getUnreadCount(), message);
                dialogs.add(dialog);
            }

            return dialogs;
        } catch (RpcException e) {
            throw getApiException(e);
        }
    }


    @Override
    public ArrayList<Message> getMessagesOfContact(int id, int lastMessageId, int limit) throws IOException, ApiException {
try {
    MessagesMessages messagesMessages = bridge.messagesGetHistory(new InputUserOrPeerContact(id), 0, lastMessageId, limit);
    ArrayList<Message> messages = new ArrayList<>();
    for (MessagesMessage messagesMessage : messagesMessages.getMessages()) {
        messages.add(createMessage(messagesMessage));
    }
    return messages;
} catch (RpcException e) {
    throw getApiException(e);
}
    }

    @Override
    public ArrayList<Message> getMessagesOfForeign(int id, long accessHash, int lastMessageId, int limit) throws IOException, ApiException {
       try {
           MessagesMessages messagesMessages = bridge.messagesGetHistory(new InputUserOrPeerForeign(id, accessHash), 0, lastMessageId, limit);
           ArrayList<Message> messages = new ArrayList<>();
           for (MessagesMessage messagesMessage : messagesMessages.getMessages()) {
               messages.add(createMessage(messagesMessage));
           }
           return messages;
       } catch (RpcException e) {
           throw getApiException(e);
       }
    }

    @Override
    public Me getMe() throws IOException, ApiException {
        try {
            return new Me((UserSelf) bridge.usersGetUsers(Arrays.asList(new InputUserOrPeerSelf())).get(0));
        } catch (RpcException e) {
            throw getApiException(e);
        }
    }


    @Override
    protected Map<Integer, Date> getStatusesImpl(Collection<? extends Person> persons) throws IOException, ApiException {
        try {
            List<InputUser> users = persons.stream().map(ApiBridgeTelegramDAO::getInputUserOrPeerFor).collect(Collectors.toList());
            List<ContactStatus> statuses = bridge.contactsGetStatuses();
            HashMap<Integer, Date> mapStatuses = new HashMap<>();
            for (ContactStatus status : statuses) {
                mapStatuses.put(status.getUserId(), status.getExpires());
            }
            Map<Integer, Date> result = new HashMap<>();
            Set<Person> absent = new HashSet<>(persons);
            for (Person person : persons) {
                if (mapStatuses.containsKey(person.getId())) {
                    result.put(person.getId(), mapStatuses.get(person.getId()));
                    absent.remove(person);
                } else {

                }
            }
            //TODO fix the absents
            return result;
        } catch (RpcException e) {
            throw getApiException(e);
        }
    }

    @Override
    protected BufferedImage[] getPhotosImpl(Person person, boolean small, boolean large) throws IOException, ApiException {
        try {
            InputUser inputUser = getInputUserOrPeerFor(person);
            User user = bridge.usersGetUsers(new ArrayList<>(Arrays.asList(inputUser))).get(0);
            BufferedImage[] bufferedImages = new BufferedImage[2];
            if (small) {
                bufferedImages[0] = user.getPhoto(bridge, true);
            }
            if (large) {
                bufferedImages[1] = user.getPhoto(bridge, false);
            }
            return bufferedImages;
        } catch (RpcException e) {
            throw getApiException(e);
        }
    }

    public static BufferedImage getPhoto(byte[] bytes) throws IOException {
        if(bytes == null)
            return null;

        try(InputStream stream = new ByteArrayInputStream(bytes)) {
            return ImageIO.read(stream);
        }
    }

    public void sendMessage(Person person, String text, long randomId) throws IOException, ApiException {
        try {
            bridge.messagesSendMessage(getInputUserOrPeerFor(person), text, randomId);
        } catch (RpcException e) {
            throw getApiException(e);
        }
    }

    public void readMessages(Message lastMessage) throws IOException, ApiException {
        try {
            bridge.messagesReadHistory(getInputUserOrPeerFor(lastMessage.getBuddy()), lastMessage.getId());
        } catch (RpcException e) {
            throw getApiException(e);
        }
    }

    public void receivedMessages(Message lastMessage) throws IOException, ApiException {
        try {
            bridge.messagesReceivedMessages(lastMessage.getId());
        } catch (RpcException e) {
            throw getApiException(e);
        }
    }

    public void importContact(String phone, String firstName, String lastName) throws ApiException, IOException {
        try {
            if(bridge.contactsImportContact(new InputContact(0, phone, firstName, lastName)) == null)
                throw new ApiException();
        } catch (RpcException e) {
            throw getApiException(e);
        }
    }

    public void deleteContact(int contactId) throws IOException, ApiException {
        try {
            if(!bridge.contactsDeleteContact(contactId)) {
                throw new ApiException();
            }
        } catch (RpcException e) {
            throw getApiException(e);
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

    protected static InputUserOrPeer getInputUserOrPeerFor(Person person) {
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

    protected static Message createMessage(org.javagram.response.object.Message message, Map<Integer, User> users) {
        Person sender = getPersonFor(users.get(message.getFromId()));
        Person receiver = getPersonFor(users.get(message.getToPeerUserId()));
        String text = message.getMessage();
        if(message.isForwarded()) {
            text = users.get(message.getFwdFromId()) + " wrote on " + message.getFwdData() + " :\n" + text;
        }
        return new Message(message.getId(), message.getDate(), text,
                !message.isUnread(), sender, receiver);
    }

    protected static Message createMessage(org.javagram.response.object.Message message, Collection<? extends Person> persons, Me me) {
        Person sender = getPersonBy(me, persons, message.getFromId());
        Person receiver = getPersonBy(me, persons, message.getToPeerUserId());
        String text = message.getMessage();
        if(message.isForwarded()) {
            Person fwd = getPersonBy(me, persons, message.getFwdFromId());
            String name = "Unknown";
            if(fwd != null)
                name = fwd.getFirstName() + " " + fwd.getLastName();
            text = name + " wrote on " + message.getFwdData() + " :\n" + text;
        }
        return new Message(message.getId(), message.getDate(), text,
                !message.isUnread(), sender, receiver);
    }

    protected static Person getPersonBy(Me me, Collection<? extends Person> users, int id) {
        if(me.getId() == id)
            return me;
        for(Person user : users) {
            if(user.getId() == id)
                return user;
        }
        return null;
    }

    public static class Entry implements Map.Entry<Integer, Boolean> {

        private int messageId;
        private boolean isDeleted;

        public Entry(int messageId, boolean isDeleted) {
            this.messageId = messageId;
            this.isDeleted = isDeleted;
        }

        @Override
        public Integer getKey() {
            return messageId;
        }

        @Override
        public Boolean getValue() {
            return isDeleted;
        }

        @Override
        public Boolean setValue(Boolean aBoolean) {
            return false;
        }
    }

    public static ApiException getApiException(RpcException e) {
        return new ApiException(e.getErrorCode(), e.getErrorTag());
    }

}
