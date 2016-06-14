package org.javagram.dao;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * Created by HerrSergio on 06.05.2016.
 */
public abstract class AbstractTelegramDAO implements TelegramDAO {

    private Status status = Status.NOT_INITIALIZED;


    private String phoneNumber;

    @Override
    public void acceptNumber(String phoneNumber) throws IOException {
        check(!isClosed() && !isLoggedIn());
        this.phoneNumber = phoneNumber;
        this.status = acceptNumberImpl();
    }

    @Override
    public void sendCode() throws IOException {
        check(canSignIn() || canSignUp());
        sendCodeImpl();
    }

        @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public boolean isLoggedIn() {
        return status == Status.LOGGED_IN;
    }

    @Override
    public boolean canSignUp() {
        return status == Status.INVITED || status == Status.NOT_REGISTERED;
    }

    @Override
    public boolean canSignIn() {
        return status == Status.REGISTERED;
    }

    @Override
    public boolean isClosed() {
        return status == Status.CLOSED;
    }

    @Override
    public Me signIn(String code) throws IOException {
        check(canSignIn());
        Me me = signInImpl(code);
        status = Status.LOGGED_IN;
        return me;
    }

    @Override
    public Me signUp(String code, String firstName, String lastName) throws IOException {
        check(canSignUp());
        Me me = signUpImpl(code, firstName, lastName);
        status = Status.LOGGED_IN;
        return me;
    }

    protected abstract Status acceptNumberImpl() throws IOException;
    protected abstract void sendCodeImpl() throws IOException;
    protected abstract Me signInImpl(String code) throws IOException;
    protected abstract Me signUpImpl(String code, String firstName, String lastName) throws IOException;
    protected abstract boolean logOutImpl();
    protected abstract State getStateImpl() throws IOException;
    protected abstract Updates getUpdatesImpl(State state) throws IOException;
    protected abstract void closeImpl();
    protected abstract Map<Integer, Date> getStatusesImpl(Collection<? extends Person> persons) throws IOException;
    protected abstract BufferedImage[] getPhotosImpl(Person person, boolean small, boolean large) throws IOException;

    @Override
    public boolean logOut() {
        if(!isLoggedIn())
            return true;
        if(logOutImpl()) {
            status = Status.REGISTERED;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String getPhoneNumber() {
        return phoneNumber;
    }

    @Override
    public void close() {
        if(status != Status.CLOSED) {
            logOut();
            closeImpl();
            status = Status.CLOSED;
        }
    }

    @Override
    public State getState() throws IOException {
        check(isLoggedIn());
        return getStateImpl();
    }

    @Override
    public Updates getUpdates(State state) throws IOException {
        check(isLoggedIn());
        return getUpdatesImpl(state);
    }

    @Override
    public Map<Integer, Date> getStatuses(Collection<? extends Person> persons) throws IOException {
        check(isLoggedIn());
        return getStatusesImpl(persons);
    }

    @Override
    public BufferedImage[] getPhotos(Person person, boolean small, boolean large) throws IOException {
        check(isLoggedIn());
        return getPhotosImpl(person, small, large);
    }

    protected static void check(boolean cond) {
        if(!cond)
            throw new IllegalStateException();
    }
}
