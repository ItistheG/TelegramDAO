package org.javagram.model;

import org.javagram.dao.*;
import org.javagram.dao.proxy.changes.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;

/**
 * Created by HerrSergio on 23.08.2016.
 */
public interface TelegramModel extends AutoCloseable {
    Status getStatus();
    void acceptNumber(String phoneNumber) throws IOException, ApiException;
    void sendCode() throws IOException, ApiException;

    void acceptNumberAndSendCode(String phoneNumber) throws IOException, ApiException;

    String getPhoneNumber();

    void signIn(String code) throws IOException, ApiException;
    void signUp(String code, String firstName, String lastName) throws IOException, ApiException;
    void logOut() throws IOException, ApiException;
    boolean isLoggedIn();
    boolean canSignUp();
    boolean canSignIn();
    boolean isClosed();


    void close() throws IOException, ApiException;

    State getState() throws IOException, ApiException;

    List<Person> getPersons();
    public Dialog getDialog(Person person);

    public Map<Person, Dialog> getDialogs(boolean includeEmpty);

    public List<Message> getMessages(Person person, int count);

    public int getAvailableMessagesCount(Person person);

    public void squeezeMessages(Person person, int count);

    public UpdateChanges update();

    public UpdateChanges update(int updateStyle);

    public Me getMe();

    public Date onlineUntil(Person person);

    public boolean isOnline(Person person);

    public BufferedImage getPhoto(Person person, boolean small) throws IOException, ApiException;

    public void sendMessage(Person person, String text, long randomId) throws IOException, ApiException;

    public long sendMessage(Person person, String text) throws IOException, ApiException;

    public void readMessages(Message lastMessage) throws IOException, ApiException;

    public void receivedMessages(Message lastMessage) throws IOException, ApiException;

    public void importContact(String phone, String firstName, String lastName) throws IOException, ApiException;

    public void deleteContact(Contact contact) throws IOException, ApiException;

    public void deleteContact(int contactId) throws IOException, ApiException;
}
