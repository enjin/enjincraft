package com.enjin.ecmp.spigot;

public class NetworkException extends RuntimeException {

    private static final String NETWORK_EXCEPTION_MESSAGE = "HTTP Request Failed";

    public NetworkException(Throwable throwable) {
        this(NETWORK_EXCEPTION_MESSAGE, throwable);
    }

    public NetworkException(int code) {
        this(code, NETWORK_EXCEPTION_MESSAGE);
    }

    public NetworkException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public NetworkException(int code, String message) {
        super(String.format("Error Code %s - %s", code, message));
    }

}
