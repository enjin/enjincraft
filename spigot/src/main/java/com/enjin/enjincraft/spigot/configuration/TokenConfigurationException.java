package com.enjin.enjincraft.spigot.configuration;

public class TokenConfigurationException extends RuntimeException {

    private static final String MESSAGE = "Token definition for token id %s is missing the %s field";

    public TokenConfigurationException(String tokenId, String field) {
        super(String.format(MESSAGE, tokenId, field));
    }

}
