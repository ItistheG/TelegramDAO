package org.javagram.dao;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by HerrSergio on 06.05.2016.
 */
public class DebugTelegramDAO extends AbstractTelegramDAO {

    private Me me;

    private static final String REGISTERED_DIGIT = "1", NOT_REGISTERED_DIGIT = "0",
        INVITED_DIGIT = "2", INVALID_NUMBER = "3", IO_EXCEPTION_DIGIT = "4",
            REGISTERED_EXPIRED_DIGIT = "6", NOT_REGISTERED_EXPIRED_DIGIT = "5",
            INVITED_EXPIRED_DIGIT = "7",
            NOT_REGISTERED_EXPIRED2_DIGIT = "8",   INVITED_EXPIRED2_DIGIT = "9";

    ;
    private static final int MIGRATION = 4;

    @Override
    protected Status acceptNumberImpl() throws IOException, ApiException {
        if(getPhoneNumber().isEmpty() || getPhoneNumber().endsWith(INVALID_NUMBER))
            throw new ApiException(ApiException.BAD_REQUEST, ApiException.PHONE_NUMBER_INVALID);
        else if(getPhoneNumber().endsWith(NOT_REGISTERED_DIGIT)
                || getPhoneNumber().endsWith(NOT_REGISTERED_EXPIRED_DIGIT)
                || getPhoneNumber().endsWith(NOT_REGISTERED_EXPIRED2_DIGIT))
            return Status.NOT_REGISTERED;
        else if(getPhoneNumber().endsWith(INVITED_DIGIT)
                || getPhoneNumber().endsWith(INVITED_EXPIRED_DIGIT)
                || getPhoneNumber().endsWith(INVITED_EXPIRED2_DIGIT))
            return Status.INVITED;
        else if(getPhoneNumber().endsWith(REGISTERED_DIGIT)
                || getPhoneNumber().endsWith(REGISTERED_EXPIRED_DIGIT))
            return Status.REGISTERED;
        else if(getPhoneNumber().endsWith(IO_EXCEPTION_DIGIT))
            throw new IOException();
        else
            throw new ApiException();
    }

    @Override
    protected void sendCodeImpl() throws IOException, ApiException {
        System.out.println("Yer kode iz " + correctCode);
    }

    @Override
    public Me getMe() throws IOException, ApiException {
        check(isLoggedIn());
        return me;
    }

    @Override
    public ArrayList<Contact> getContacts() throws IOException, ApiException {
        Stream<Person> persons = getData().keySet().stream().filter(p -> p instanceof Contact);
        Stream<Contact> contacts = persons.map(p -> (Contact)p);
        return new ArrayList<>(contacts.collect(Collectors.toList()));
    }

       @Override
    public ArrayList<Dialog> getDialogs() throws IOException, ApiException {
        ArrayList<Dialog> dialogs = new ArrayList<>();
        for(Map.Entry<Person, Message[]> entry : getData().entrySet()) {
            if(entry.getValue().length > 0) {
                dialogs.add(new Dialog(entry.getKey(), unread(entry.getValue()), entry.getValue()[0]));
            }
        }
        return dialogs;
    }

    @Override
    public ArrayList<Message> getMessagesOfContact(int id, int lastMessageId, int limit) throws IOException, ApiException {
        if(limit < 1)
            limit = 100;
        ArrayList<Message> messages = new ArrayList<>();
        for(Person contact : getData().keySet()) {
            if(!(contact instanceof Contact))
                continue;
            if(contact.getId() == id) {
                for(Message message : getData().get(contact)) {
                    if(lastMessageId > 0 && lastMessageId <= message.getId()) {
                        continue;
                    }

                    messages.add(message);
                    if(--limit == 0)
                        break;
                }
            }
        }
        return messages;
    }

    @Override
    public ArrayList<Message> getMessagesOfForeign(int id, long accessHash, int lastMessageId, int limit) throws IOException, ApiException {
        return new ArrayList<>();
    }

    private static final String correctCode = "00000";

    @Override
    protected Me signInImpl(String code) throws IOException, ApiException {
        if(correctCode.equals(code)) {
            if(getPhoneNumber().endsWith(NOT_REGISTERED_EXPIRED_DIGIT) || getPhoneNumber().endsWith(INVITED_EXPIRED_DIGIT)
                || getPhoneNumber().endsWith(REGISTERED_EXPIRED_DIGIT))
                throw new ApiException(ApiException.BAD_REQUEST, ApiException.PHONE_CODE_EXPIRED);
            if(getPhoneNumber().endsWith(NOT_REGISTERED_DIGIT) || getPhoneNumber().endsWith(INVITED_DIGIT)
                    || getPhoneNumber().endsWith(NOT_REGISTERED_EXPIRED2_DIGIT) || getPhoneNumber().endsWith(INVITED_EXPIRED2_DIGIT))
                throw new ApiException(ApiException.BAD_REQUEST, ApiException.PHONE_NUMBER_UNOCCUPIED);
            return me = new Me("Doe", "John", getPhoneNumber(), 0);
        } else {
            throw new ApiException(ApiException.BAD_REQUEST, code.isEmpty() ? ApiException.PHONE_CODE_EMPTY : ApiException.PHONE_CODE_INVALID );
        }
    }

    @Override
    protected Me signUpImpl(String code, String firstName, String lastName) throws IOException, ApiException {
        if(correctCode.equals(code)) {
            if(getPhoneNumber().endsWith(NOT_REGISTERED_EXPIRED2_DIGIT) || getPhoneNumber().endsWith(INVITED_EXPIRED2_DIGIT) ||
                    getPhoneNumber().endsWith(NOT_REGISTERED_EXPIRED_DIGIT) || getPhoneNumber().endsWith(INVITED_EXPIRED_DIGIT) )
                throw new ApiException(ApiException.BAD_REQUEST, ApiException.PHONE_CODE_EXPIRED);
            firstName = firstName.trim();
            lastName = lastName.trim();
            if(firstName.isEmpty())
                throw new ApiException(ApiException.BAD_REQUEST, ApiException.FIRSTNAME_INVALID);
            if(lastName.isEmpty())
                throw new ApiException(ApiException.BAD_REQUEST, ApiException.LASTNAME_INVALID);
            return me = new Me(firstName, lastName, getPhoneNumber(), 0);
        } else {
            throw new ApiException(ApiException.BAD_REQUEST, code.isEmpty() ? ApiException.PHONE_CODE_EMPTY : ApiException.PHONE_CODE_INVALID );
        }
    }

    @Override
    protected void logOutImpl() {

    }

    @Override
    protected void closeImpl() {

    }

    @Override
    protected State getStateImpl() throws IOException, ApiException {
        return new State() {
            @Override
            public boolean isTheSameAs(State state) {
                return true;
            }
        };
    }

    @Override
    protected Updates getUpdatesImpl(State state) throws IOException, ApiException {
        return new Updates(new LinkedHashMap<>(), new HashSet<>(), new ArrayList<>(),
                new HashMap<>(), new HashMap<>(), new LinkedHashSet<>(),
                new LinkedHashSet<>(), false, new State() {
            @Override
            public boolean isTheSameAs(State state) {
                return true;
            }
        });
    }

    @Override
    protected Updates getAsyncUpdatesImpl(State state, Collection<? extends Person> persons, Me me) throws IOException, ApiException {
        return getUpdatesImpl(state);
    }

    @Override
    protected Map<Integer, Date> getStatusesImpl(Collection<? extends Person> persons) throws IOException, ApiException {
        return Collections.EMPTY_MAP;
    }

    @Override
    protected BufferedImage[] getPhotosImpl(Person person, boolean small, boolean large) throws IOException, ApiException {
        return new BufferedImage[2];
    }

    @Override
    public void sendMessage(Person person, String text, long randomId) throws IOException, ApiException {
        throw new IOException("NotImplementedException");
    }

    @Override
    public void readMessages(Message lastMessage) throws IOException, ApiException {
        throw new IOException("NotImplementedException");
    }

    @Override
    public void receivedMessages(Message lastMessage) throws IOException, ApiException {
        throw new IOException("NotImplementedException");
    }

    @Override
    public void importContact(String phone, String firstName, String lastName) throws ApiException, IOException {
        throw new IOException("NotImplementedException");
    }

    @Override
    public void deleteContact(int contactId) throws ApiException, IOException {
        throw new IOException("NotImplementedException");
    }

    protected Map<Person, Message[]> data;

    protected Map<Person, Message[]> getData() throws IOException, ApiException {
        if(data == null) {
            data = new LinkedHashMap<>();

            Contact contact = new Contact("Doe", "Jane", "1234567890", 1);
            data.put(contact,
                new Message[] {
                    new Message(3, new Date(), "Nothing", false, contact, getMe()),
                    new Message(2, new Date(), "Something", true, contact, getMe()),
                    new Message(1, new Date(), "Anything", true, getMe(), contact)
            });

            contact = new Contact("Doe", "Jack", "0123456789", 2);
            data.put(contact, new Message[0]);

        }
        return data;
    }

    protected static int unread(Message[] messages) {
        int count = 0;
        for(int i = 0; i < messages.length; i++) {
            if(messages[i].isRead())
                break;
            else
                count++;
        }
        return count;
    }
}
