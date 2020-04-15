package com.enjin.enjincraft.spigot.token;

import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.player.EnjPlayer;
import com.enjin.enjincraft.spigot.player.PlayerManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.World;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class TokenManager {

    // Status codes
    public static final int PERM_ADDED_SUCCESS = 210;         // Permission was added
    public static final int PERM_ADDED_DUPLICATEPERM = 410;   // Permission is a duplicate
    public static final int PERM_ADDED_BLACKLISTED = 411;     // Permission is blacklisted
    public static final int PERM_REMOVED_SUCCESS = 250;       // Permission was removed
    public static final int PERM_REMOVED_NOPERMONTOKEN = 450; // Permission is not assigned
    public static final int PERM_NOSUCHTOKEN = 400;           // The token does not exist

    public static final String JSON_EXT = ".json";
    public static final int JSON_EXT_LENGTH = JSON_EXT.length();
    public static final String GLOBAL = "*";

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

            String tokenId = null;
            TokenModel tokenModel = null;
            boolean changed = false;
            try (FileReader fr = new FileReader(file)) {
                String fileName = file.getName();
                tokenId = fileName.substring(0, fileName.length() - JSON_EXT_LENGTH);
                tokenModel = gson.fromJson(fr, TokenModel.class);
                tokenModel.load();
                changed = tokenModel.applyBlacklist(bootstrap.getConfig().getPermissionBlacklist());
                tokenModels.put(tokenId, tokenModel);
                permGraph.addToken(tokenModel);
            } catch (Exception e) {
                bootstrap.log(e);
            } finally {
                if (changed)
                    saveToken(tokenId, tokenModel);
            }
        }

        LegacyTokenConverter legacyConverter = new LegacyTokenConverter(bootstrap);
        if (legacyConverter.fileExists())
            legacyConverter.process();
    }

    public void saveToken(String tokenId, TokenModel tokenModel) {
        if (!dir.exists())
            dir.mkdirs();

        File file = new File(dir, String.format("%s%s", tokenId, JSON_EXT));

        tokenModel.applyBlacklist(bootstrap.getConfig().getPermissionBlacklist());

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

        tokenModel.applyBlacklist(bootstrap.getConfig().getPermissionBlacklist());

        try (FileWriter fw = new FileWriter(file, false)) {
            gson.toJson(tokenModel, fw);
        } catch (Exception e) {
            bootstrap.log(e);
        }
    }

    public int addPermissionToToken(String perm, String tokenId, String world) {
        if (bootstrap.getConfig().getPermissionBlacklist().contains(perm))
            return PERM_ADDED_BLACKLISTED;

        TokenModel tokenModel = tokenModels.get(tokenId);

        if (tokenModel == null)
            return PERM_NOSUCHTOKEN;

        // Checks if the permission was not added
        if (!tokenModel.addPermission(perm, world))
            return PERM_ADDED_DUPLICATEPERM;

        permGraph.addTokenPerm(perm, tokenId, world);
        updateTokenConf(tokenId, tokenModel);

        PlayerManager playerManager = bootstrap.getPlayerManager();
        for (UUID uuid : playerManager.getPlayers().keySet()) {
            Optional<EnjPlayer> player = playerManager.getPlayer(uuid);

            player.ifPresent(enjPlayer -> enjPlayer.addPermission(perm, tokenId, world));
        }

        return PERM_ADDED_SUCCESS;
    }

    public int addPermissionToToken(String perm, String tokenId, Collection<String> worlds) {
        if (bootstrap.getConfig().getPermissionBlacklist().contains(perm))
            return PERM_ADDED_BLACKLISTED;

        TokenModel tokenModel = tokenModels.get(tokenId);

        if (tokenModel == null)
            return PERM_NOSUCHTOKEN;

        worlds.forEach(world -> addPermissionToToken(perm, tokenId, world));

        return PERM_ADDED_SUCCESS;
    }

    public int removePermissionFromToken(String perm, String tokenId, String world) {
        TokenModel tokenModel = tokenModels.get(tokenId);

        if (tokenModel == null)
            return PERM_NOSUCHTOKEN;

        // Checks if the permission was not removed
        if (!tokenModel.removePermission(perm, world))
            return PERM_REMOVED_NOPERMONTOKEN;

        permGraph.removeTokenPerm(perm, tokenId, world);
        updateTokenConf(tokenId, tokenModel);

        PlayerManager playerManager = bootstrap.getPlayerManager();
        for (UUID uuid : playerManager.getPlayers().keySet()) {
            Optional<EnjPlayer> player = playerManager.getPlayer(uuid);

            player.ifPresent(enjPlayer -> enjPlayer.removePermission(perm, world));
        }

        return PERM_REMOVED_SUCCESS;
    }

    public int removePermissionFromToken(String perm, String tokenId, Collection<String> worlds) {
        TokenModel tokenModel = tokenModels.get(tokenId);

        if (tokenModel == null)
            return PERM_NOSUCHTOKEN;

        worlds.forEach(world -> removePermissionFromToken(perm, tokenId, world));

        return PERM_REMOVED_SUCCESS;
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
