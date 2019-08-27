package com.enjin.ecmp.spigot.configuration;

import com.enjin.ecmp.spigot.i18n.Translation;
import com.google.gson.*;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.enjin.ecmp.spigot.configuration.ConfigKeys.*;

public class EnjConfig {

    public static final String FILE_NAME = "config.json";
    public static final Gson PRETTY_PRINT = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private Plugin plugin;
    private File file;
    private JsonObject root;

    private String platformBaseUrl;
    private int appId;
    private String appSecret;
    private int devIdentityId;
    private boolean sdkDebugging;
    private boolean pluginDebugging;
    private Map<String, TokenDefinition> tokens;
    private String locale;
    private String sentry;

    public EnjConfig(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), FILE_NAME);
        this.appId = -1;
        this.devIdentityId = -1;
        this.tokens = new HashMap<>();
        this.locale = Translation.DEFAULT_LOCALE;
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

    public void save() {
        try {
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }

            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter fw = new FileWriter(file);
            fw.write(PRETTY_PRINT.toJson(root));
            fw.close();
        } catch (IOException ex) {
            throw new ConfigurationException("Unable to save config.", ex);
        }
    }

    private void clean() {
        this.platformBaseUrl = null;
        this.appId = -1;
        this.appSecret = null;
        this.devIdentityId = -1;
        this.tokens.clear();
        this.sdkDebugging = false;
        this.pluginDebugging = false;
        this.locale = "en_US";
    }

    private void init(JsonObject root) {
        if (root.has(BASE_URL))
            platformBaseUrl = root.get(BASE_URL).getAsString();
        if (root.has(APP_ID))
            appId = root.get(APP_ID).getAsInt();
        if (root.has(APP_SECRET))
            appSecret = root.get(APP_SECRET).getAsString();
        if (root.has(DEV_IDENT_ID))
            devIdentityId = root.get(DEV_IDENT_ID).getAsInt();
        if (root.has(DEBUGGING)) {
            JsonElement element = root.get(DEBUGGING);
            if (element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                if (object.has(SDK_DEBUG))
                    this.sdkDebugging = object.get(SDK_DEBUG).getAsBoolean();
                if (object.has(PLUGIN_DEBUG))
                    this.pluginDebugging = object.get(PLUGIN_DEBUG).getAsBoolean();
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
        if (root.has(LOCALE))
            this.locale = root.get(LOCALE).getAsString();
        if (root.has(SENTRY))
            this.sentry = root.get(SENTRY).getAsString();
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

    public boolean isSdkDebugging() {
        return sdkDebugging;
    }

    public boolean isPluginDebugging() {
        return pluginDebugging;
    }

    public Map<String, TokenDefinition> getTokens() {
        return tokens;
    }

    public String getLocale() {
        return locale;
    }

    public String getSentry() {
        return sentry;
    }

    public void setLocale(String locale) {
        this.locale = locale;
        this.root.addProperty(LOCALE, locale);
    }

    public JsonObject getRoot() {
        return root;
    }
}
