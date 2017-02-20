package org.javagram.model;

import org.javagram.dao.*;
import org.javagram.dao.proxy.TelegramProxy;
import org.javagram.dao.proxy.changes.UpdateChanges;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by HerrSergio on 23.08.2016.
 */
public abstract class AbstractTelegramModel implements TelegramModel {

    private TelegramDAO telegramDAO;
    private TelegramProxy telegramProxy;

    private void checkProxy(boolean exist) {
        if(telegramProxy == null && exist || telegramProxy != null && !exist)
            throw new IllegalStateException();
    }

    public AbstractTelegramModel(TelegramDAO telegramDAO) {
        this.telegramDAO = telegramDAO;
    }

    @Override
    public Status getStatus() {
        return telegramDAO.getStatus();
    }

    @Override
    public void acceptNumber(String phoneNumber) throws IOException, ApiException {
        checkProxy(false);
        telegramDAO.acceptNumber(phoneNumber);
    }

    @Override
    public void sendCode() throws IOException, ApiException {
        checkProxy(false);
        telegramDAO.sendCode();
    }

    @Override
    public void acceptNumberAndSendCode(String phoneNumber) throws IOException, ApiException {
        checkProxy(false);
        telegramDAO.acceptNumber(phoneNumber);
    }

    @Override
    public String getPhoneNumber() {
        return telegramDAO.getPhoneNumber();
    }

    @Override
    public void signIn(String code) throws IOException, ApiException {
        checkProxy(false);
        Me me = telegramDAO.signIn(code);
        telegramProxy = new TelegramProxy(telegramDAO);
        //return me;
    }

    @Override
    public void signUp(String code, String firstName, String lastName) throws IOException, ApiException {
        checkProxy(false);
        Me me = telegramDAO.signUp(code, firstName, lastName);
        telegramProxy = new TelegramProxy(telegramDAO);
        //return me;
    }

    @Override
    public void logOut() throws IOException, ApiException {
        telegramProxy = null;
        telegramDAO.logOut();
    }

    @Override
    public boolean isLoggedIn() {
        return telegramDAO.isLoggedIn();
    }

    @Override
    public boolean canSignUp() {
        return telegramDAO.canSignUp();
    }

    @Override
    public boolean canSignIn() {
        return telegramDAO.canSignUp();
    }

    @Override
    public boolean isClosed() {
        return telegramDAO.isClosed();
    }

    @Override
    public void close() throws IOException, ApiException  {
        telegramProxy = null;
        telegramDAO.close();
    }

    @Override
    public State getState() throws IOException, ApiException {
        return telegramDAO.getState();
    }

    @Override
    public List<Person> getPersons() {
        checkProxy(true);
        return telegramProxy.getPersons();
    }

    @Override
    public Dialog getDialog(Person person) {
        checkProxy(true);
        return telegramProxy.getDialog(person);
    }

    @Override
    public Map<Person, Dialog> getDialogs(boolean includeEmpty) {
        checkProxy(true);
        return telegramProxy.getDialogs(includeEmpty);
    }

    @Override
    public List<Message> getMessages(Person person, int count) {
        checkProxy(true);
        return telegramProxy.getMessages(person, count);
    }

    @Override
    public int getAvailableMessagesCount(Person person) {
        checkProxy(true);
        return telegramProxy.getAvailableMessagesCount(person);
    }

    @Override
    public void squeezeMessages(Person person, int count) {
        checkProxy(true);
        telegramProxy.squeezeMessages(person, count);
    }

    @Override
    public UpdateChanges update() {
        checkProxy(true);
        return telegramProxy.update();
    }

    @Override
    public UpdateChanges update(int updateStyle) {
        checkProxy(true);
        return telegramProxy.update(updateStyle);
    }

    @Override
    public Me getMe() {
        checkProxy(true);
        return telegramProxy.getMe();
    }

    @Override
    public Date onlineUntil(Person person) {
        checkProxy(true);
        return telegramProxy.onlineUntil(person);
    }

    @Override
    public boolean isOnline(Person person) {
        checkProxy(true);
        return telegramProxy.isOnline(person);
    }

    @Override
    public BufferedImage getPhoto(Person person, boolean small) throws IOException, ApiException {
        checkProxy(true);
        return telegramProxy.getPhoto(person, small);
    }

    @Override
    public void sendMessage(Person person, String text, long randomId) throws IOException, ApiException {
        checkProxy(true);
        telegramProxy.sendMessage(person, text, randomId);
    }

    @Override
    public long sendMessage(Person person, String text) throws IOException, ApiException {
        checkProxy(true);
        return telegramProxy.sendMessage(person, text);
    }

    @Override
    public void readMessages(Message lastMessage) throws IOException, ApiException {
        checkProxy(true);
        telegramProxy.readMessages(lastMessage);
    }

    @Override
    public void receivedMessages(Message lastMessage) throws IOException, ApiException {
        checkProxy(true);
        telegramProxy.receivedMessages(lastMessage);
    }

    @Override
    public void importContact(String phone, String firstName, String lastName) throws IOException, ApiException {
        checkProxy(true);
        telegramProxy.importContact(phone, firstName, lastName);
    }

    @Override
    public void deleteContact(Contact contact) throws IOException, ApiException {
        checkProxy(true);
        telegramProxy.deleteContact(contact);
    }

    @Override
    public void deleteContact(int contactId) throws IOException, ApiException {
        checkProxy(true);
        telegramProxy.deleteContact(contactId);
    }
}
