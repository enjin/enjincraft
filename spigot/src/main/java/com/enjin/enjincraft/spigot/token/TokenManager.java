package com.enjin.enjincraft.spigot.token;

import com.enjin.enjincraft.spigot.GraphQLException;
import com.enjin.enjincraft.spigot.NetworkException;
import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.player.EnjPlayer;
import com.enjin.enjincraft.spigot.player.PlayerManagerImpl;
import com.enjin.enjincraft.spigot.storage.Database;
import com.enjin.enjincraft.spigot.util.StringUtils;
import com.enjin.enjincraft.spigot.util.TokenUtils;
import com.enjin.enjincraft.spigot.wallet.TokenWalletViewState;
import com.enjin.sdk.TrustedPlatformClient;
import com.enjin.sdk.graphql.GraphQLResponse;
import com.enjin.sdk.models.token.GetToken;
import com.enjin.sdk.models.token.Token;
import com.enjin.sdk.services.notification.NotificationsService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;

public class TokenManager {

    // Status codes
    public static final int TOKEN_CREATE_SUCCESS       = 200; // Token was created
    public static final int TOKEN_UPDATE_SUCCESS       = 201; // Token was updated
    public static final int TOKEN_DELETE_SUCCESS       = 202; // Token was deleted
    public static final int PERM_ADDED_SUCCESS         = 210; // Permission was added
    public static final int PERM_REMOVED_SUCCESS       = 250; // Permission was removed
    public static final int TOKEN_EXPORT_SUCCESS       = 290; // Token(s) were exported
    public static final int TOKEN_IMPORT_SUCCESS       = 291; // Token(s) were imported
    public static final int TOKEN_EXPORT_EMPTY         = 292; // No token to export
    public static final int TOKEN_IMPORT_EMPTY         = 293; // No token to import
    public static final int TOKEN_CREATE_FAILED        = 400; // Token was not created
    public static final int TOKEN_UPDATE_FAILED        = 401; // Token was not updated
    public static final int TOKEN_DELETE_FAILED        = 402; // Token was not deleted
    public static final int TOKEN_NOSUCHTOKEN          = 403; // The token does not exist
    public static final int TOKEN_ALREADYEXISTS        = 404; // Token already exists
    public static final int TOKEN_INVALIDDATA          = 405; // Invalid token data
    public static final int TOKEN_DELETE_FAILEDNFTBASE = 406; // Deleting NFT base when still has instances
    public static final int TOKEN_MARKEDFORDELETION    = 407; // Token is marked for deletion
    public static final int TOKEN_ISNOTBASE            = 408; // Token is not base model
    public static final int TOKEN_CREATE_FAILEDNFTBASE = 409; // Unable to create base model
    public static final int TOKEN_DUPLICATENICKNAME    = 420; // Token alternate id already exists
    public static final int TOKEN_HASNICKNAME          = 421; // Token has alternate
    public static final int TOKEN_INVALIDNICKNAME      = 422; // Nickname cannot be a valid token id
    public static final int TOKEN_HASWALLETVIEWSTATE   = 430; // Token has wallet view state
    public static final int PERM_ADDED_DUPLICATEPERM   = 440; // Permission is a duplicate
    public static final int PERM_ADDED_BLACKLISTED     = 441; // Permission is blacklisted
    public static final int PERM_REMOVED_NOPERMONTOKEN = 450; // Permission is not assigned
    public static final int PERM_ISGLOBAL              = 460; // Permission is global
    public static final int TOKEN_EXPORT_FAILED        = 490; // Token(s) were not exported
    public static final int TOKEN_IMPORT_FAILED        = 491; // Token(s) were not imported
    public static final int TOKEN_EXPORT_PARTIAL       = 492; // Some token(s) were not exported
    public static final int TOKEN_IMPORT_PARTIAL       = 493; // Some token(s) were not imported

    public static final Charset CHARSET = StandardCharsets.UTF_8;
    public static final String EXPORT_FOLDER = "tokens_export";
    public static final String IMPORT_FOLDER = "tokens_import";
    public static final String TOKENS_FOLDER = "tokens";
    public static final String JSON_EXT = ".json";
    public static final int JSON_EXT_LENGTH = JSON_EXT.length();
    public static final String GLOBAL = "*";

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(TokenPermission.class, new TokenPermission.TokenPermissionSerializer())
            .registerTypeAdapter(TokenPermission.class, new TokenPermission.TokenPermissionDeserializer())
            .setPrettyPrinting()
            .create();

    private final SpigotBootstrap bootstrap;
    @Getter(value = AccessLevel.PACKAGE, onMethod_ = {@NotNull})
    private final File dir;
    private final File exportDir;
    private final File importDir;
    private final Map<String, TokenModel> tokenModels = new HashMap<>();
    private final Map<String, String> alternateIds = new HashMap<>();
    private final TokenPermissionGraph permGraph = new TokenPermissionGraph();

    public TokenManager(SpigotBootstrap bootstrap) {
        this(bootstrap, bootstrap.plugin().getDataFolder());
    }

    public TokenManager(SpigotBootstrap bootstrap, File dir) {
        this.bootstrap = bootstrap;
        this.dir = new File(dir, TOKENS_FOLDER);
        this.exportDir = new File(dir, EXPORT_FOLDER);
        this.importDir = new File(dir, IMPORT_FOLDER);

        this.exportDir.mkdirs();
        this.importDir.mkdirs();
    }

    public void loadTokens() {
        tokenModels.clear();
        alternateIds.clear();
        permGraph.clear();

        // Load tokens from db
        try {
            List<TokenModel> tokens = bootstrap.db().getAllTokens();
            List<TokenModel> nftInstances = new ArrayList<>();
            List<String> permissionBlacklist = bootstrap.getConfig().getPermissionBlacklist();

            // Loads base tokens
            tokens.forEach(tokenModel -> {
                try {
                    if (tokenModel.isNonFungibleInstance()) { // Store NFT instances for later
                        nftInstances.add(tokenModel);
                        return;
                    }

                    tokenModel.load();

                    boolean changed = tokenModel.applyBlacklist(permissionBlacklist);
                    if (changed)
                        updateTokenPermissionsDatabase(tokenModel);

                    cacheAndSubscribe(tokenModel);
                } catch (Exception e) {
                    bootstrap.log(e);
                }
            });

            // Loads NFT instances
            nftInstances.forEach(tokenModel -> {
                try {
                    String baseFullId = TokenUtils.createFullId(tokenModel.getId());
                    if (!tokenModels.containsKey(baseFullId))
                        throw new IllegalStateException(String.format("Base model does not exist for non-fungible token %s", tokenModel.getId()));

                    tokenModel.load();

                    boolean changed = tokenModel.applyBlacklist(permissionBlacklist);
                    if (changed)
                        updateTokenPermissionsDatabase(tokenModel);

                    cacheAndSubscribe(tokenModel);
                } catch (Exception e) {
                    bootstrap.log(e);
                }
            });
        } catch (Exception e) {
            bootstrap.log(e);
        }

        // Process legacy token configuration files
        new LegacyTokenConverter(bootstrap).process();
    }

    public int saveToken(@NonNull TokenModel tokenModel) throws NullPointerException {
        String     alternateId = tokenModel.getAlternateId();
        TokenModel other       = getToken(alternateId);
        if (other != null && !other.getId().equals(tokenModel.getId())) { // Alternate id already exists
            return TOKEN_DUPLICATENICKNAME;
        } else if (alternateId != null && !isValidAlternateId(alternateId)) { // Alternate id is invalid
            return TOKEN_INVALIDNICKNAME;
        } else if (tokenModel.isMarkedForDeletion()) { // Token is marked for deletion
            return TOKEN_MARKEDFORDELETION;
        } else if (!isValidToken(tokenModel)) { // Token has invalid data
            return TOKEN_INVALIDDATA;
        } else if (tokenModels.containsKey(tokenModel.getFullId())) { // Token already exists
            return TOKEN_ALREADYEXISTS;
        } else if (tokenModel.isNonFungibleInstance()) { // Token is instance of a non-fungible token
            // Creates the base model if necessary
            if (!hasToken(tokenModel.getId())) {
                int baseStatus = saveToken(TokenModel.builder()
                        .id(tokenModel.getId())
                        .nonfungible(true)
                        .alternateId(tokenModel.getAlternateId())
                        .walletViewState(tokenModel.getWalletViewState())
                        .build());
                if (baseStatus != TOKEN_CREATE_SUCCESS)
                    return TOKEN_CREATE_FAILEDNFTBASE;
            }

            setNameFromURIFromBase(tokenModel);
        }

        tokenModel.applyBlacklist(bootstrap.getConfig().getPermissionBlacklist());

        int status = saveTokenToDatabase(tokenModel);
        if (status == TOKEN_CREATE_SUCCESS) {
            cacheAndSubscribe(tokenModel);

            PlayerManagerImpl playerManager = bootstrap.getPlayerManager();
            if (playerManager != null) {
                playerManager.getPlayers()
                        .values()
                        .forEach(enjPlayer -> {
                            enjPlayer.validateInventory();
                            enjPlayer.addTokenPermissions(tokenModel);
                        });
            }
        }

        return status;
    }

    private int saveTokenToDatabase(TokenModel tokenModel) {
        Database db = bootstrap.db();
        try {
            if (tokenModel.isBaseModel())
                db.createToken(tokenModel);

            db.createTokenInstance(tokenModel);
            tokenModel.load();
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

        if (tokenModel.isBaseModel() && tokenModel.getAlternateId() != null) {
            if (isValidAlternateId(tokenModel.getAlternateId()))
                alternateIds.put(tokenModel.getAlternateId(), tokenModel.getFullId());
            else
                bootstrap.debug(String.format("Invalid alternate id for token \"%s\"", tokenModel.getId()));
        }
    }

    public int updateTokenConf(@NonNull TokenModel tokenModel) throws NullPointerException {
        return updateTokenConf(tokenModel, true);
    }

    public int updateTokenConf(@NonNull TokenModel tokenModel,
                               boolean updateOnPlayers) throws NullPointerException {
        if (tokenModel.isMarkedForDeletion())
            return TOKEN_MARKEDFORDELETION;
        else if (!isValidToken(tokenModel))
            return TOKEN_INVALIDDATA;
        else if (!tokenModels.containsKey(tokenModel.getFullId()))
            return TOKEN_NOSUCHTOKEN;

        int status = updateTokenConfDatabase(tokenModel);
        if (status == TOKEN_UPDATE_SUCCESS) {
            tokenModels.put(tokenModel.getFullId(), tokenModel);

            if (updateOnPlayers)
                updateTokenOnPlayers(tokenModel.getFullId());
        }

        return status;
    }

    private int updateTokenConfDatabase(TokenModel tokenModel) {
        Database db = bootstrap.db();
        try {
            if (tokenModel.isBaseModel())
                db.updateToken(tokenModel);

            db.updateTokenInstance(tokenModel);
            tokenModel.load();
        } catch (Exception e) {
            bootstrap.log(e);
            return TOKEN_UPDATE_FAILED;
        }

        return TOKEN_UPDATE_SUCCESS;
    }

    private void updateTokenPermissionsDatabase(TokenModel tokenModel) throws SQLException {
        Database db = bootstrap.db();
        String id    = tokenModel.getId();
        String index = tokenModel.getIndex();
        List<TokenPermission> dbPermissions;

        // Removes any permissions from the database that the token no longer has
        dbPermissions = db.getPermissions(id, index);
        for (TokenPermission permission : dbPermissions) {
            TokenPermission other = tokenModel.getPermission(permission.getPermission());
            if (other != null)
                permission.removeWorlds(other.getWorlds());

            if (!permission.getWorlds().isEmpty())
                db.deletePermission(id, index, permission);
        }

        // Adds any permissions from the token that are not in the database
        dbPermissions = db.getPermissions(id, index);
        for (TokenPermission permission : tokenModel.getAssignablePermissions()) {
            int idx = dbPermissions.indexOf(permission);
            if (idx >= 0) {
                TokenPermission other = dbPermissions.get(idx);
                permission.removeWorlds(other.getWorlds());
            }

            if (!permission.getWorlds().isEmpty())
                db.addPermission(id, index, permission);
        }
    }

    private void updateTokenOnPlayers(String fullId) {
        PlayerManagerImpl playerManager = bootstrap.getPlayerManager();
        if (playerManager == null)
            return;

        for (UUID uuid : playerManager.getPlayers().keySet()) {
            EnjPlayer player = playerManager.getPlayer(uuid);
            if (player != null)
                player.updateToken(fullId);
        }
    }

    public int updateAlternateId(String id, String newAlternateId) {
        TokenModel tokenModel = getToken(id);
        if (tokenModel == null)
            return TOKEN_NOSUCHTOKEN;
        else if (!tokenModel.isBaseModel())
            return TOKEN_ISNOTBASE;
        else if (tokenModel.getAlternateId() != null && newAlternateId.equals(tokenModel.getAlternateId()))
            return TOKEN_HASNICKNAME;
        else if (alternateIds.containsKey(newAlternateId))
            return TOKEN_DUPLICATENICKNAME;
        else if (!isValidAlternateId(newAlternateId))
            return TOKEN_INVALIDNICKNAME;
        else if (tokenModel.getAlternateId() != null)
            alternateIds.remove(tokenModel.getAlternateId());

        tokenModel.setAlternateId(newAlternateId);
        alternateIds.put(newAlternateId, id);

        return updateTokenConf(tokenModel, false);
    }

    public int updateMetadataURI(String id, String metadataURI) {
        TokenModel tokenModel = getToken(id);
        if (tokenModel == null)
            return TOKEN_NOSUCHTOKEN;
        else if (!tokenModel.isBaseModel())
            return TOKEN_ISNOTBASE;

        if (StringUtils.isEmpty(metadataURI))
            metadataURI = null;

        tokenModel.setMetadataURI(metadataURI);
        int status = updateTokenConf(tokenModel, true);

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
        client.getTokenService().getTokenAsync(new GetToken()
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

    public int updateWalletViewState(@NonNull String id,
                                     @NonNull TokenWalletViewState walletViewState) throws NullPointerException {
        TokenModel baseModel = getToken(id);
        if (baseModel == null)
            return TOKEN_NOSUCHTOKEN;
        else if (!baseModel.isBaseModel())
            return TOKEN_ISNOTBASE;
        else if (baseModel.getWalletViewState() == walletViewState)
            return TOKEN_HASWALLETVIEWSTATE;

        baseModel.setWalletViewState(walletViewState);

        int status = updateTokenConf(baseModel, false);
        if (status == TOKEN_UPDATE_SUCCESS) {
            updateNonfungibleInstances(baseModel);

            bootstrap.getPlayerManager()
                    .getPlayers()
                    .values()
                    .forEach(EnjPlayer::validateInventory);
        }

        return status;
    }

    private void updateNonfungibleInstances(TokenModel baseModel) {
        if (baseModel == null
                || !baseModel.isBaseModel()
                || !baseModel.isNonfungible())
            return;

        String alternateId = baseModel.getAlternateId();
        TokenWalletViewState walletViewState = baseModel.getWalletViewState();
        boolean markedForDeletion = baseModel.isMarkedForDeletion();

        tokenModels.values().forEach(tokenModel -> {
            String tokenId    = tokenModel.getId();
            String tokenIndex = tokenModel.getIndex();
            if (!tokenId.equals(baseModel.getId()) || tokenIndex.equals(TokenUtils.BASE_INDEX))
                return;

            tokenModel.setAlternateId(alternateId);
            tokenModel.setWalletViewState(walletViewState);
            tokenModel.setMarkedForDeletion(markedForDeletion);
        });
    }

    public int deleteTokenConf(@NonNull String id) throws NullPointerException {
        TokenModel tokenModel = getToken(id);
        if (tokenModel == null) {
            return TOKEN_NOSUCHTOKEN;
        } else if (tokenModel.isNonfungible() && tokenModel.isBaseModel()) {
            for (TokenModel other : tokenModels.values()) {
                if (other.getId().equals(tokenModel.getId()) && other != tokenModel)
                    return TOKEN_DELETE_FAILEDNFTBASE;
            }
        }

        int status = deleteFromDB(tokenModel);
        if (status == TOKEN_DELETE_SUCCESS) {
            tokenModel.setMarkedForDeletion(true);
            uncacheAndUnsubscribe(tokenModel);

            PlayerManagerImpl playerManager = bootstrap.getPlayerManager();
            if (playerManager != null) {
                playerManager.getPlayers()
                        .values()
                        .forEach(enjPlayer -> {
                            enjPlayer.validateInventory();
                            enjPlayer.removeTokenPermissions(tokenModel);
                        });
            }
        }

        return status;
    }

    private int deleteFromDB(TokenModel tokenModel) {
        try {
            if (tokenModel.isBaseModel())
                bootstrap.db().deleteToken(tokenModel.getId());
            else
                bootstrap.db().deleteTokenInstance(tokenModel.getId(), tokenModel.getIndex());
        } catch (Exception e) {
            bootstrap.log(e);
            return TOKEN_DELETE_FAILED;
        }

        return TOKEN_DELETE_SUCCESS;
    }

    private void uncacheAndUnsubscribe(TokenModel tokenModel) {
        tokenModels.remove(tokenModel.getFullId());
        permGraph.removeToken(tokenModel);

        if (tokenModel.isBaseModel()) {
            unsubscribeToToken(tokenModel);

            if (tokenModel.getAlternateId() != null)
                alternateIds.remove(tokenModel.getAlternateId());
        }
    }

    public int addPermissionToToken(String perm, String id, String world) {
        if (bootstrap.getConfig().getPermissionBlacklist().contains(perm))
            return PERM_ADDED_BLACKLISTED;

        TokenModel tokenModel = getToken(id);
        if (tokenModel == null)
            return TOKEN_NOSUCHTOKEN;

        TokenPermission permission = tokenModel.getPermission(perm);
        if (permission != null && permission.isGlobal() && !world.equals(GLOBAL))
            return PERM_ISGLOBAL;
        else if (!tokenModel.addPermissionToWorld(perm, world)) // Checks if the permission was not added
            return PERM_ADDED_DUPLICATEPERM;

        permGraph.addTokenPerm(perm, tokenModel.getFullId(), world);

        int status = addPermissionToDB(perm, tokenModel.getFullId(), Collections.singleton(world));
        if (status != TOKEN_UPDATE_SUCCESS) {
            addPermissionToPlayers(perm, tokenModel.getFullId(), world);
            return status;
        }

        return PERM_ADDED_SUCCESS;
    }

    public int addPermissionToToken(String perm, String id, Collection<String> worlds) {
        if (bootstrap.getConfig().getPermissionBlacklist().contains(perm))
            return PERM_ADDED_BLACKLISTED;

        TokenModel tokenModel = getToken(id);
        if (tokenModel == null)
            return TOKEN_NOSUCHTOKEN;

        TokenPermission permission = tokenModel.getPermission(perm);
        if (permission != null && permission.isGlobal() && !worlds.contains(GLOBAL))
            return PERM_ISGLOBAL;
        else if (!tokenModel.addPermissionToWorlds(perm, worlds)) // Checks if the permission was not added
            return PERM_ADDED_DUPLICATEPERM;

        permGraph.addTokenPerm(perm, tokenModel.getFullId(), worlds);

        int status = addPermissionToDB(perm, tokenModel.getFullId(), worlds);
        if (status != TOKEN_UPDATE_SUCCESS) {
            worlds.forEach(world -> addPermissionToPlayers(perm, tokenModel.getFullId(), world));
            return status;
        }

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
                db.deletePermission(id, index, perm, currentWorlds);
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
        PlayerManagerImpl playerManager = bootstrap.getPlayerManager();

        for (UUID uuid : playerManager.getPlayers().keySet()) {
            EnjPlayer player = playerManager.getPlayer(uuid);
            if (player != null)
                player.addPermission(perm, id, world);
        }
    }

    public int removePermissionFromToken(String perm, String id, String world) {
        TokenModel tokenModel = getToken(id);
        if (tokenModel == null)
            return TOKEN_NOSUCHTOKEN;

        TokenPermission permission = tokenModel.getPermission(perm);
        if (permission != null && permission.isGlobal() && !world.equals(GLOBAL))
            return PERM_ISGLOBAL;
        else if (!tokenModel.removePermissionFromWorld(perm, world)) // Checks if the permission was not removed
            return PERM_REMOVED_NOPERMONTOKEN;

        permGraph.removeTokenPerm(perm, tokenModel.getFullId(), world);

        int status = removePermissionFromDB(perm, tokenModel.getFullId(), Collections.singleton(world));
        if (status != TOKEN_UPDATE_SUCCESS) {
            removePermissionFromPlayers(perm, world);
            return status;
        }

        return PERM_REMOVED_SUCCESS;
    }

    public int removePermissionFromToken(String perm, String id, Collection<String> worlds) {
        TokenModel tokenModel = getToken(id);
        if (tokenModel == null)
            return TOKEN_NOSUCHTOKEN;

        TokenPermission permission = tokenModel.getPermission(perm);
        if (permission != null && permission.isGlobal() && !worlds.contains(GLOBAL))
            return PERM_ISGLOBAL;
        else if (!tokenModel.removePermissionFromWorlds(perm, worlds)) // Checks if the permission was not removed
            return PERM_REMOVED_NOPERMONTOKEN;

        permGraph.removeTokenPerm(perm, tokenModel.getFullId(), worlds);

        int status = removePermissionFromDB(perm, tokenModel.getFullId(), worlds);
        if (status != TOKEN_UPDATE_SUCCESS) {
            worlds.forEach(world -> removePermissionFromPlayers(perm, world));
            return status;
        }

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

            db.deletePermission(id, index, perm, worlds);
        } catch (Exception e) {
            bootstrap.log(e);
            return TOKEN_UPDATE_FAILED;
        }

        return TOKEN_UPDATE_SUCCESS;
    }

    private void removePermissionFromPlayers(String perm, String world) {
        PlayerManagerImpl playerManager = bootstrap.getPlayerManager();

        for (UUID uuid : playerManager.getPlayers().keySet()) {
            EnjPlayer player = playerManager.getPlayer(uuid);
            if (player != null)
                player.removePermission(perm, world);
        }
    }

    public boolean hasToken(@NonNull String id) throws NullPointerException {
        if (hasAlternateId(id))
            return true;
        else if (TokenUtils.isValidId(id))
            id = TokenUtils.createFullId(id);
        else if (!TokenUtils.isValidFullId(id))
            return false;

        return tokenModels.containsKey(id);
    }

    public boolean hasAlternateId(@NonNull String id) throws NullPointerException {
        return alternateIds.containsKey(id);
    }

    @Nullable
    public TokenModel getToken(@Nullable ItemStack is) {
        try {
            TokenModel tokenModel = tokenModels.get(TokenUtils.createFullId(TokenUtils.getTokenID(is),
                                                                            TokenUtils.getTokenIndex(is)));
            if (tokenModel != null && tokenModel.isNonfungible() != TokenUtils.isNonFungible(is))
                throw new IllegalStateException("Token item has different fungibility state than its registered model");

            return tokenModel;
        } catch (IllegalArgumentException ignored) {
        } catch (Exception e) {
            bootstrap.log(e);
        }

        return null;
    }

    @Nullable
    public TokenModel getToken(@NonNull String id) throws NullPointerException {
        if (hasAlternateId(id))
            id = alternateIds.get(id);
        else if (TokenUtils.isValidId(id))
            id = TokenUtils.createFullId(id);
        else if (!TokenUtils.isValidFullId(id))
            return null;

        return tokenModels.get(id);
    }

    @NotNull
    public Set<String> getFullIds() {
        return new HashSet<>(tokenModels.keySet());
    }

    @NotNull
    public Set<String> getTokenIds() {
        Set<String> tokenIds = new HashSet<>();
        tokenModels.keySet().forEach(fullId -> tokenIds.add(TokenUtils.getTokenID(fullId)));

        return tokenIds;
    }

    @NotNull
    public Set<String> getAlternateIds() {
        return new HashSet<>(alternateIds.keySet());
    }

    @NotNull
    public Set<TokenModel> getTokens() {
        return new HashSet<>(tokenModels.values());
    }

    @NotNull
    public Set<Map.Entry<String, TokenModel>> getEntries() {
        return new HashSet<>(tokenModels.entrySet());
    }

    @NotNull
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

    private void unsubscribeToToken(TokenModel tokenModel) {
        if (tokenModel == null)
            return;

        NotificationsService service = bootstrap.getNotificationsService();
        if (service != null && service.isSubscribedToToken(tokenModel.getId()))
            service.unsubscribeToToken(tokenModel.getId());
    }

    public int exportTokens() {
        if (!exportDir.exists()) {
            try {
                if (!exportDir.mkdirs())
                    throw new Exception("Unable to create token export directory");
            } catch (Exception e) {
                bootstrap.log(e);
                return TOKEN_EXPORT_FAILED;
            }
        }

        Collection<TokenModel> tokens = tokenModels.values();
        if (tokens.isEmpty())
            return TOKEN_EXPORT_EMPTY;

        int exportCount = 0;
        for (TokenModel tokenModel : tokens) {
            if (exportToken(tokenModel) == TOKEN_EXPORT_SUCCESS) {
                exportCount++;
            } else {
                String name = StringUtils.isEmpty(tokenModel.getAlternateId())
                        ? tokenModel.getId()
                        : tokenModel.getAlternateId();
                String msg = tokenModel.isNonFungibleInstance()
                        ? String.format("Failed to export token \"%s\" #%d", name, TokenUtils.convertIndexToLong(tokenModel.getIndex()))
                        : String.format("Failed to export token \"%s\"", name);
                bootstrap.debug(msg);
            }
        }

        if (exportCount == tokens.size())
            return TOKEN_EXPORT_SUCCESS;
        else if (exportCount > 0)
            return TOKEN_EXPORT_PARTIAL;
        else
            return TOKEN_EXPORT_FAILED;
    }

    public int exportToken(@NonNull String id) throws NullPointerException {
        TokenModel tokenModel = getToken(id);
        if (tokenModel == null) {
            return TOKEN_NOSUCHTOKEN;
        } else if (!exportDir.exists()) {
            try {
                if (!exportDir.mkdirs())
                    throw new Exception("Unable to create token export directory");
            } catch (Exception e) {
                bootstrap.log(e);
                return TOKEN_EXPORT_FAILED;
            }
        }

        // Checks if non-fungible instances need to also be exported
        if (tokenModel.isNonfungible()
                && tokenModel.isBaseModel()
                && (id.equals(tokenModel.getId()) || id.equals(tokenModel.getAlternateId()))) {
            String name = StringUtils.isEmpty(tokenModel.getAlternateId())
                    ? tokenModel.getId()
                    : tokenModel.getAlternateId();

            for (TokenModel model : tokenModels.values()) {
                if (model != tokenModel
                        && model.getId().equals(tokenModel.getId())
                        && exportToken(model) != TOKEN_EXPORT_SUCCESS) { // Could not export instance
                    bootstrap.debug(String.format("Failed to export non-fungible instance #%d of token \"%s\"",
                            TokenUtils.convertIndexToLong(model.getIndex()),
                            name));
                }
            }
        }

        return exportToken(tokenModel);
    }

    private int exportToken(TokenModel tokenModel) {
        File file = new File(exportDir, String.format("%s%s", tokenModel.getFullId(), JSON_EXT));
        try (OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file), CHARSET)) {
            gson.toJson(tokenModel, out);
        } catch (Exception e) {
            bootstrap.log(e);
            return TOKEN_EXPORT_FAILED;
        }

        return TOKEN_EXPORT_SUCCESS;
    }

    public int importTokens() {
        if (!importDir.exists() || !importDir.isDirectory())
            return TOKEN_IMPORT_FAILED;

        File[] files = importDir.listFiles();
        if (files == null)
            return TOKEN_IMPORT_FAILED;
        else if (files.length == 0)
            return TOKEN_IMPORT_EMPTY;

        int tokenCount   = 0;
        int importCount = 0;
        for (File file : files) {
            if (file.isDirectory() || !file.getName().endsWith(JSON_EXT))
                continue;

            String filename = file.getName()
                    .replace(JSON_EXT, "")
                    .toLowerCase();
            if (!TokenUtils.isValidFullId(filename) && !TokenUtils.isValidId(filename))
                continue;

            tokenCount++;

            int status = importToken(file);
            if (status == TOKEN_CREATE_SUCCESS || status == TOKEN_UPDATE_SUCCESS) {
                importCount++;
            } else {
                String msg;
                switch (status) {
                    case TOKEN_CREATE_FAILED:
                        msg = "failed to save the token";
                        break;
                    case TOKEN_UPDATE_FAILED:
                        msg = "failed to update the token";
                        break;
                    case TOKEN_DUPLICATENICKNAME:
                        msg = "the alternate id already exists for another token";
                        break;
                    case TOKEN_INVALIDNICKNAME:
                        msg = "the token does not have a valid nickname";
                        break;
                    case TOKEN_INVALIDDATA:
                        msg = "the token's data is invalid";
                        break;
                    case TOKEN_CREATE_FAILEDNFTBASE:
                        msg = "failed to create the non-fungible base token";
                        break;
                    default:
                        msg = String.format("unhandled result (status: %d)", status);
                        break;
                }

                bootstrap.debug(String.format("Could not import token from file: \"%s\", reason given: \"%s\"", filename, msg));
            }
        }

        if (importCount > 0) {
            bootstrap.getPlayerManager()
                    .getPlayers()
                    .values()
                    .forEach(EnjPlayer::validateInventory);

            if (importCount == tokenCount)
                return TOKEN_IMPORT_SUCCESS;
            else
                return TOKEN_IMPORT_PARTIAL;
        } else if (tokenCount > 0) {
            return TOKEN_IMPORT_FAILED;
        } else {
            return TOKEN_IMPORT_EMPTY;
        }
    }

    private int importToken(File file) {
        try (InputStreamReader in = new InputStreamReader(new FileInputStream(file), CHARSET)) {
            TokenModel tokenModel = gson.fromJson(in, TokenModel.class);
            tokenModel.load();

            String filename = file.getName()
                    .replace(JSON_EXT, "")
                    .toLowerCase();
            String fullId = TokenUtils.isValidFullId(filename)
                    ? filename
                    : TokenUtils.createFullId(filename);
            if (!fullId.equals(tokenModel.getFullId()))
                return TOKEN_INVALIDDATA;

            // Ensures non-fungible tokens follow necessary attributes of the base model
            TokenModel baseModel = tokenModel.isNonFungibleInstance()
                    ? getToken(tokenModel.getId())
                    : null;
            if (baseModel != null) {
                if (baseModel.isNonfungible() != tokenModel.isNonfungible())
                    return TOKEN_INVALIDDATA;

                tokenModel.setAlternateId(baseModel.getAlternateId());
                tokenModel.setWalletViewState(baseModel.getWalletViewState());
            }

            TokenModel other = tokenModels.get(tokenModel.getFullId());
            if (other == null)
                return saveToken(tokenModel);

            int status = updateTokenConf(tokenModel);
            if (status == TOKEN_UPDATE_SUCCESS) {
                Database db = bootstrap.db();
                String id    = tokenModel.getId();
                String index = tokenModel.getIndex();

                permGraph.removeToken(other);
                for (TokenPermission permission : other.getAssignablePermissions()) {
                    db.deletePermission(id, index, permission);
                    permission.getWorlds().forEach(world -> removePermissionFromPlayers(permission.getPermission(), world));
                }

                permGraph.addToken(tokenModel);
                for (TokenPermission permission : tokenModel.getAssignablePermissions()) {
                    db.addPermission(id, index, permission);
                    permission.getWorlds().forEach(world -> addPermissionToPlayers(permission.getPermission(),
                                                                                   tokenModel.getFullId(),
                                                                                   world));
                }

                if (tokenModel.isBaseModel() && tokenModel.isNonfungible())
                    updateNonfungibleInstances(tokenModel);
            }

            return status;
        } catch (Exception e) {
            bootstrap.log(e);
            return TOKEN_IMPORT_FAILED;
        }
    }

    private boolean isValidToken(TokenModel tokenModel) {
        String alternateId = tokenModel == null
                ? null
                : tokenModel.getAlternateId();

        if (tokenModel == null
                || !tokenModel.isValid()
                || (alternateId != null && !isValidAlternateId(alternateId))
                || (!tokenModel.isNonfungible()) && !tokenModel.isBaseModel()) {
            return false;
        } else if (tokenModel.isNonfungible()) {
            TokenModel baseModel = getToken(tokenModel.getId());
            if (baseModel == null || tokenModel.isBaseModel())
                return true;

            return baseModel.isNonfungible()
                    && (alternateId == null || alternateId.equals(baseModel.getAlternateId()))
                    && tokenModel.getWalletViewState() == baseModel.getWalletViewState();
        }

        return true;
    }

    public static boolean isValidAlternateId(String alternateId) {
        return !(TokenUtils.isValidFullId(alternateId, true)
                || TokenUtils.isValidId(alternateId, true));
    }

}
