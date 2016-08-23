package org.javagram.model;

import org.javagram.dao.*;
import org.javagram.dao.proxy.changes.*;
import org.javagram.response.InconsistentDataException;

import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by HerrSergio on 23.08.2016.
 */
public interface TelegramModel extends Closeable {
    Status getStatus();
    void acceptNumber(String phoneNumber) throws IOException;
    void sendCode() throws IOException;

    void acceptNumberAndSendCode(String phoneNumber) throws IOException;

    String getPhoneNumber();

    void signIn(String code) throws IOException;
    void signUp(String code, String firstName, String lastName) throws IOException;
    boolean logOut();
    boolean isLoggedIn();
    boolean canSignUp();
    boolean canSignIn();
    boolean isClosed();


    void close();

    State getState() throws IOException;

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

    public BufferedImage getPhoto(Person person, boolean small) throws IOException;

    public void sendMessage(Person person, String text, long randomId) throws IOException;

    public long sendMessage(Person person, String text) throws IOException;

    public void readMessages(Message lastMessage) throws IOException;

    public void receivedMessages(Message lastMessage) throws IOException;

    public boolean importContact(String phone, String firstName, String lastName);

    public boolean deleteContact(Contact contact);

    public boolean deleteContact(int contactId);
}
