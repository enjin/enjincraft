package com.enjin.enjincraft.spigot.token;

import com.enjin.enjincraft.spigot.GraphQLException;
import com.enjin.enjincraft.spigot.NetworkException;
import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.player.EnjPlayer;
import com.enjin.enjincraft.spigot.player.PlayerManager;
import com.enjin.enjincraft.spigot.storage.Database;
import com.enjin.enjincraft.spigot.util.StringUtils;
import com.enjin.enjincraft.spigot.util.TokenUtils;
import com.enjin.sdk.TrustedPlatformClient;
import com.enjin.sdk.graphql.GraphQLResponse;
import com.enjin.sdk.models.token.GetToken;
import com.enjin.sdk.models.token.Token;
import com.enjin.sdk.services.notification.NotificationsService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.NonNull;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
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
    public static final int TOKEN_ALREADYEXISTS = 405;        // Token already exists
    public static final int TOKEN_INVALIDDATA = 406;          // Invalid token data
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
        tokenModels.clear();
        alternateIds.clear();
        permGraph.clear();

        if (!dir.exists())
            dir.mkdirs();

        // Load tokens from directory (assumed fungible and non-fungible base)
        for (File file : dir.listFiles()) {
            // Ignores directories and non-JSON files
            if (file.isDirectory() || !file.getName().endsWith(".json"))
                continue;

            try (InputStreamReader in = new InputStreamReader(new FileInputStream(file), CHARSET)) {
                TokenModel tokenModel = gson.fromJson(in, TokenModel.class);
                tokenModel.load();

                boolean changed = tokenModel.applyBlacklist(bootstrap.getConfig().getPermissionBlacklist());
                if (changed)
                    updateTokenConf(tokenModel);
                else
                    cacheAndSubscribe(tokenModel);
            } catch (Exception e) {
                bootstrap.log(e);
            }
        }

        // Load tokens from db (assumed non-fungible)
        try {
            List<TokenModel> tokens = bootstrap.db().getAllTokens();
            tokens.forEach(tokenModel -> {
                try {
                    setNameFromURIFromBase(tokenModel);
                    tokenModel.setNonfungible(true);
                    tokenModel.load();

                    boolean changed = tokenModel.applyBlacklist(bootstrap.getConfig().getPermissionBlacklist());
                    if (changed)
                        updateTokenConf(tokenModel);
                    else
                        cacheAndSubscribe(tokenModel);
                } catch (Exception e) {
                    bootstrap.log(e);
                }
            });
        } catch (SQLException e) {
            bootstrap.log(e);
        }

        LegacyTokenConverter legacyConverter = new LegacyTokenConverter(bootstrap);
        if (legacyConverter.fileExists())
            legacyConverter.process();
    }

    public int saveToken(@NonNull TokenModel tokenModel) {
        if (tokenModel.isNonFungibleInstance()) {
            setNameFromURIFromBase(tokenModel);

            return saveTokenToDatabase(tokenModel);
        } else {
            return saveTokenToJson(tokenModel);
        }
    }

    private int saveTokenToJson(TokenModel tokenModel) {
        // Prevents tokens with the same alternate id from existing
        String alternateId = tokenModel.getAlternateId();
        String otherFullId = alternateIds.get(alternateId);
        if (alternateId != null && otherFullId != null && !otherFullId.equals(tokenModel.getFullId()))
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
                alternateIds.put(tokenModel.getAlternateId(), tokenModel.getFullId());
        } catch (Exception e) {
            bootstrap.log(e);
            return TOKEN_CREATE_FAILED;
        }

        return TOKEN_CREATE_SUCCESS;
    }

    private int saveTokenToDatabase(TokenModel tokenModel) {
        if (!TokenUtils.isValidFullId(tokenModel.getFullId()) || tokenModel.getNbt() == null)
            return TOKEN_INVALIDDATA;

        TokenModel other = tokenModels.get(tokenModel.getFullId());
        if (other != null)
            return TOKEN_ALREADYEXISTS;

        try {
            bootstrap.db().createToken(tokenModel);
            tokenModel.load();
            cacheAndSubscribe(tokenModel);
        } catch (Exception e) {
            bootstrap.log(e);
            return TOKEN_CREATE_FAILED;
        }

        return TOKEN_CREATE_SUCCESS;
    }

    private void cacheAndSubscribe(TokenModel tokenModel) {
        tokenModels.put(tokenModel.getFullId(), tokenModel);
        permGraph.addToken(tokenModel);
        subscribeToToken(tokenModel);

        if (tokenModel.getAlternateId() != null)
            alternateIds.put(tokenModel.getAlternateId(), TokenUtils.createFullId(tokenModel.getId()));
    }

    public int updateTokenConf(TokenModel tokenModel) {
        if (tokenModel.isNonFungibleInstance())
            return updateTokenConfDatabase(tokenModel);
        else
            return updateTokenConfJson(tokenModel);
    }

    private int updateTokenConfJson(TokenModel tokenModel) {
        return updateTokenConfJson(tokenModel, true);
    }

    private int updateTokenConfJson(TokenModel tokenModel, boolean updateOnPlayers) {
        TokenModel oldModel = tokenModels.get(tokenModel.getFullId());
        if (!dir.exists() || oldModel == null)
            return saveToken(tokenModel);

        tokenModel.applyBlacklist(bootstrap.getConfig().getPermissionBlacklist());
        boolean newNbt = !tokenModel.getNbt().equals(oldModel.getNbt());

        File file = new File(dir, String.format("%s%s", tokenModel.getId(), JSON_EXT));
        try (OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file, false), CHARSET)) {
            gson.toJson(tokenModel, out);
            tokenModel.load();
            tokenModels.put(tokenModel.getFullId(), tokenModel);
        } catch (Exception e) {
            bootstrap.log(e);
            return TOKEN_UPDATE_FAILED;
        }

        if (newNbt && updateOnPlayers)
            updateTokenOnPlayers(tokenModel.getFullId());

        return TOKEN_UPDATE_SUCCESS;
    }

    private int updateTokenConfDatabase(TokenModel tokenModel) {
        return updateTokenConfDatabase(tokenModel, true);
    }

    private int updateTokenConfDatabase(TokenModel tokenModel, boolean updateOnPlayers) {
        String id    = tokenModel.getId();
        String index = tokenModel.getIndex();
        String nbt   = tokenModel.getNbt();

        if (TokenUtils.isValidId(id) || TokenUtils.isValidIndex(index) || nbt == null)
            return TOKEN_INVALIDDATA;

        TokenModel oldModel = tokenModels.get(tokenModel.getFullId());
        if (oldModel == null)
            return saveTokenToDatabase(tokenModel);

        tokenModel.applyBlacklist(bootstrap.getConfig().getPermissionBlacklist());
        boolean newNbt = !nbt.equals(oldModel.getNbt());

        Database db = bootstrap.db();
        try {
            // Removes any permissions from the db that the token no longer has
            Set<TokenPermission> permissions = new HashSet<>(db.getPermissions(id, id));
            permissions.removeAll(tokenModel.getAssignablePermissions());
            for (TokenPermission permission : permissions) {
                db.removePermission(id, index, permission);
            }

            db.updateNBT(id, index, nbt);
            tokenModel.load();
            tokenModels.put(tokenModel.getFullId(), tokenModel);
        } catch (Exception e) {
            bootstrap.log(e);
            return TOKEN_UPDATE_FAILED;
        }

        if (newNbt && updateOnPlayers)
            updateTokenOnPlayers(tokenModel.getFullId());

        return TOKEN_UPDATE_SUCCESS;
    }

    private void updateTokenOnPlayers(String fullId) {
        PlayerManager playerManager = bootstrap.getPlayerManager();
        if (playerManager == null)
            return;

        for (UUID uuid : playerManager.getPlayers().keySet()) {
            Optional<EnjPlayer> player = playerManager.getPlayer(uuid);
            player.ifPresent(enjPlayer -> enjPlayer.updateToken(fullId));
        }
    }

    public int updateAlternateId(String id, String newAlternateId) {
        TokenModel tokenModel = getToken(id);
        if (tokenModel == null)
            return TOKEN_NOSUCHTOKEN;

        if (tokenModel.getAlternateId() != null && newAlternateId.equals(tokenModel.getAlternateId()))
            return TOKEN_HASNICKNAME;
        else if (alternateIds.containsKey(newAlternateId))
            return TOKEN_DUPLICATENICKNAME;

        if (tokenModel.getAlternateId() != null)
            alternateIds.remove(tokenModel.getAlternateId());

        tokenModel.setAlternateId(newAlternateId);
        alternateIds.put(newAlternateId, id);

        return updateTokenConfJson(tokenModel, false);
    }

    public int updateMetadataURI(String id, String metadataURI) {
        TokenModel tokenModel = getToken(id);
        if (tokenModel == null)
            return TOKEN_NOSUCHTOKEN;

        if (StringUtils.isEmpty(metadataURI))
            metadataURI = null;

        tokenModel.setMetadataURI(metadataURI);
        int status = updateTokenConfJson(tokenModel, true);

        // TODO: Check if possible to set up URIs for particular NFT index.
        // Updates necessary non-fungible tokens if the model is the base model
        if (status == TOKEN_UPDATE_SUCCESS
                && tokenModel.isNonfungible()
                && tokenModel.isBaseModel())
            updateNameFromURIOnTokens(tokenModel);

        return status;
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

    private void setNameFromURIFromBase(TokenModel tokenModel) {
        if (tokenModel == null)
            return;

        TokenModel baseModel = getToken(tokenModel.getId());
        if (baseModel == null
                || !baseModel.isNonfungible()
                || tokenModel == baseModel)
            return;

        tokenModel.setNameFromURI(baseModel.getNameFromURI());
        tokenModel.load();

        updateTokenOnPlayers(tokenModel.getFullId());
    }

    private void updateNameFromURIOnTokens(TokenModel baseModel) {
        if (baseModel == null || !baseModel.isNonfungible())
            return;

        for (TokenModel tokenModel : tokenModels.values()) {
            if (tokenModel.getId().equals(baseModel.getId()))
                setNameFromURIFromBase(tokenModel);
        }
    }

    public int addPermissionToToken(String perm, String id, String world) {
        if (bootstrap.getConfig().getPermissionBlacklist().contains(perm))
            return PERM_ADDED_BLACKLISTED;

        TokenModel tokenModel = getToken(id);
        if (tokenModel == null)
            return TOKEN_NOSUCHTOKEN;
        else if (!tokenModel.addPermissionToWorld(perm, world)) // Checks if the permission was not added
            return PERM_ADDED_DUPLICATEPERM;

        permGraph.addTokenPerm(perm, tokenModel.getFullId(), world);

        int status = tokenModel.isNonFungibleInstance()
                ? addPermissionToDB(perm, tokenModel.getFullId(), Collections.singleton(world))
                : updateTokenConf(tokenModel);
        addPermissionToPlayers(perm, tokenModel.getFullId(), world);

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
        else if (!tokenModel.addPermissionToWorlds(perm, worlds)) // Checks if the permission was not added
            return PERM_ADDED_DUPLICATEPERM;

        permGraph.addTokenPerm(perm, tokenModel.getFullId(), worlds);

        int status = tokenModel.isNonFungibleInstance()
                ? addPermissionToDB(perm, tokenModel.getFullId(), worlds)
                : updateTokenConf(tokenModel);
        worlds.forEach(world -> addPermissionToPlayers(perm, tokenModel.getFullId(), world));

        if (status != TOKEN_UPDATE_SUCCESS)
            return status;

        return PERM_ADDED_SUCCESS;
    }

    private int addPermissionToDB(String perm, String fullId, Collection<String> worlds) {
        TokenModel tokenModel = tokenModels.get(fullId);
        if (tokenModel == null)
            return TOKEN_NOSUCHTOKEN;

        String id    = tokenModel.getId();
        String index = tokenModel.getIndex();
        Database db = bootstrap.db();
        try {
            Collection<String> currentWorlds = db.getPermissionWorlds(id, index, perm);
            if (currentWorlds.contains(GLOBAL)) {
                return PERM_ADDED_DUPLICATEPERM;
            } else if (worlds.contains(GLOBAL)) {
                db.removePermission(id, index, perm, currentWorlds);
                worlds.retainAll(Collections.singleton(GLOBAL));
            }

            db.addPermission(id, index, perm, worlds);
        } catch (Exception e) {
            bootstrap.log(e);
            return TOKEN_UPDATE_FAILED;
        }

        return TOKEN_UPDATE_SUCCESS;
    }

    private void addPermissionToPlayers(String perm, String id, String world) {
        PlayerManager playerManager = bootstrap.getPlayerManager();

        for (UUID uuid : playerManager.getPlayers().keySet()) {
            Optional<EnjPlayer> player = playerManager.getPlayer(uuid);
            player.ifPresent(enjPlayer -> enjPlayer.addPermission(perm, id, world));
        }
    }

    public int removePermissionFromToken(String perm, String id, String world) {
        TokenModel tokenModel = getToken(id);
        if (tokenModel == null)
            return TOKEN_NOSUCHTOKEN;
        else if (!tokenModel.removePermissionFromWorld(perm, world)) // Checks if the permission was not removed
            return PERM_REMOVED_NOPERMONTOKEN;

        permGraph.removeTokenPerm(perm, tokenModel.getFullId(), world);

        int status = tokenModel.isNonFungibleInstance()
                ? removePermissionFromDB(perm, tokenModel.getFullId(), Collections.singleton(world))
                : updateTokenConfJson(tokenModel);
        removePermissionFromPlayers(perm, world);

        if (status != TOKEN_UPDATE_SUCCESS)
            return status;

        return PERM_REMOVED_SUCCESS;
    }

    public int removePermissionFromToken(String perm, String id, Collection<String> worlds) {
        TokenModel tokenModel = getToken(id);
        if (tokenModel == null)
            return TOKEN_NOSUCHTOKEN;
        else if (!tokenModel.removePermissionFromWorlds(perm, worlds)) // Checks if the permission was not removed
            return PERM_REMOVED_NOPERMONTOKEN;

        permGraph.removeTokenPerm(perm, tokenModel.getFullId(), worlds);

        int status = tokenModel.isNonFungibleInstance()
                ? removePermissionFromDB(perm, tokenModel.getFullId(), worlds)
                : updateTokenConfJson(tokenModel);
        worlds.forEach(world -> removePermissionFromPlayers(perm, world));

        if (status != TOKEN_UPDATE_SUCCESS)
            return status;

        return PERM_REMOVED_SUCCESS;
    }

    private int removePermissionFromDB(String perm, String fullId, Collection<String> worlds) {
        TokenModel tokenModel = tokenModels.get(fullId);
        if (tokenModel == null)
            return TOKEN_NOSUCHTOKEN;

        String id    = tokenModel.getId();
        String index = tokenModel.getIndex();
        Database db = bootstrap.db();
        try {
            Collection<String> currentWorlds = db.getPermissionWorlds(id, index, perm);
            boolean currentIsGlobal = currentWorlds.contains(GLOBAL);
            boolean newIsGlobal     = worlds.contains(GLOBAL);
            if (currentIsGlobal && !newIsGlobal)
                return PERM_REMOVED_NOPERMONTOKEN;
            else if (newIsGlobal)
                worlds = currentWorlds;

            db.removePermission(id, index, perm, worlds);
        } catch (Exception e) {
            bootstrap.log(e);
            return TOKEN_UPDATE_FAILED;
        }

        return TOKEN_UPDATE_SUCCESS;
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
        else if (TokenUtils.isValidId(id))
            id = TokenUtils.createFullId(id);
        else if (!TokenUtils.isValidFullId(id))
            return false;

        return tokenModels.containsKey(id);
    }

    public boolean hasAlternateId(String id) {
        return alternateIds.containsKey(id);
    }

    public TokenModel getToken(String id) {
        if (hasAlternateId(id))
            id = alternateIds.get(id);
        else if (TokenUtils.isValidId(id))
            id = TokenUtils.createFullId(id);
        else if (!TokenUtils.isValidFullId(id))
            return null;

        return tokenModels.get(id);
    }

    public Set<String> getFullIds() {
        return new HashSet<>(tokenModels.keySet());
    }

    public Set<String> getTokenIds() {
        Set<String> tokenIds = new HashSet<>();
        tokenModels.keySet().forEach(fullId -> tokenIds.add(TokenUtils.getTokenID(fullId)));

        return tokenIds;
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
        return new TokenPermissionGraph(permGraph);
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
