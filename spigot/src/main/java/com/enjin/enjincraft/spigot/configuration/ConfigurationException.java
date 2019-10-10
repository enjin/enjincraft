package com.enjin.enjincraft.spigot.configuration;

public class ConfigurationException extends RuntimeException {

    public ConfigurationException(Throwable throwable) {
        super(throwable);
    }

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable throwable) {
        super(message, throwable);
    }

}
