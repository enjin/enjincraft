package com.enjin.enjincraft.spigot.configuration;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.plugin.Plugin;
import com.google.gson.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TokenConf {

    public static final String FILE_NAME = "tokens.json";
    public static final Gson PRETTY_PRINT = new GsonBuilder()
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
        this.tokens.clear();
    }

    private void init(JsonObject root) {
        if (root.has(TokenConfKeys.TOKENS)) {
            JsonElement element = root.get(TokenConfKeys.TOKENS);
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

    public Map<String, TokenDefinition> getTokens() {
        return tokens;
    }

}
