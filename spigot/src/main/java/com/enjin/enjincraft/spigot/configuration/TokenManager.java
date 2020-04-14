package com.enjin.enjincraft.spigot.configuration;

import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.player.EnjPlayer;
import com.enjin.enjincraft.spigot.player.PlayerManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

public class TokenManager {

    public static final String JSON_EXT = ".json";
    public static final int JSON_EXT_LENGTH = JSON_EXT.length();

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private SpigotBootstrap bootstrap;
    private File dir;
    private Map<String, TokenModel> tokenModels = new HashMap<>();
    private TokenPermissionGraph permGraph = new TokenPermissionGraph();

    public TokenManager(SpigotBootstrap bootstrap, File dir) {
        this.bootstrap = bootstrap;
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
                permGraph.addToken(tokenModel);
            } catch (Exception e) {
                bootstrap.log(e);
            }
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
            permGraph.addToken(tokenModel);
        } catch (Exception e) {
            bootstrap.log(e);
        }
    }

    public void updateTokenConf(String tokenId, TokenModel tokenModel) {
        if (!dir.exists()) {
            saveToken(tokenId, tokenModel);
            return;
        }

        File file = new File(dir, String.format("%s%s", tokenId, JSON_EXT));

        try (FileWriter fw = new FileWriter(file, false)) {
            gson.toJson(tokenModel, fw);
        } catch (Exception e) {
            bootstrap.log(e);
        }
    }

    public void addPermissionToToken(String perm, String tokenId) {
        TokenModel tokenModel = tokenModels.get(tokenId);

        if (tokenModel == null)
            return;

        // Checks if the permission was not added
        if (!tokenModel.addPermission(perm))
            return;

        permGraph.addTokenPerm(perm, tokenId);
        updateTokenConf(tokenId, tokenModel);

        PlayerManager playerManager = bootstrap.getPlayerManager();
        for (UUID uuid : playerManager.getPlayers().keySet()) {
            Optional<EnjPlayer> player = playerManager.getPlayer(uuid);

            if (player.isPresent())
                player.get().addPermission(perm, tokenId);
        }
    }

    public void removePermissionFromToken(String perm, String tokenId) {
        TokenModel tokenModel = tokenModels.get(tokenId);

        if (tokenModel == null)
            return;

        // Checks if the permission was not removed
        if (!tokenModel.removePermission(perm))
            return;

        permGraph.removeTokenPerm(perm, tokenId);
        updateTokenConf(tokenId, tokenModel);

        PlayerManager playerManager = bootstrap.getPlayerManager();
        for (UUID uuid : playerManager.getPlayers().keySet()) {
            Optional<EnjPlayer> player = playerManager.getPlayer(uuid);

            if (player.isPresent())
                player.get().removePermission(perm);
        }
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

    public TokenPermissionGraph getTokenPermissions() {
        return permGraph;
    }

}
