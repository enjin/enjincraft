package com.enjin.ecmp.spigot;

import com.enjin.ecmp.spigot.cmd.CmdEnj;
import com.enjin.ecmp.spigot.configuration.EcmpConfig;
import com.enjin.ecmp.spigot.hooks.PlaceholderApiExpansion;
import com.enjin.ecmp.spigot.listeners.NotificationListener;
import com.enjin.ecmp.spigot.listeners.TokenItemListener;
import com.enjin.ecmp.spigot.player.PlayerManager;
import com.enjin.ecmp.spigot.trade.TradeManager;
import com.enjin.ecmp.spigot.util.MessageUtils;
import com.enjin.enjincoin.sdk.TrustedPlatformClient;
import com.enjin.enjincoin.sdk.graphql.GraphQLResponse;
import com.enjin.enjincoin.sdk.http.HttpResponse;
import com.enjin.enjincoin.sdk.model.service.auth.AuthResult;
import com.enjin.enjincoin.sdk.model.service.platform.PlatformDetails;
import com.enjin.enjincoin.sdk.service.notifications.NotificationsService;
import com.enjin.enjincoin.sdk.service.notifications.PusherNotificationService;
import com.enjin.java_commons.StringUtils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static okhttp3.logging.HttpLoggingInterceptor.Level.BODY;
import static okhttp3.logging.HttpLoggingInterceptor.Level.NONE;

public class SpigotBootstrap implements Bootstrap, Module {

    private final EcmpPlugin plugin;
    private EcmpConfig config;

    private TrustedPlatformClient trustedPlatformClient;
    private NotificationsService notificationsService;
    private PlayerManager playerManager;
    private TradeManager tradeManager;

    public SpigotBootstrap(EcmpPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void setUp() {
        // Init and load configuration file
        config = new EcmpConfig(plugin);
        config.load();

        // Validate that the required config values are valid
        if (!validateConfig()) {
            Bukkit.getPluginManager().disablePlugin(plugin);
            return;
        }

        // Create the trusted platform client
        trustedPlatformClient = new TrustedPlatformClient.Builder()
                .httpLogLevel(config.isSdkDebugging() ? BODY : NONE)
                .baseUrl(config.getPlatformBaseUrl())
                .readTimeout(1, TimeUnit.MINUTES)
                .build();

        HttpResponse<AuthResult> authResult;

        try {
            // Attempt to authenticate the client using an app secret
            authResult = trustedPlatformClient.authAppSync(config.getAppId(), config.getAppSecret());

            // Could not authenticate the client
            if (!authResult.isSuccess()) {
                getLogger().warning(String.format("%s: Authentication Failed", authResult.code()));
                Bukkit.getPluginManager().disablePlugin(plugin);
                return;
            }
        } catch (IOException ex) {
            // An exception was caught while attempting authenticating the client
            getLogger().warning("Exception occurred when authenticating the trusted platform client.");
            ex.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(plugin);
            return;
        }

        HttpResponse<GraphQLResponse<PlatformDetails>> platformResponse;

        try {
            // Fetch the platform details
            platformResponse = trustedPlatformClient.getPlatformService().getPlatformSync();

            // Could not fetch the platform details
            if (!(platformResponse.isSuccess() && platformResponse.body().isSuccess())) {
                getLogger().warning(String.format("%s: Unable to fetch platform details.", authResult.code()));
                Bukkit.getPluginManager().disablePlugin(plugin);
                return;
            }

            // Init the notification service with the fetched platform details
            PlatformDetails details = platformResponse.body().getData();
            notificationsService = new PusherNotificationService(details);
        } catch (IOException ex) {
            // An exception was caught while fetching the platform details
            getLogger().warning("Exception occurred when fetching platform details.");
            ex.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(plugin);
            return;
        }

        try {
            // Start the notification service and register a listener
            notificationsService.start();
            notificationsService.registerListener(new NotificationListener(this));
            notificationsService.subscribeToApp(config.getAppId());
        } catch (Exception ex) {
            // An exception occurred while starting the notification service
            getLogger().warning("Exception occurred when starting the notification service.");
            ex.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(plugin);
            return;
        }

        // Init Managers
        playerManager = new PlayerManager(this);
        tradeManager = new TradeManager(this);

        // Register Listeners
        Bukkit.getPluginManager().registerEvents(playerManager, plugin);
        Bukkit.getPluginManager().registerEvents(tradeManager, plugin);
        Bukkit.getPluginManager().registerEvents(new TokenItemListener(this), plugin);

        // Register Commands
        plugin.getCommand("enj").setExecutor(new CmdEnj(this));

        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null){
            MessageUtils.sendComponent(Bukkit.getConsoleSender(), TextComponent.of("[ECMP] Registering PlaceholderAPI Expansion")
                    .color(TextColor.GOLD));
            PlaceholderExpansion expansion = new PlaceholderApiExpansion(this);
            boolean registered = expansion.register();
            if (registered) {
                MessageUtils.sendComponent(Bukkit.getConsoleSender(), TextComponent.of("[ECMP] Registered PlaceholderAPI Expansion")
                        .color(TextColor.GREEN));
            } else {
                MessageUtils.sendComponent(Bukkit.getConsoleSender(), TextComponent.of("[ECMP] Could not register PlaceholderAPI Expansion")
                        .color(TextColor.RED));
            }
        }
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

    public Plugin plugin() {
        return plugin;
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

    public void debug(String log) {
        if (config.isPluginDebugging()) {
            getLogger().info(log);
        }
    }

    public Logger getLogger() {
        return plugin.getLogger();
    }
}
