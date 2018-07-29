package com.enjin.enjincoin.spigot_framework;

import com.enjin.enjincoin.spigot_framework.controllers.ConversationManager;
import com.enjin.enjincoin.spigot_framework.player.PlayerManager;
import com.enjin.minecraft_commons.spigot.ui.MenuAPI;
import com.google.gson.JsonObject;
import com.enjin.enjincoin.sdk.client.service.tokens.vo.Token;
import com.enjin.enjincoin.spigot_framework.controllers.SdkClientController;

import java.util.Map;

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
     * <p>Returns the ConversationManager instance.</p>
     *
     * @return the conversation manager
     *
     * @since 1.0
     */
    ConversationManager getConversationManager();

    /**
     * <p>Returns the MenuAPI instance.</p>
     *
     * @return the menu api
     *
     * @since 1.0
     */
    MenuAPI getMenuAPI();

    /**
     * <p>Returns the Enjin Coin SDK client controller.</p>
     *
     * @return the sdk client controller
     *
     * @since 1.0
     */
    SdkClientController getSdkController();

    /**
     * <p>Returns the Player Manager that handles the fetching
     * and User and Identity data and initialization of MinecraftPlayers.</p>
     *
     * @return PlayerManager instance
     *
     * @since 1.0
     */
    PlayerManager getPlayerManager();

    /**
     * <p>Returns a mapping of token IDs and the associated token data.</p>
     *
     * @return a map of token IDs and associated data
     *
     * @since 1.0
     */
    Map<String, Token> getTokens();

    /**
     * <p>Returns the config for this bootstrap.</p>
     *
     * @return the config
     *
     * @since 1.0
     */
    JsonObject getConfig();

    /**
     * <p>Returns the configured App ID to use.</p>
     *
     * @return configured App ID as Integer
     *
     * @since 1.0
     */
    Integer getAppId();

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
