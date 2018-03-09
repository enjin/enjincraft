package io.enjincoin.spigot_framework.controllers;

import io.enjincoin.sdk.client.Client;
import io.enjincoin.sdk.client.Clients;
import io.enjincoin.sdk.client.config.Config;
import io.enjincoin.spigot_framework.BasePlugin;

import java.io.IOException;
import java.util.logging.Level;

public class SdkClientController {

    private final BasePlugin main;
    private Client client;

    public SdkClientController(BasePlugin main) {
        this.main = main;
    }

    public void setUp() {
        this.client = Clients.createClient(this.main.getBaseUrl());
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
