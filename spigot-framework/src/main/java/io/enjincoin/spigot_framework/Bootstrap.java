package io.enjincoin.spigot_framework;

import com.google.gson.JsonObject;
import io.enjincoin.sdk.client.service.identities.vo.Identity;
import io.enjincoin.sdk.client.service.tokens.vo.Token;
import io.enjincoin.spigot_framework.controllers.SdkClientController;

import java.util.Map;
import java.util.UUID;

/**
 * <p>An entry-point for the Enjin Coin Minecraft framework. Serves
 * as a hub for the core functions of the framework.</p>
 *
 * @since 1.0
 */
public interface Bootstrap {

    /**
     * <p>Initialization mechanism for this bootstrap.</p>
     *
     * @since 1.0
     */
    void setUp();

    /**
     * <p>Cleanup mechanism for this bootstrap.</p>
     *
     * @since 1.0
     */
    void tearDown();

    /**
     * <p>Returns the Enjin Coin SDK client controller.</p>
     *
     * @return the sdk client controller
     *
     * @since 1.0
     */
    SdkClientController getSdkController();

    /**
     * <p>Returns a mapping of trusted platform identities associated
     * with the UUIDs of online players.</p>
     *
     * @return a map of UUIDs and associated identities of online players
     *
     * @since 1.0
     */
    Map<UUID, Identity> getIdentities();

    /**
     * <p>Returns a mapping of token IDs and the associated token data.</p>
     *
     * @return a map of token IDs and associated data
     *
     * @since 1.0
     */
    Map<Integer, Token> getTokens();

    /**
     * <p>Returns the config for this bootstrap.</p>
     *
     * @return the config
     *
     * @since 1.0
     */
    JsonObject getConfig();

    /**
     * <p>Returns whether debug mode is enabled or not.</p>
     *
     * @return true if debug mode is enabled
     *
     * @since 1.0
     */
    boolean isDebugEnabled();

    /**
     * <p>Logs a debug message if debug mode is enabled.</p>
     *
     * @param log the message to log
     *
     * @since 1.0
     */
    void debug(String log);

}
