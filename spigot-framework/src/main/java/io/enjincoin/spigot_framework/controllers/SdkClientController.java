package io.enjincoin.spigot_framework.controllers;

import io.enjincoin.sdk.client.Client;
import io.enjincoin.sdk.client.Clients;
import io.enjincoin.sdk.client.config.Config;
import io.enjincoin.spigot_framework.BasePlugin;

public class SdkClientController {

    private final BasePlugin main;
    private Client client;

    public SdkClientController(BasePlugin main) {
        this.main = main;
    }

    public void setUp() {
        Config sdkConfig = this.main.getSdkConfig();
        if (sdkConfig == null) {
            this.main.getLogger().severe("Failed to set up sdk client because no config was loaded.");
        } else {
            this.client = Clients.create(sdkConfig);
        }
    }

    public void tearDown() {
         this.client.close();
    }

    public Client getClient() {
        return client;
    }
}
