package org.javagram.dao.proxy.changes;

/**
 * Created by HerrSergio on 21.05.2016.
 */
public class UpdateException extends RuntimeException {

    public UpdateException() {

    }

    public UpdateException(Exception e) {
        super(e);
    }
}
