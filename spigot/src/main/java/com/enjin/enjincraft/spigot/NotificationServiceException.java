package com.enjin.enjincraft.spigot;

public class NotificationServiceException extends RuntimeException {

    private static final String MESSAGE = "Unable to start notification service";

    public NotificationServiceException(Throwable throwable) {
        super(MESSAGE, throwable);
    }

}
