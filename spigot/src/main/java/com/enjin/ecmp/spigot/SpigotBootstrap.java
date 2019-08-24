package com.enjin.ecmp.spigot;

import com.enjin.ecmp.spigot.cmd.CmdEnj;
import com.enjin.ecmp.spigot.configuration.ConfigurationException;
import com.enjin.ecmp.spigot.configuration.EnjConfig;
import com.enjin.ecmp.spigot.hooks.PlaceholderApiExpansion;
import com.enjin.ecmp.spigot.i18n.Translation;
import com.enjin.ecmp.spigot.listeners.NotificationListener;
import com.enjin.ecmp.spigot.listeners.TokenItemListener;
import com.enjin.ecmp.spigot.player.PlayerManager;
import com.enjin.ecmp.spigot.storage.Database;
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
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static okhttp3.logging.HttpLoggingInterceptor.Level.BODY;
import static okhttp3.logging.HttpLoggingInterceptor.Level.NONE;

public class SpigotBootstrap implements Bootstrap, Module {

    private final EnjPlugin plugin;
    private EnjConfig config;
    private Database database;

    private TrustedPlatformClient trustedPlatformClient;
    private PlatformDetails platformDetails;
    private NotificationsService notificationsService;
    private PlayerManager playerManager;
    private TradeManager tradeManager;

    public SpigotBootstrap(EnjPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void setUp() {
        try {
            if (!initConfig()) return;

            loadLocale();

            this.database = new Database(this);

            // Create the trusted platform client
            trustedPlatformClient = new TrustedPlatformClient.Builder()
                    .httpLogLevel(config.isSdkDebugging() ? BODY : NONE)
                    .baseUrl(config.getPlatformBaseUrl())
                    .readTimeout(1, TimeUnit.MINUTES)
                    .build();

            authenticateTPClient();
            fetchPlatformDetails();
            startNotificationService();

            // Init Managers
            playerManager = new PlayerManager(this);
            tradeManager = new TradeManager(this);

            // Register Listeners
            Bukkit.getPluginManager().registerEvents(playerManager, plugin);
            Bukkit.getPluginManager().registerEvents(tradeManager, plugin);
            Bukkit.getPluginManager().registerEvents(new TokenItemListener(this), plugin);

            // Register Commands
            PluginCommand pluginCommand = plugin.getCommand("enj");
            CmdEnj cmdEnj = new CmdEnj(this);
            pluginCommand.setExecutor(cmdEnj);

            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
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
        } catch (Exception ex) {
            ex.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(plugin);
        }
    }

    private boolean initConfig() {
        // Init and load configuration file
        config = new EnjConfig(plugin);
        config.load();

        // Validate that the required config values are valid
        if (!validateConfig()) {
            Bukkit.getPluginManager().disablePlugin(plugin);
            return false;
        }

        return true;
    }

    private void authenticateTPClient() throws AuthenticationException {
        try {
            // Attempt to authenticate the client using an app secret
            HttpResponse<AuthResult> networkResponse = trustedPlatformClient.authAppSync(
                    config.getAppId(),
                    config.getAppSecret()
            );

            // Could not authenticate the client
            if (!networkResponse.isSuccess()) {
                throw new AuthenticationException(networkResponse.code());
            }
        } catch (IOException ex) {
            throw new AuthenticationException(ex);
        }
    }

    private void fetchPlatformDetails() throws NetworkException, GraphQLException {
        try {
            // Fetch the platform details
            HttpResponse<GraphQLResponse<PlatformDetails>> networkResponse = trustedPlatformClient.getPlatformService()
                    .getPlatformSync();

            if (!networkResponse.isSuccess()) {
                throw new NetworkException(networkResponse.code());
            }

            GraphQLResponse<PlatformDetails> graphQLResponse = networkResponse.body();

            if (!graphQLResponse.isSuccess()) {
                throw new GraphQLException(graphQLResponse.getErrors());
            }

            platformDetails = graphQLResponse.getData();
        } catch (IOException ex) {
            throw new NetworkException(ex);
        }
    }

    private void startNotificationService() {
        try {
            // Start the notification service and register a listener
            notificationsService = new PusherNotificationService(platformDetails);
            notificationsService.start();
            notificationsService.registerListener(new NotificationListener(this));
            notificationsService.subscribeToApp(config.getAppId());
        } catch (Exception ex) {
            throw new NotificationServiceException(ex);
        }
    }

    @Override
    public void tearDown() {
        try {
            if (trustedPlatformClient != null) trustedPlatformClient.close();
            if (notificationsService != null) notificationsService.shutdown();
        } catch (Exception ex) {
            ex.printStackTrace();
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

    public TradeManager getTradeManager() {
        return tradeManager;
    }

    @Override
    public EnjConfig getConfig() {
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

    public void loadLocale() throws ConfigurationException {
        YamlConfiguration lang = loadLocaleResource(config.getLocale());

        if (lang == null)
            lang = loadLocaleResource(Translation.DEFAULT_LOCALE);

        if (lang == null)
            throw new ConfigurationException("Could not load default (en_US) translation.");

        Translation.setLang(lang);
    }

    public YamlConfiguration loadLocaleResource(String locale) {
        InputStream is = plugin.getResource(String.format("lang/%s.yml", locale));

        if (is == null)
            return null;

        return YamlConfiguration.loadConfiguration(new InputStreamReader(is, Charset.forName("UTF-8")));
    }

    public void debug(String log) {
        if (config.isPluginDebugging()) {
            getLogger().info(log);
        }
    }

    public Logger getLogger() {
        return plugin.getLogger();
    }

    public Database db() {
        return database;
    }
}
