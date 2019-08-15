package com.enjin.ecmp.spigot.configuration;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import static com.enjin.ecmp.spigot.configuration.ConfigKeys.*;

public class EnjConfig {

    public static final String FILE_NAME = "config.json";

    private Plugin plugin;
    private File file;
    private JsonObject root;

    private String platformBaseUrl;
    private int appId;
    private String appSecret;
    private int devIdentityId;
    private boolean allowVanillaItemsInTrades;
    private boolean sdkDebugging;
    private boolean pluginDebugging;
    private Map<String, TokenDefinition> tokens;

    public EnjConfig(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), FILE_NAME);
        this.appId = -1;
        this.devIdentityId = -1;
        this.tokens = new HashMap<>();
    }

    public boolean load() {
        boolean result = false;

        clean();

        try {
            if (!file.exists()) plugin.saveResource(file.getName(), false);

            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(new FileReader(file));

            if (element instanceof JsonObject) {
                init(root = element.getAsJsonObject());
                result = true;
            }
        } catch (Exception ex) {
            throw new ConfigurationException(ex);
        }

        return result;
    }

    private void clean() {
        this.platformBaseUrl = null;
        this.appId = -1;
        this.appSecret = null;
        this.devIdentityId = -1;
        this.tokens.clear();
        this.allowVanillaItemsInTrades = false;
        this.sdkDebugging = false;
        this.pluginDebugging = false;
    }

    private void init(JsonObject root) {
        if (root.has(BASE_URL)) platformBaseUrl = root.get(BASE_URL).getAsString();
        if (root.has(APP_ID)) appId = root.get(APP_ID).getAsInt();
        if (root.has(APP_SECRET)) appSecret = root.get(APP_SECRET).getAsString();
        if (root.has(DEV_IDENT_ID)) devIdentityId = root.get(DEV_IDENT_ID).getAsInt();
        if (root.has(VANILLA_ITEMS_TRADING)) allowVanillaItemsInTrades = root.get(VANILLA_ITEMS_TRADING).getAsBoolean();
        if (root.has(DEBUGGING)) {
            JsonElement element = root.get(DEBUGGING);
            if (element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                if (object.has(SDK_DEBUG)) this.sdkDebugging = object.get(SDK_DEBUG).getAsBoolean();
                if (object.has(PLUGIN_DEBUG)) this.pluginDebugging = object.get(PLUGIN_DEBUG).getAsBoolean();
            }
        }
        if (root.has(TOKENS)) {
            JsonElement element = root.get(TOKENS);
            if (element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                    String id = entry.getKey();
                    JsonElement tokenDefElem = entry.getValue();
                    if (tokenDefElem.isJsonObject()) {
                        try {
                            this.tokens.put(id, new TokenDefinition(id, tokenDefElem.getAsJsonObject()));
                        } catch (Exception ex) {
                            throw new ConfigurationException(String.format("Invalid token definition: %s", id), ex);
                        }
                    }
                }
            }
        }
    }

    public String getPlatformBaseUrl() {
        return platformBaseUrl;
    }

    public int getAppId() {
        return appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public int getDevIdentityId() {
        return devIdentityId;
    }

    public boolean isAllowVanillaItemsInTrades() {
        return allowVanillaItemsInTrades;
    }

    public boolean isSdkDebugging() {
        return sdkDebugging;
    }

    public boolean isPluginDebugging() {
        return pluginDebugging;
    }

    public Map<String, TokenDefinition> getTokens() {
        return tokens;
    }

    public JsonObject getRoot() {
        return root;
    }
}
