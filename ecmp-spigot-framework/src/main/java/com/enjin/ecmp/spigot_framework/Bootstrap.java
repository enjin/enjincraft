package com.enjin.ecmp.spigot_framework;

import com.enjin.ecmp.spigot_framework.controllers.SdkClientController;
import com.enjin.ecmp.spigot_framework.player.PlayerManager;
import com.enjin.ecmp.spigot_framework.trade.TradeManager;
import com.google.gson.JsonObject;

import java.util.logging.Logger;

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
     * @since 1.0
     */
    SdkClientController getSdkController();

    /**
     * <p>Returns the Player Manager that handles the fetching
     * and User and Identity data and initialization of MinecraftPlayers.</p>
     *
     * @return PlayerManager instance
     * @since 1.0
     */
    PlayerManager getPlayerManager();

    TradeManager getTradeManager();

    /**
     * <p>Returns the config for this bootstrap.</p>
     *
     * @return the config
     * @since 1.0
     */
    EcmpConfig getConfig();

    /**
     * <p>Logs a debug message if debug mode is enabled.</p>
     *
     * @param log the message to log
     * @since 1.0
     */
    void debug(String log);

    Logger getLogger();

}
