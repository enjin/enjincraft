package com.enjin.ecmp.spigot_framework;

import com.enjin.ecmp.spigot_framework.commands.RootCommand;
import com.enjin.ecmp.spigot_framework.controllers.SdkClientController;
import com.enjin.ecmp.spigot_framework.listeners.InventoryListener;
import com.enjin.ecmp.spigot_framework.player.PlayerManager;
import com.enjin.ecmp.spigot_framework.trade.TradeManager;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.util.logging.Logger;

public class SpigotBootstrap extends PluginBootstrap {

    private final BasePlugin plugin;
    private EcmpConfig config;

    private SdkClientController sdkClientController;
    private PlayerManager playerManager;
    private TradeManager tradeManager;

    public SpigotBootstrap(BasePlugin plugin) {
        this.plugin = plugin;
    }

    public BasePlugin getPlugin() {
        return plugin;
    }

    @Override
    public void setUp() {
        this.config = new EcmpConfig(plugin);

        this.playerManager = new PlayerManager(this.plugin);
        this.tradeManager = new TradeManager(this.plugin);

        try {
            // Initialize the Enjin Coin SDK controller with the provided Spigot plugin and the config.
            this.sdkClientController = new SdkClientController(this);
            this.sdkClientController.setUp();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Register Listeners
        Bukkit.getPluginManager().registerEvents(this.playerManager, this.plugin);
        Bukkit.getPluginManager().registerEvents(this.tradeManager, this.plugin);
        Bukkit.getPluginManager().registerEvents(new InventoryListener(this.plugin), this.plugin);

        // Register Commands
        this.plugin.getCommand("enj").setExecutor(new RootCommand(this.plugin));
    }

    @Override
    public void tearDown() {
        this.sdkClientController.tearDown();
        this.sdkClientController = null;
    }

    @Override
    public SdkClientController getSdkController() {
        return this.sdkClientController;
    }

    @Override
    public PlayerManager getPlayerManager() {
        return this.playerManager;
    }

    @Override
    public TradeManager getTradeManager() {
        return this.tradeManager;
    }

    @Override
    public EcmpConfig getConfig() {
        return config;
    }

    @Override
    public void debug(String log) {
        if (config.isPluginDebugging())
            getLogger().info(log);
    }

    @Override
    public Logger getLogger() {
        return plugin.getLogger();
    }
}
