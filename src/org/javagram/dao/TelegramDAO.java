package org.javagram.dao;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;

/**
 * Created by HerrSergio on 06.05.2016.
 */
public interface TelegramDAO extends AutoCloseable {
    Status getStatus();
    void acceptNumber(String phoneNumber) throws IOException, ApiException;
    void sendCode() throws IOException, ApiException;

    default void acceptNumberAndSendCode(String phoneNumber) throws IOException, ApiException {
        acceptNumber(phoneNumber);
        sendCode();
    }

    String getPhoneNumber();

    Me signIn(String code) throws IOException, ApiException;
    Me signUp(String code, String firstName, String lastName) throws IOException, ApiException;
    void logOut() throws IOException, ApiException;
    boolean isLoggedIn();
    boolean canSignUp();
    boolean canSignIn();
    boolean isClosed();
    boolean isInvalid();
    Me getMe() throws IOException, ApiException;

    ArrayList<Contact> getContacts() throws IOException, ApiException;
    ArrayList<Dialog> getDialogs() throws  IOException, ApiException;
    ArrayList<Message> getMessagesOfContact(int id, int lastMessageId, int limit) throws IOException, ApiException;
    ArrayList<Message> getMessagesOfForeign(int id, long accessHash, int lastMessageId, int limit) throws IOException, ApiException;

    default ArrayList<Message> getMessages(Person person, Message last, int limit) throws IOException, ApiException {
        int lastMessageId = 0;
        if(last != null)
            lastMessageId = last.getId();
        if(person instanceof Contact) {
            return getMessagesOfContact(person.getId(), lastMessageId, limit);
        } else if(person instanceof Foreign) {
            return getMessagesOfForeign(person.getId(), ((Foreign) person).getAccessHash(), lastMessageId, limit);
        } else {
            throw new IllegalArgumentException();
        }
    }

    default LinkedHashMap<Person, Dialog> getList(boolean excludeForeign, boolean excludeEmpty) throws IOException, ApiException {
        LinkedHashMap<Person, Dialog> list = new LinkedHashMap<>();
        for(Dialog dialog : getDialogs()) {
            if(dialog.getBuddy() instanceof Foreign && excludeForeign)
                continue;
            list.put(dialog.getBuddy(), dialog);
        }
        if(!excludeEmpty) {
            for (Contact contact : getContacts()) {
                list.putIfAbsent(contact, null);

            }
        }
        return list;
    }

    void close() throws IOException, ApiException;

    State getState() throws IOException, ApiException;
    Updates getUpdates(State state) throws IOException, ApiException;
    Updates getAsyncUpdates(State state, Collection<?extends Person> persons, Me me) throws IOException, ApiException;

    Map<Integer, Date> getStatuses(Collection<? extends Person> persons) throws IOException, ApiException;
    BufferedImage[] getPhotos(Person person, boolean small, boolean large) throws IOException, ApiException;

    void sendMessage(Person person, String text, long randomId) throws IOException, ApiException;
    void readMessages(Message lastMessage) throws IOException, ApiException;
    void receivedMessages(Message lastMessage) throws IOException, ApiException;

    //boolean setTyping(Person person, boolean typing);

    default long sendMessage(Person person, String text) throws IOException, ApiException {
        long randomId = Math.round(Math.random() * 0x100000000L);
        randomId <<= 32;
        randomId |= Math.round(Math.random() * 0x100000000L);
        sendMessage(person, text, randomId);
        return randomId;
    }

    void importContact(String phone, String firstName, String lastName) throws ApiException, IOException;
    void deleteContact(int contactId) throws IOException, ApiException;
    default void deleteContact(Contact contact) throws IOException, ApiException {
        deleteContact(contact.getId());
    }
}
