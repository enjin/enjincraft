package com.enjin.ecmp.spigot_framework;

import com.enjin.ecmp.spigot_framework.commands.RootCommand;
import com.enjin.ecmp.spigot_framework.listeners.EnjinCoinEventListener;
import com.enjin.ecmp.spigot_framework.listeners.InventoryListener;
import com.enjin.ecmp.spigot_framework.player.PlayerManager;
import com.enjin.ecmp.spigot_framework.trade.TradeManager;
import com.enjin.enjincoin.sdk.TrustedPlatformClient;
import com.enjin.enjincoin.sdk.graphql.GraphQLResponse;
import com.enjin.enjincoin.sdk.http.HttpResponse;
import com.enjin.enjincoin.sdk.model.service.auth.AuthResult;
import com.enjin.enjincoin.sdk.model.service.platform.PlatformDetails;
import com.enjin.enjincoin.sdk.service.notifications.NotificationsService;
import com.enjin.enjincoin.sdk.service.notifications.PusherNotificationService;
import com.enjin.java_commons.StringUtils;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static okhttp3.logging.HttpLoggingInterceptor.Level.BODY;
import static okhttp3.logging.HttpLoggingInterceptor.Level.NONE;

public class SpigotBootstrap extends PluginBootstrap {

    private final BasePlugin plugin;
    private EcmpConfig config;

    private TrustedPlatformClient trustedPlatformClient;
    private NotificationsService notificationsService;
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
        config = new EcmpConfig(plugin);
        config.load();

        if (!validateConfig()) {
            Bukkit.getPluginManager().disablePlugin(plugin);
            return;
        }

        trustedPlatformClient = new TrustedPlatformClient.Builder()
                .httpLogLevel(config.isSdkDebugging() ? BODY : NONE)
                .baseUrl(config.getPlatformBaseUrl())
                .readTimeout(1, TimeUnit.MINUTES)
                .build();

        HttpResponse<AuthResult> authResult;

        try {
            authResult = trustedPlatformClient.authAppSync(config.getAppId(), config.getAppSecret());

            if (!authResult.isSuccess()) {
                getLogger().warning(String.format("%s: Authentication Failed", authResult.code()));
                Bukkit.getPluginManager().disablePlugin(plugin);
                return;
            }
        } catch (IOException ex) {
            getLogger().warning("Exception occurred when authenticating the trusted platform client.");
            ex.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(plugin);
            return;
        }

        HttpResponse<GraphQLResponse<PlatformDetails>> platformResponse;

        try {
            platformResponse = trustedPlatformClient.getPlatformService().getPlatformSync();

            if (!(platformResponse.isSuccess() && platformResponse.body().isSuccess())) {
                getLogger().warning(String.format("%s: Unable to fetch platform details.", authResult.code()));
                Bukkit.getPluginManager().disablePlugin(plugin);
                return;
            }

            PlatformDetails details = platformResponse.body().getData();
            notificationsService = new PusherNotificationService(details);
        } catch (IOException ex) {
            getLogger().warning("Exception occurred when fetching platform details.");
            ex.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(plugin);
            return;
        }

        try {
            notificationsService.start();
            notificationsService.registerListener(new EnjinCoinEventListener(plugin));
        } catch (Exception ex) {
            getLogger().warning("Exception occurred when starting the notification service.");
            ex.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(plugin);
            return;
        }

        // Init Managers
        playerManager = new PlayerManager(plugin);
        tradeManager = new TradeManager(plugin);

        // Register Listeners
        Bukkit.getPluginManager().registerEvents(playerManager, plugin);
        Bukkit.getPluginManager().registerEvents(tradeManager, plugin);
        Bukkit.getPluginManager().registerEvents(new InventoryListener(plugin), plugin);

        // Register Commands
        plugin.getCommand("enj").setExecutor(new RootCommand(plugin));
    }

    private boolean validateConfig() {
        boolean validUrl = !StringUtils.isEmpty(config.getPlatformBaseUrl());
        boolean validAppId = config.getAppId() >= 0;
        boolean validSecret = !StringUtils.isEmpty(config.getAppSecret());
        boolean validIdentityId = config.getDevIdentityId() >= 0;

        if (!validUrl) plugin.getLogger().warning("Invalid platform url specified in config.");
        if (!validAppId) plugin.getLogger().warning("Invalid app id specified in config.");
        if (!validSecret) plugin.getLogger().warning("Invalid app secret specified in config.");
        if (!validIdentityId) plugin.getLogger().warning("Invalid dev identity id specified in config.");

        return validUrl && validAppId && validSecret && validIdentityId;
    }

    @Override
    public void tearDown() {
        if (trustedPlatformClient != null) {
            try {
                trustedPlatformClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public TrustedPlatformClient getTrustedPlatformClient() {
        return trustedPlatformClient;
    }

    @Override
    public NotificationsService getNotificationsService() {
        return notificationsService;
    }

    @Override
    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    @Override
    public TradeManager getTradeManager() {
        return tradeManager;
    }

    @Override
    public EcmpConfig getConfig() {
        return config;
    }

    @Override
    public void debug(String log) {
        if (config.isPluginDebugging()) {
            getLogger().info(log);
        }
    }

    @Override
    public Logger getLogger() {
        return plugin.getLogger();
    }
}
