package io.enjincoin.spigot_framework.controllers;

import com.google.gson.JsonObject;
import io.enjincoin.sdk.client.Client;
import io.enjincoin.sdk.client.Clients;
import io.enjincoin.sdk.client.config.Config;
import io.enjincoin.spigot_framework.BasePlugin;

import java.io.IOException;
import java.util.logging.Level;

public class SdkClientController {

    public static final String PLATFORM_BASE_URL = "platformBaseUrl";

    private final BasePlugin main;
    private final JsonObject config;
    private Client client;

    public SdkClientController(BasePlugin main, JsonObject config) {
        this.main = main;
        this.config = config;
    }

    public void setUp() {
        if (!config.has(PLATFORM_BASE_URL))
            throw new IllegalStateException(String.format("The \"%s\" key does not exists in the config.", PLATFORM_BASE_URL));
        this.client = Clients.createClient(this.config.get(PLATFORM_BASE_URL).getAsString());
    }

    public void tearDown() {
         try {
             this.client.close();
         } catch (IOException e) {
             this.main.getLogger().log(Level.WARNING, "An error occurred while shutting down the Enjin Coin client.", e);
         }
    }

    public Client getClient() {
        return client;
    }
}
