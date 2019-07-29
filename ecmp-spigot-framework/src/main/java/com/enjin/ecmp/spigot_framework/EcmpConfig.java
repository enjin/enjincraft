package com.enjin.ecmp.spigot_framework;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileReader;

import static com.enjin.ecmp.spigot_framework.ConfigKeys.*;

public class EcmpConfig {

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

    public EcmpConfig(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), FILE_NAME);
        this.appId = -1;
        this.devIdentityId = -1;
    }

    public boolean load() {
        boolean result = false;

        try {
            if (!file.exists()) plugin.saveResource(file.getName(), false);

            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(new FileReader(file));

            if (element instanceof JsonObject) {
                init(root = element.getAsJsonObject());
                result = true;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Unable to create or load config file.");
        }

        return result;
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

    public JsonObject getRoot() {
        return root;
    }
}
