package com.enjin.enjincoin.spigot_framework.controllers;

import com.google.gson.JsonObject;
import com.enjin.enjincoin.sdk.client.Client;
import com.enjin.enjincoin.sdk.client.Clients;
import com.enjin.enjincoin.spigot_framework.BasePlugin;
import org.apache.commons.lang.NullArgumentException;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * <p>Controller for the Enjin Coin SDK client.</p>
 *
 * @since 1.0
 */
public class SdkClientController {

    /**
     * Config key for the base url of the trusted platform.
     */
    public static final String PLATFORM_BASE_URL = "platformBaseUrl";

    /**
     * Config key for the app id to use with the trusted platform.
     */
    public static final String APP_ID = "appId";

    /**
     * Config key for the UUID in an individual users session entry.
     */
    public static final String UUID = "uuid";

    /**
     * <p>The spigot plugin.</p>
     */
    private final BasePlugin main;

    /**
     * <p>The cached bootstrap config.</p>
     */
    private final JsonObject config;

    /**
     * <p>The Enjin Coin SDK client.</p>
     */
    private Client client;

    /**
     * <p>A UUID session/client map for user connections</p>
     */
    private Map<UUID, Client> sessionMap;

    /**
     * <p>Controller constructor.</p>
     *
     * @param main the Spigot plugin
     * @param config the bootstrap config
     */
    public SdkClientController(BasePlugin main, JsonObject config) {
        this.main = main;
        this.config = config;
    }

    /**
     * <p>Initialization mechanism for this controller.</p>
     *
     * @since 1.0
     */
    public void setUp() {
        if (!config.has(PLATFORM_BASE_URL))
            throw new IllegalStateException(String.format("The \"%s\" key does not exists in the config.", PLATFORM_BASE_URL));
        if (!config.has(APP_ID))
            throw new IllegalStateException(String.format("The \"%s\" key does not exists in the config.", APP_ID));
        this.client = Clients.createClient(this.config.get(PLATFORM_BASE_URL).getAsString(),
                this.main.getBootstrap().getAppId(), this.main.getBootstrap().isDebugEnabled());
    }

    /**
     * <p>Initialize and add a user's session to the sessions map</p>
     *
     * @since #.#
     */
    public void addSession(UUID uuid) {
        if (uuid == null)
            throw new NullArgumentException(String.format("\"$s\" is null.", UUID));
        if (!config.has(PLATFORM_BASE_URL))
            throw new IllegalStateException(String.format("The \"%s\" key does not exists in the config.", PLATFORM_BASE_URL));
        if (!config.has(APP_ID))
            throw new IllegalStateException(String.format("The \"%s\" key does not exists in the config.", APP_ID));

        Client session = Clients.createClient(this.config.get(PLATFORM_BASE_URL).getAsString(),
                this.config.get(APP_ID).getAsInt(), this.main.getBootstrap().isDebugEnabled());
    }

    /**
     * <p>Cleanup mechanism for this controller.</p>
     *
     * @since 1.0
     */
    public void tearDown() {
         try {
             this.client.close();
         } catch (IOException e) {
             this.main.getLogger().log(Level.WARNING, "An error occurred while shutting down the Enjin Coin client.", e);
         }
    }

    /**
     * <p>Returns the Enjin Coin SDK client.</p>
     *
     * @return the client or null if not initialized
     *
     * @since 1.0
     */
    public Client getClient() {
        return client;
    }
}
