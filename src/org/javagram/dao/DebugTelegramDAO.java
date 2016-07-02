package org.javagram.dao;

import org.javagram.response.object.UserContact;

import java.awt.event.ContainerAdapter;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by HerrSergio on 06.05.2016.
 */
public class DebugTelegramDAO extends AbstractTelegramDAO {

    private Me me;

    @Override
    protected Status acceptNumberImpl() throws IOException {
        if(getPhoneNumber().isEmpty())
            return Status.NOT_REGISTERED;
        else
            return Status.REGISTERED;
    }

    @Override
    protected void sendCodeImpl() throws IOException {
        System.out.println("Yer kode iz " + correctCode);
    }

    @Override
    public Me getMe() throws IOException {
        check(isLoggedIn());
        return me;
    }

    @Override
    public ArrayList<Contact> getContacts() throws IOException {
        Stream<Person> persons = getData().keySet().stream().filter(p -> p instanceof Contact);
        Stream<Contact> contacts = persons.map(p -> (Contact)p);
        return new ArrayList<>(contacts.collect(Collectors.toList()));
    }

    @Override
    public ArrayList<Dialog> getDialogs() throws IOException {
        ArrayList<Dialog> dialogs = new ArrayList<>();
        for(Map.Entry<Person, Message[]> entry : getData().entrySet()) {
            if(entry.getValue().length > 0) {
                dialogs.add(new Dialog(entry.getKey(), unread(entry.getValue()), entry.getValue()[0]));
            }
        }
        return dialogs;
    }

    @Override
    public ArrayList<Message> getMessagesOfContact(int id, int lastMessageId, int limit) throws IOException {
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
    public ArrayList<Message> getMessagesOfForeign(int id, long accessHash, int lastMessageId, int limit) throws IOException {
        return new ArrayList<>();
    }

    private static final String correctCode = "00000";

    @Override
    protected Me signInImpl(String code) throws IOException {
        if(correctCode.equals(code)) {
            return me = new Me("Doe", "John", getPhoneNumber(), 0);
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    protected Me signUpImpl(String code, String firstName, String lastName) throws IOException {
        if(correctCode.equals(code)) {
            return me = new Me(firstName, lastName, getPhoneNumber(), 0);
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    protected boolean logOutImpl() {
        return true;
    }

    @Override
    protected void closeImpl() {

    }

    @Override
    protected State getStateImpl() throws IOException {
        return new State() {
            @Override
            public boolean isTheSameAs(State state) {
                return true;
            }
        };
    }

    @Override
    protected Updates getUpdatesImpl(State state) throws IOException {
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
    protected Updates getAsyncUpdatesImpl(State state, Collection<? extends Person> persons, Me me) throws IOException {
        return getUpdatesImpl(state);
    }

    @Override
    protected Map<Integer, Date> getStatusesImpl(Collection<? extends Person> persons) throws IOException {
        return Collections.EMPTY_MAP;
    }

    @Override
    protected BufferedImage[] getPhotosImpl(Person person, boolean small, boolean large) throws IOException {
        return new BufferedImage[2];
    }

    @Override
    public void sendMessage(Person person, String text, long randomId) throws IOException {
        throw new IOException("NotImplementedException");
    }

    @Override
    public void readMessages(Message lastMessage) throws IOException {
        throw new IOException("NotImplementedException");
    }

    @Override
    public void receivedMessages(Message lastMessage) throws IOException {
        throw new IOException("NotImplementedException");
    }

    @Override
    public boolean importContact(String phone, String firstName, String lastName) {
        return false;
    }

    @Override
    public boolean deleteContact(int contactId) {
        return false;
    }

    protected Map<Person, Message[]> data;

    protected Map<Person, Message[]> getData() throws IOException {
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
