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

    private Status status = Status.INVALID;


    private String phoneNumber;

    @Override
    public void acceptNumber(String phoneNumber) throws IOException, ApiException {
        check(!isClosed() && !isLoggedIn());
        try {
            this.phoneNumber = phoneNumber;
            this.status = acceptNumberImpl();
        } catch (Throwable throwable) {
            this.phoneNumber = null;
            this.status = Status.INVALID;
            throw throwable;
        }
    }

    @Override
    public void sendCode() throws IOException, ApiException {
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
    public boolean isInvalid() {
        return status == Status.INVALID;
    }

    @Override
    public Me signIn(String code) throws IOException, ApiException {
        check(canSignIn() || canSignUp());
        Me me = signInImpl(code);
        status = Status.LOGGED_IN;
        return me;
    }

    @Override
    public Me signUp(String code, String firstName, String lastName) throws IOException, ApiException {
        check(canSignUp());
        Me me = signUpImpl(code, firstName, lastName);
        status = Status.LOGGED_IN;
        return me;
    }

    protected abstract Status acceptNumberImpl() throws IOException, ApiException;

    protected abstract void sendCodeImpl() throws IOException, ApiException;

    protected abstract Me signInImpl(String code) throws IOException, ApiException;

    protected abstract Me signUpImpl(String code, String firstName, String lastName) throws IOException, ApiException;

    protected abstract void logOutImpl() throws IOException, ApiException;

    protected abstract State getStateImpl() throws IOException, ApiException;

    protected abstract Updates getUpdatesImpl(State state) throws IOException, ApiException;

    protected abstract Updates getAsyncUpdatesImpl(State state, Collection<? extends Person> persons, Me me) throws IOException, ApiException;

    protected abstract void closeImpl() throws IOException, ApiException;

    protected abstract Map<Integer, Date> getStatusesImpl(Collection<? extends Person> persons) throws IOException, ApiException;

    protected abstract BufferedImage[] getPhotosImpl(Person person, boolean small, boolean large) throws IOException, ApiException;

    @Override
    public void logOut() throws IOException, ApiException {
        check(isLoggedIn());
        logOutImpl();
        status = Status.REGISTERED;
    }

    @Override
    public String getPhoneNumber() {
        return phoneNumber;
    }

    @Override
    public void close() throws IOException, ApiException {
        if (status != Status.CLOSED) {
            try {
                logOut();
            } catch (Exception e) {

            } finally {
                closeImpl();
                status = Status.CLOSED;
            }
        }
    }

    @Override
    public State getState() throws IOException, ApiException {
        check(isLoggedIn());
        return getStateImpl();
    }

    @Override
    public Updates getUpdates(State state) throws IOException, ApiException {
        check(isLoggedIn());
        return getUpdatesImpl(state);
    }

    @Override
    public Updates getAsyncUpdates(State state, Collection<? extends Person> persons, Me me) throws IOException, ApiException {
        check(isLoggedIn());
        return getAsyncUpdatesImpl(state, persons, me);
    }

    @Override
    public Map<Integer, Date> getStatuses(Collection<? extends Person> persons) throws IOException, ApiException {
        check(isLoggedIn());
        return getStatusesImpl(persons);
    }

    @Override
    public BufferedImage[] getPhotos(Person person, boolean small, boolean large) throws IOException, ApiException {
        check(isLoggedIn());
        return getPhotosImpl(person, small, large);
    }

    protected static void check(boolean cond) {
        if (!cond)
            throw new IllegalStateException();
    }
}
