package io.enjincoin.spigot_framework;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.enjincoin.sdk.client.service.identities.vo.Identity;
import io.enjincoin.sdk.client.service.notifications.NotificationsService;
import io.enjincoin.sdk.client.service.tokens.TokensService;
import io.enjincoin.sdk.client.service.tokens.vo.Token;
import io.enjincoin.spigot_framework.commands.RootCommand;
import io.enjincoin.spigot_framework.controllers.SdkClientController;
import io.enjincoin.spigot_framework.listeners.ConnectionListener;
import io.enjincoin.spigot_framework.listeners.InventoryListener;
import io.enjincoin.spigot_framework.listeners.notifications.GenericNotificationListener;
import org.bukkit.Bukkit;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class SpigotBootstrap extends PluginBootstrap {

    private final BasePlugin main;
    private boolean debug;
    private SdkClientController sdkClientController;
    private Map<UUID, Identity> identities;
    private Map<Integer, Token> tokens;

    public SpigotBootstrap(BasePlugin main) {
        this.main = main;
    }

    @Override
    public void setUp() {
        this.identities = new ConcurrentHashMap<>();
        this.tokens = new ConcurrentHashMap<>();

        // Load the config to ensure that it is created or already exists.
        final JsonObject config = getConfig();

        if (config != null && config.has("platformBaseUrl") && config.has("appId")) {
            if (config.has("debug"))
                this.debug = config.get("debug").getAsBoolean();

            this.sdkClientController = new SdkClientController(this.main, config);
            this.sdkClientController.setUp();

            final NotificationsService notificationsService = this.sdkClientController.getClient().getNotificationsService();
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.whenComplete((result, throwable) -> {
                if (throwable != null || result == null || !result) {
                    this.main.getLogger().warning("An error occurred while starting the notifications service.");
                } else {
                    this.main.getLogger().info("Registering pusher notification listener.");
                    notificationsService.addNotificationListener(new GenericNotificationListener(this.main));
                }
            });
            notificationsService.startAsync(future);

            final TokensService tokensService = this.sdkClientController.getClient().getTokensService();
            tokensService.getTokensAsync(new Callback<Token[]>() {
                @Override
                public void onResponse(Call<Token[]> call, Response<Token[]> response) {
                    if (response.isSuccessful()) {
                        Token[] tokens = response.body();
                        for (Token token : tokens) {
                            if (token.getAppId() != config.get("appId").getAsInt())
                                continue;

                            SpigotBootstrap.this.tokens.put(token.getTokenId(), token);
                        }
                    }
                }

                @Override
                public void onFailure(Call<Token[]> call, Throwable t) {
                    main.getLogger().warning("An error occurred while fetching tokens.");
                }
            });
        }

        // Register Listeners
        Bukkit.getPluginManager().registerEvents(new ConnectionListener(this.main), this.main);
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
    public Map<UUID, Identity> getIdentities() {
        return this.identities;
    }

    @Override
    public Map<Integer, Token> getTokens() {
        return this.tokens;
    }

    @Override
    public JsonObject getConfig() {
        File file = new File(this.main.getDataFolder(), "config.json");
        JsonElement element = null;

        try {
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
    public boolean isDebugEnabled() {
        return this.debug;
    }

    @Override
    public void debug(String log) {
        if (isDebugEnabled())
            this.main.getLogger().info(log);
    }

}
