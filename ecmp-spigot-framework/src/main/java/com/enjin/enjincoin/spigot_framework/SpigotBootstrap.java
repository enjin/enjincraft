package com.enjin.enjincoin.spigot_framework;

import com.enjin.enjincoin.sdk.model.service.tokens.GetTokens;
import com.enjin.enjincoin.sdk.model.service.tokens.Token;
import com.enjin.enjincoin.sdk.service.tokens.TokensService;
import com.enjin.enjincoin.spigot_framework.commands.RootCommand;
import com.enjin.enjincoin.spigot_framework.controllers.SdkClientController;
import com.enjin.enjincoin.spigot_framework.listeners.InventoryListener;
import com.enjin.enjincoin.spigot_framework.player.PlayerManager;
import com.enjin.enjincoin.spigot_framework.trade.TradeManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class SpigotBootstrap extends PluginBootstrap {

    private final BasePlugin main;

    private Integer appId;
    private BigInteger devIdentityId;
    private boolean allowVanillaItemsInTrades;
    private boolean sdkDebug;
    private boolean pluginDebug;
    private Map<String, Token> tokens;

    private SdkClientController sdkClientController;
    private PlayerManager playerManager;
    private TradeManager tradeManager;

    public SpigotBootstrap(BasePlugin main) {
        this.main = main;
    }

    @Override
    public void setUp() {
        this.playerManager = new PlayerManager(this.main);
        this.tradeManager = new TradeManager(this.main);
        this.tokens = new ConcurrentHashMap<>();

        // Load the config to ensure that it is created or already exists.
        final JsonObject config = getConfig();

        // Validate that the config exists and has required fields.
        if (config == null || !config.has("platformBaseUrl")
                || !config.has("appId")
                || !config.has("secret")) {
            return;
        }

        this.appId = config.get("appId").getAsInt();
        this.devIdentityId = config.get("devIdentityId").getAsBigInteger();

        if (config.has("allowVanillaItemsInTrades")) {
            this.allowVanillaItemsInTrades = config.get("allowVanillaItemsInTrades").getAsBoolean();
        }

        // If the config has debug mode set the debug flag equal to the config value.
        if (config.has("debugging")) {
            JsonObject debugging = config.getAsJsonObject("debugging");
            if (debugging.has("sdk")) {
                sdkDebug = debugging.get("sdk").getAsBoolean();
            }

            if (debugging.has("plugin")) {
                pluginDebug = debugging.get("plugin").getAsBoolean();
            }
        }

        try {
            // Initialize the Enjin Coin SDK controller with the provided Spigot plugin and the config.
            this.sdkClientController = new SdkClientController(this.main, config);
            this.sdkClientController.setUp();

            // Fetch a list of all tokens registered to the configured app ID.
            final TokensService tokensService = this.sdkClientController.getClient().getTokensService();
            tokensService.getTokensAsync(new GetTokens(), response -> {
                if (response.body() != null) {
                    List<Token> data = response.body().getData();
                    data.forEach(token -> {
                        if (config.get("appId").getAsInt() == token.getAppId() && config.get("tokens").getAsJsonObject().has(token.getTokenId())) {
                            tokens.put(token.getTokenId(), token);
                        }
                    });
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Register Listeners
        Bukkit.getPluginManager().registerEvents(this.playerManager, this.main);
        Bukkit.getPluginManager().registerEvents(this.tradeManager, this.main);
        Bukkit.getPluginManager().registerEvents(new InventoryListener(this.main), this.main);

        // Register Commands
        this.main.getCommand("enj").setExecutor(new RootCommand(this.main));
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
    public Map<String, Token> getTokens() {
        return this.tokens;
    }

    @Override
    public JsonObject getConfig() {
        File file = new File(this.main.getDataFolder(), "config.json");
        JsonElement element = null;

        try {
            // Save the default config bundled with the plugin jar to the plugin data folder.
            if (!file.exists())
                this.main.saveResource(file.getName(), false);

            JsonParser parser = new JsonParser();
            element = parser.parse(new FileReader(file));

            if (!(element instanceof JsonObject)) {
                boolean deleted = file.delete();
                if (deleted)
                    element = getConfig();
                else
                    this.main.getLogger().warning("An error occurred while load the configuration file.");
            }
        } catch (IOException ex) {
            this.main.getLogger().warning("An error occurred while creating the configuration file.");
        }

        return element.getAsJsonObject();
    }

    @Override
    public Integer getAppId() {
        return this.appId;
    }

    @Override
    public BigInteger getDevIdentityId() {
        return this.devIdentityId;
    }

    @Override
    public void debug(String log) {
        if (isPluginDebuggingEnabled())
            getLogger().info(log);
    }

    @Override
    public Logger getLogger() {
        return this.main.getLogger();
    }

    @Override
    public boolean isPluginDebuggingEnabled() {
        return this.pluginDebug;
    }

    @Override
    public boolean isSDKDebuggingEnabled() {
        return this.sdkDebug;
    }

    @Override
    public boolean isAllowVanillaItemsInTrades() {
        return allowVanillaItemsInTrades;
    }
}
