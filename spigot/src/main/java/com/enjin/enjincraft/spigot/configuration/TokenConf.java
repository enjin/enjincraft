package com.enjin.enjincraft.spigot.configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class TokenConf {

    public static final String FILE_NAME    = "tokens.json";
    public static final Gson   PRETTY_PRINT = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private Plugin plugin;
    private File file;
    private JsonObject root;

    private Map<String, TokenDefinition> tokens;

    public TokenConf(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), FILE_NAME);
        this.tokens = new HashMap<>();
    }

    public void load() {
        clean();

        try {
            if (!file.exists())
                plugin.saveResource(file.getName(), false);

            JsonElement element = PRETTY_PRINT.fromJson(new FileReader(file), JsonElement.class);

            if (element instanceof JsonObject) {
                root = element.getAsJsonObject();
                init(root);
            }
        } catch (Exception ex) {
            throw new ConfigurationException(ex);
        }
    }

    public void save() {
        try {
            if (file.getParentFile() != null)
                file.getParentFile().mkdirs();

            if (!file.exists() && !file.createNewFile()) {
                throw new IllegalStateException("File state changed before create operation");
            }

            try (Writer fw = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.ISO_8859_1)) {
                fw.write(PRETTY_PRINT.toJson(root));
            }
        } catch (IOException ex) {
            throw new ConfigurationException("Unable to save config.", ex);
        }
    }

    private void clean() {
        this.tokens.clear();
    }

    private void init(JsonObject root) {
        if (!root.has(TokenConfKeys.TOKENS))
            return;

        JsonElement element = root.get(TokenConfKeys.TOKENS);
        if (!element.isJsonObject())
            return;

        JsonObject object = element.getAsJsonObject();

        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String id = entry.getKey();
            JsonElement tokenDefElem = entry.getValue();

            if (!tokenDefElem.isJsonObject())
                continue;

            try {
                this.tokens.put(id, new TokenDefinition(id, tokenDefElem.getAsJsonObject()));
            } catch (Exception ex) {
                throw new ConfigurationException(String.format("Invalid token definition: %s", id), ex);
            }
        }
    }

    public Map<String, TokenDefinition> getTokens() {
        return tokens;
    }

    public JsonObject getTokensRaw() {
        return root.getAsJsonObject("tokens");
    }

}
