package com.enjin.enjincraft.spigot.token;

import com.enjin.enjincraft.spigot.GraphQLException;
import com.enjin.enjincraft.spigot.NetworkException;
import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.player.EnjPlayer;
import com.enjin.enjincraft.spigot.player.PlayerManager;
import com.enjin.enjincraft.spigot.util.StringUtils;
import com.enjin.sdk.TrustedPlatformClient;
import com.enjin.sdk.graphql.GraphQLResponse;
import com.enjin.sdk.models.token.GetToken;
import com.enjin.sdk.models.token.Token;
import com.enjin.sdk.services.notification.NotificationsService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TokenManager {

    // Status codes
    public static final int TOKEN_CREATE_SUCCESS = 200;       // Token was created
    public static final int TOKEN_UPDATE_SUCCESS = 201;       // Token was updated
    public static final int TOKEN_CREATE_FAILED = 400;        // Token was not created
    public static final int TOKEN_UPDATE_FAILED = 401;        // Token was not updated
    public static final int TOKEN_NOSUCHTOKEN = 402;          // The token does not exist
    public static final int TOKEN_DUPLICATENICKNAME = 403;    // Token alternate id already exists
    public static final int TOKEN_HASNICKNAME = 404;          // Token has alternate
    public static final int PERM_ADDED_SUCCESS = 210;         // Permission was added
    public static final int PERM_ADDED_DUPLICATEPERM = 410;   // Permission is a duplicate
    public static final int PERM_ADDED_BLACKLISTED = 411;     // Permission is blacklisted
    public static final int PERM_REMOVED_SUCCESS = 250;       // Permission was removed
    public static final int PERM_REMOVED_NOPERMONTOKEN = 450; // Permission is not assigned

    public static final Charset CHARSET = StandardCharsets.UTF_8;
    public static final String JSON_EXT = ".json";
    public static final int JSON_EXT_LENGTH = JSON_EXT.length();
    public static final String GLOBAL = "*";

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(TokenPermission.class, new TokenPermission.TokenPermissionSerializer())
            .registerTypeAdapter(TokenPermission.class, new TokenPermission.TokenPermissionDeserializer())
            .setPrettyPrinting()
            .create();

    private SpigotBootstrap bootstrap;
    private File dir;
    private Map<String, TokenModel> tokenModels = new HashMap<>();
    private Map<String, String> alternateIds = new HashMap<>();
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

            TokenModel tokenModel = null;
            boolean changed = false;
            try (InputStreamReader in = new InputStreamReader(new FileInputStream(file), CHARSET)) {
                tokenModel = gson.fromJson(in, TokenModel.class);
                tokenModel.load();
                changed = tokenModel.applyBlacklist(bootstrap.getConfig().getPermissionBlacklist());
                cacheAndSubscribe(tokenModel);
            } catch (Exception e) {
                bootstrap.log(e);
            } finally {
                if (tokenModel != null && changed)
                    saveToken(tokenModel);
            }
        }

        LegacyTokenConverter legacyConverter = new LegacyTokenConverter(bootstrap);
        if (legacyConverter.fileExists())
            legacyConverter.process();
    }

    public int saveToken(TokenModel tokenModel) {
        // Prevents tokens with the same alternate id from existing
        String alternateId = tokenModel.getAlternateId();
        String otherToken = alternateIds.get(alternateId);
        if (alternateId != null && otherToken != null && !otherToken.equals(tokenModel.getId()))
            return TOKEN_DUPLICATENICKNAME;

        if (!dir.exists())
            dir.mkdirs();

        File file = new File(dir, String.format("%s%s", tokenModel.getId(), JSON_EXT));

        tokenModel.applyBlacklist(bootstrap.getConfig().getPermissionBlacklist());

        try (OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file, false), CHARSET)) {
            gson.toJson(tokenModel, out);
            tokenModel.load();
            cacheAndSubscribe(tokenModel);

            if (tokenModel.getAlternateId() != null)
                alternateIds.put(tokenModel.getAlternateId(), tokenModel.getId());
        } catch (Exception e) {
            bootstrap.log(e);
            return TOKEN_CREATE_FAILED;
        }

        return TOKEN_CREATE_SUCCESS;
    }

    private void cacheAndSubscribe(TokenModel tokenModel) {
        tokenModels.put(tokenModel.getId(), tokenModel);
        permGraph.addToken(tokenModel);
        subscribeToToken(tokenModel);
    }

    public int updateTokenConf(TokenModel tokenModel) {
        TokenModel oldModel = tokenModels.get(tokenModel.getId());

        if (!dir.exists() || oldModel == null)
            return saveToken(tokenModel);

        File file = new File(dir, String.format("%s%s", tokenModel.getId(), JSON_EXT));

        tokenModel.applyBlacklist(bootstrap.getConfig().getPermissionBlacklist());

        boolean newNbt = !tokenModel.getNbt().equals(oldModel.getNbt());

        try (OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file, false), CHARSET)) {
            gson.toJson(tokenModel, out);
            tokenModel.load();
            tokenModels.put(tokenModel.getId(), tokenModel);
        } catch (Exception e) {
            bootstrap.log(e);
            return TOKEN_UPDATE_FAILED;
        }

        if (newNbt)
            updateTokenOnPlayers(tokenModel.getId());

        return TOKEN_UPDATE_SUCCESS;
    }

    private void updateTokenOnPlayers(String tokenId) {
        PlayerManager playerManager = bootstrap.getPlayerManager();

        if (playerManager == null)
            return;

        for (UUID uuid : playerManager.getPlayers().keySet()) {
            Optional<EnjPlayer> player = playerManager.getPlayer(uuid);

            player.ifPresent(enjPlayer -> enjPlayer.updateToken(tokenId));
        }
    }

    public int updateAlternateId(String tokenId, String alternateId) {
        if (!tokenModels.containsKey(tokenId))
            return TOKEN_NOSUCHTOKEN;

        TokenModel tokenModel = tokenModels.get(tokenId);

        if (tokenModel.getAlternateId() != null && alternateId.equals(tokenModel.getAlternateId()))
            return TOKEN_HASNICKNAME;
        else if (alternateIds.containsKey(alternateId))
            return TOKEN_DUPLICATENICKNAME;

        if (tokenModel.getAlternateId() != null)
            alternateIds.remove(tokenModel.getAlternateId());

        tokenModel.setAlternateId(alternateId);
        alternateIds.put(alternateId, tokenId);

        return updateTokenConf(tokenModel);
    }

    public int updateMetadataURI(String id, String metadataURI) {
        TokenModel tokenModel = getToken(id);
        if (tokenModel == null)
            return TOKEN_NOSUCHTOKEN;

        if (StringUtils.isEmpty(metadataURI))
            metadataURI = null;

        tokenModel.setMetadataURI(metadataURI);
        return updateTokenConf(tokenModel);
    }

    public void updateMetadataURI(String id) throws GraphQLException, NetworkException {
        TokenModel tokenModel = getToken(id);
        if (tokenModel == null)
            return;

        TrustedPlatformClient client = bootstrap.getTrustedPlatformClient();
        client.getTokenService()
                .getTokenAsync(new GetToken()
                                .tokenId(tokenModel.getId())
                                .withItemUri(),
                        networkResponse -> {
                            if (!networkResponse.isSuccess())
                                throw new NetworkException(networkResponse.code());

                            GraphQLResponse<Token> graphQLResponse = networkResponse.body();
                            if (!graphQLResponse.isSuccess())
                                throw new GraphQLException(graphQLResponse.getErrors());

                            String metadataURI = graphQLResponse.getData().getItemURI();
                            updateMetadataURI(tokenModel.getId(), metadataURI);
                });
    }

    public int addPermissionToToken(String perm, String id, String world) {
        if (bootstrap.getConfig().getPermissionBlacklist().contains(perm))
            return PERM_ADDED_BLACKLISTED;

        TokenModel tokenModel = getToken(id);

        if (tokenModel == null)
            return TOKEN_NOSUCHTOKEN;

        // Checks if the permission was not added
        if (!tokenModel.addPermissionToWorld(perm, world))
            return PERM_ADDED_DUPLICATEPERM;

        permGraph.addTokenPerm(perm, tokenModel.getId(), world);

        int status = updateTokenConf(tokenModel);
        addPermissionToPlayers(perm, tokenModel.getId(), world);

        if (status != TOKEN_UPDATE_SUCCESS)
            return status;

        return PERM_ADDED_SUCCESS;
    }

    public int addPermissionToToken(String perm, String id, Collection<String> worlds) {
        if (bootstrap.getConfig().getPermissionBlacklist().contains(perm))
            return PERM_ADDED_BLACKLISTED;

        TokenModel tokenModel = getToken(id);

        if (tokenModel == null)
            return TOKEN_NOSUCHTOKEN;

        // Checks if the permission was not added
        if (!tokenModel.addPermissionToWorlds(perm, worlds))
            return PERM_ADDED_DUPLICATEPERM;

        permGraph.addTokenPerm(perm, tokenModel.getId(), worlds);

        int status = updateTokenConf(tokenModel);
        worlds.forEach(world -> addPermissionToPlayers(perm, tokenModel.getId(), world));

        if (status != TOKEN_UPDATE_SUCCESS)
            return status;

        return PERM_ADDED_SUCCESS;
    }

    private void addPermissionToPlayers(String perm, String tokenId, String world) {
        PlayerManager playerManager = bootstrap.getPlayerManager();
        for (UUID uuid : playerManager.getPlayers().keySet()) {
            Optional<EnjPlayer> player = playerManager.getPlayer(uuid);

            player.ifPresent(enjPlayer -> enjPlayer.addPermission(perm, tokenId, world));
        }
    }

    public int removePermissionFromToken(String perm, String id, String world) {
        TokenModel tokenModel = getToken(id);

        if (tokenModel == null)
            return TOKEN_NOSUCHTOKEN;

        // Checks if the permission was not removed
        if (!tokenModel.removePermissionFromWorld(perm, world))
            return PERM_REMOVED_NOPERMONTOKEN;

        permGraph.removeTokenPerm(perm, tokenModel.getId(), world);

        int status = updateTokenConf(tokenModel);
        removePermissionFromPlayers(perm, world);

        if (status != TOKEN_UPDATE_SUCCESS)
            return status;

        return PERM_REMOVED_SUCCESS;
    }

    public int removePermissionFromToken(String perm, String id, Collection<String> worlds) {
        TokenModel tokenModel = getToken(id);

        if (tokenModel == null)
            return TOKEN_NOSUCHTOKEN;

        // Checks if the permission was not removed
        if (!tokenModel.removePermissionFromWorlds(perm, worlds))
            return PERM_REMOVED_NOPERMONTOKEN;

        permGraph.removeTokenPerm(perm, tokenModel.getId(), worlds);

        int status = updateTokenConf(tokenModel);
        worlds.forEach(world -> removePermissionFromPlayers(perm, world));

        if (status != TOKEN_UPDATE_SUCCESS)
            return status;

        return PERM_REMOVED_SUCCESS;
    }

    private void removePermissionFromPlayers(String perm, String world) {
        PlayerManager playerManager = bootstrap.getPlayerManager();
        for (UUID uuid : playerManager.getPlayers().keySet()) {
            Optional<EnjPlayer> player = playerManager.getPlayer(uuid);

            player.ifPresent(enjPlayer -> enjPlayer.removePermission(perm, world));
        }
    }

    public boolean hasToken(String id) {
        if (hasAlternateId(id))
            return true;

        return tokenModels.containsKey(id);
    }

    public boolean hasAlternateId(String id) {
        return alternateIds.containsKey(id);
    }

    public TokenModel getToken(String id) {
        if (hasAlternateId(id))
            id = alternateIds.get(id);

        return tokenModels.get(id);
    }

    public Set<String> getTokenIds() {
        return new HashSet<>(tokenModels.keySet());
    }

    public Set<String> getAlternateIds() {
        return new HashSet<>(alternateIds.keySet());
    }

    public Set<TokenModel> getTokens() {
        return new HashSet<>(tokenModels.values());
    }

    public Set<Map.Entry<String, TokenModel>> getEntries() {
        return new HashSet<>(tokenModels.entrySet());
    }

    public TokenPermissionGraph getTokenPermissions() {
        return permGraph;
    }

    public void subscribeToTokens() {
        for (Map.Entry<String, TokenModel> entry : tokenModels.entrySet()) {
            subscribeToToken(entry.getValue());
        }
    }

    private void subscribeToToken(TokenModel tokenModel) {
        if (tokenModel == null)
            return;

        NotificationsService service = bootstrap.getNotificationsService();
        if (service != null && !service.isSubscribedToToken(tokenModel.getId()))
            service.subscribeToToken(tokenModel.getId());
    }

}
