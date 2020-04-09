package com.enjin.enjincraft.spigot.configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TokenManager {

    public static final String JSON_EXT = ".json";
    public static final int JSON_EXT_LENGTH = JSON_EXT.length();

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private File dir;
    private Map<String, TokenModel> tokenModels = new HashMap<>();

    public TokenManager(File dir) {
        this.dir = new File(dir, "tokens");
    }

    public void loadTokens() {
        if (!dir.exists())
            dir.mkdirs();

        tokenModels.clear();

        for (File file : dir.listFiles()) {
            // Ignores directories and non-JSON files
            if (file.isDirectory() || !file.getName().endsWith(".json"))
                continue;

            try (FileReader fr = new FileReader(file)) {
                String fileName = file.getName();
                String tokenId = fileName.substring(0, fileName.length() - JSON_EXT_LENGTH);
                TokenModel tokenModel = gson.fromJson(fr, TokenModel.class);
                tokenModel.load();
                tokenModels.put(tokenId, tokenModel);
            } catch (Exception e) {}
        }
    }

    public void saveToken(String tokenId, TokenModel tokenModel) {
        if (!dir.exists())
            dir.mkdirs();

        File file = new File(dir, String.format("%s%s", tokenId, JSON_EXT));

        try (FileWriter fw = new FileWriter(file, false)) {
            gson.toJson(tokenModel, fw);
            tokenModel.load();
            tokenModels.put(tokenId, tokenModel);
        } catch (Exception e) {}
    }

    public boolean hasToken(String tokenId) {
        return tokenModels.containsKey(tokenId);
    }

    public TokenModel getToken(String tokenId) {
        return tokenModels.get(tokenId);
    }

    public Set<String> getTokenIds() {
        return tokenModels.keySet();
    }

    public Collection<TokenModel> getTokens() {
        return tokenModels.values();
    }

    public Set<Map.Entry<String, TokenModel>> getEntries() {
        return tokenModels.entrySet();
    }

}
