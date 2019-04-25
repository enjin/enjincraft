package com.enjin.enjincoin.spigot_framework.controllers;

import com.enjin.enjincoin.sdk.Client;
import com.enjin.enjincoin.sdk.Clients;
import com.enjin.enjincoin.spigot_framework.BasePlugin;
import com.google.gson.JsonObject;
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
     * Config key for the secret to use with the trusted platform.
     */
    public static final String SECRET = "secret";

    /**
     * Config key for the UUID in an individual users session entry.
     */
    public static final String UUID = "uuid";

    private final BasePlugin plugin;

    private final JsonObject config;

    private Client client;

    /**
     * <p>Controller constructor.</p>
     *
     * @param plugin   the Spigot plugin
     * @param config the bootstrap config
     */
    public SdkClientController(BasePlugin plugin, JsonObject config) {
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * <p>Initialization mechanism for this controller.</p>
     *
     * @since 1.0
     */
    public void setUp() throws IOException {
        if (!config.has(PLATFORM_BASE_URL))
            throw new IllegalStateException(String.format("The \"%s\" key does not exists in the config.", PLATFORM_BASE_URL));
        if (!config.has(APP_ID))
            throw new IllegalStateException(String.format("The \"%s\" key does not exists in the config.", APP_ID));
        if (!config.has(SECRET))
            throw new IllegalStateException(String.format("The \"%s\" key does not exists in the config.", APP_ID));
        this.client = Clients.createClient(this.config.get(PLATFORM_BASE_URL).getAsString(),
                this.plugin.getBootstrap().getAppId(), this.plugin.getBootstrap().isSDKDebuggingEnabled());
        this.client.auth(this.config.get(SECRET).getAsString());
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
            this.plugin.getLogger().log(Level.WARNING, "An error occurred while shutting down the Enjin Coin client.", e);
        }
    }

    /**
     * <p>Returns the Enjin Coin SDK client.</p>
     *
     * @return the client or null if not initialized
     * @since 1.0
     */
    public Client getClient() {
        return client;
    }
}
