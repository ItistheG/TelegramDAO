package org.javagram.dao;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by HerrSergio on 20.02.2017.
 */
public class ApiException extends Exception {
    public static final String PHONE_NUMBER_INVALID = "PHONE_NUMBER_INVALID",
            PHONE_CODE_EMPTY = "PHONE_CODE_EMPTY",
            PHONE_CODE_EXPIRED = "PHONE_CODE_EXPIRED",
            PHONE_CODE_INVALID = "PHONE_CODE_INVALID",

            FIRSTNAME_INVALID = "FIRSTNAME_INVALID",
            LASTNAME_INVALID = "LASTNAME_INVALID",
            PHONE_NUMBER_UNOCCUPIED = "PHONE_NUMBER_UNOCCUPIED",
            UNKNOWN = "UNKNOWN";

    static final int BAD_REQUEST = 400;

    public Pattern PHONE_MIGRATE_X = Pattern.compile("PHONE_MIGRATE_(\\d+)"),
            NETWORK_MIGRATE_X =  Pattern.compile("NETWORK_MIGRATE_(\\d+)"),
            MIGRATE_X =  Pattern.compile("(?:NETWORK|PHONE)_MIGRATE|_(\\d+)");

    private int code = 0;

    public ApiException() {
        this(0, UNKNOWN);
    }

    public ApiException(int code, String message) {
        super(message);
        this.code = code;
    }

    public boolean isPhoneNumberInvalid() {
        return getMessage().equals(PHONE_NUMBER_INVALID);
    }

    public boolean isPhoneNumberUnoccupied() {
        return getMessage().equals(PHONE_NUMBER_UNOCCUPIED);
    }

    public boolean isCodeExpired() {
        return getMessage().equals(PHONE_CODE_EXPIRED);
    }

    public boolean isCodeInvalid() {
        return getMessage().equals(PHONE_CODE_INVALID);
    }

    public boolean isCodeEmpty() {
        return getMessage().equals(PHONE_CODE_EMPTY);
    }

    public boolean isCodeInvalidOrEmpty() {
        return isCodeEmpty() || isCodeInvalid();
    }

    public boolean isNameInvalid() {
        return getMessage().equals(FIRSTNAME_INVALID) || getMessage().equals(LASTNAME_INVALID);
    }

    public Integer migration() {
        Matcher matcher = MIGRATE_X.matcher(getMessage());
        if(matcher.matches()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }
}
