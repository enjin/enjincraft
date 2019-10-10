package com.enjin.enjincraft.spigot;

public class AuthenticationException extends NetworkException {

    public static final String AUTHENTICATION_EXCEPTION_MESSAGE = "TP Authentication Failed";

    public AuthenticationException(Throwable throwable) {
        super(AUTHENTICATION_EXCEPTION_MESSAGE, throwable);
    }

    public AuthenticationException(int code) {
        super(code, AUTHENTICATION_EXCEPTION_MESSAGE);
    }

}
