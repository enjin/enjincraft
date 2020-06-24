package com.enjin.enjincraft.spigot.storage;

import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.enums.TradeState;
import com.enjin.enjincraft.spigot.token.TokenModel;
import com.enjin.enjincraft.spigot.token.TokenPermission;
import com.enjin.enjincraft.spigot.trade.TradeSession;
import lombok.NonNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

public class Database {

    public static final String DB_FILE_NAME = "enjincraft.db";
    public static final String URL_FORMAT = "jdbc:sqlite:%s";
    public static final String RESOURCE_FORMAT = "db/%s.sql";

    // Setup statement paths
    public static final String SETUP_TOKEN_STATEMENT = "token/SetupToken";
    public static final String SETUP_TOKENPERMS_STATEMENT = "token/SetupTokenPermissions";
    public static final String SETUP_TRADE_STATEMENT = "trade/SetupTrade";
    @Deprecated
    public static final String TEMPLATE_SETUP_DB = "SetupDatabase";

    // Token statement paths
    public static final String TEMPLATE_CREATE_TOKEN = "token/CreateToken";
    public static final String TEMPLATE_DELETE_TOKEN = "token/DeleteToken";
    public static final String TEMPLATE_GET_TOKEN = "token/GetToken";
    public static final String TEMPLATE_GET_ALL_TOKENS = "token/GetAllTokens";
    public static final String TEMPLATE_GET_NBT = "token/CreateToken";
    public static final String TEMPLATE_UPDATE_NBT = "token/UpdateNBT";

    // Token permission statement paths
    public static final String TEMPLATE_ADD_PERMISSION = "token/AddPermission";
    public static final String TEMPLATE_DELETE_PERMISSION = "token/DeletePermission";
    public static final String TEMPLATE_GET_PERMISSIONS = "token/GetPermissions";
    public static final String TEMPLATE_GET_PERMISSIONWORLDS = "token/GetPermissionWorlds";

    // Trade statement paths
    public static final String TEMPLATE_CREATE_TRADE = "trade/CreateTrade";
    public static final String TEMPLATE_COMPLETE_TRADE = "trade/CompleteTrade";
    public static final String TEMPLATE_TRADE_EXECUTED = "trade/TradeExecuted";
    public static final String TEMPLATE_CANCEL_TRADE = "trade/CancelTrade";
    public static final String TEMPLATE_GET_PENDING_TRADES = "trade/GetPending";
    public static final String TEMPLATE_GET_SESSION_REQ_ID = "trade/GetSessionFromRequestId";

    private SpigotBootstrap bootstrap;
    private File dbFile;
    private Connection conn;

    // Token
    private final PreparedStatement createToken;
    private final PreparedStatement deleteToken;
    private final PreparedStatement getToken;
    private final PreparedStatement getAllTokens;
    private final PreparedStatement getNBT;
    private final PreparedStatement updateNBT;

    // Token permission
    private final PreparedStatement addPermission;
    private final PreparedStatement deletePermission;
    private final PreparedStatement getPermissions;
    private final PreparedStatement getPermissionWorlds;

    // Trade
    private final PreparedStatement createTrade;
    private final PreparedStatement completeTrade;
    private final PreparedStatement tradeExecuted;
    private final PreparedStatement cancelTrade;
    private final PreparedStatement getPendingTrades;
    private final PreparedStatement getSessionReqId;

    public Database(SpigotBootstrap bootstrap) throws SQLException, IOException {
        this.bootstrap = bootstrap;
        this.dbFile = new File(bootstrap.plugin().getDataFolder(), DB_FILE_NAME);
        this.conn = DriverManager.getConnection(String.format(URL_FORMAT, this.dbFile.getCanonicalPath()));

        init();

        // Token prepared statements
        this.createToken = createPreparedStatement(TEMPLATE_CREATE_TOKEN);
        this.deleteToken = createPreparedStatement(TEMPLATE_DELETE_TOKEN);
        this.getToken = createPreparedStatement(TEMPLATE_GET_TOKEN);
        this.getAllTokens = createPreparedStatement(TEMPLATE_GET_ALL_TOKENS);
        this.getNBT = createPreparedStatement(TEMPLATE_GET_NBT);
        this.updateNBT = createPreparedStatement(TEMPLATE_UPDATE_NBT);

        // Token permission prepared statements
        this.addPermission = createPreparedStatement(TEMPLATE_ADD_PERMISSION);
        this.deletePermission = createPreparedStatement(TEMPLATE_DELETE_PERMISSION);
        this.getPermissions = createPreparedStatement(TEMPLATE_GET_PERMISSIONS);
        this.getPermissionWorlds = createPreparedStatement(TEMPLATE_GET_PERMISSIONWORLDS);

        // Trade prepared statements
        this.createTrade = createPreparedStatement(TEMPLATE_CREATE_TRADE);
        this.completeTrade = createPreparedStatement(TEMPLATE_COMPLETE_TRADE);
        this.tradeExecuted = createPreparedStatement(TEMPLATE_TRADE_EXECUTED);
        this.cancelTrade = createPreparedStatement(TEMPLATE_CANCEL_TRADE);
        this.getPendingTrades = createPreparedStatement(TEMPLATE_GET_PENDING_TRADES);
        this.getSessionReqId = createPreparedStatement(TEMPLATE_GET_SESSION_REQ_ID);
    }

    private void init() throws SQLException, IOException {
        Statement setupStatement = conn.createStatement();
        setupStatement.addBatch("PRAGMA foreign_keys=ON");
        setupStatement.addBatch(loadSqlFile(SETUP_TOKEN_STATEMENT));
        setupStatement.addBatch(loadSqlFile(SETUP_TOKENPERMS_STATEMENT));
        setupStatement.addBatch(loadSqlFile(SETUP_TRADE_STATEMENT));
        setupStatement.executeBatch();
    }

    public int createToken(@NonNull TokenModel tokenModel) throws SQLException {
        String id    = tokenModel.getId();
        String index = tokenModel.getIndex();
        synchronized (createToken) {
            createToken.clearParameters();

            try {
                createToken.setString(1, id);
                createToken.setString(2, index);
                createToken.setString(3, tokenModel.getNbt());

                int result = createToken.executeUpdate();

                List<TokenPermission> permissions = tokenModel.getAssignablePermissions();
                for (TokenPermission permission : permissions) {
                    addPermission(id, index, permission.getPermission(), permission.getWorlds());
                }

                return result;
            } finally {
                try {
                    createToken.clearParameters();
                } catch (SQLException e) {
                    bootstrap.log(e);
                }
            }
        }
    }

    public int deleteToken(@NonNull String tokenId,
                           @NonNull String tokenIndex) throws SQLException {
        synchronized (deleteToken) {
            deleteToken.clearParameters();

            try {
                deleteToken.setString(1, tokenId);
                deleteToken.setString(2, tokenIndex);

                return deleteToken.executeUpdate();
            } finally {
                try {
                    deleteToken.clearParameters();
                } catch (SQLException e) {
                    bootstrap.log(e);
                }
            }
        }
    }

    public TokenModel getToken(@NonNull String tokenId,
                               @NonNull String tokenIndex) throws SQLException {
        synchronized (getToken) {
            getToken.clearParameters();

            try {
                getToken.setString(1, tokenId);
                getToken.setString(2, tokenIndex);

                try (ResultSet rs = getToken.executeQuery()) {
                    if (rs.next()) {
                        TokenModel tokenModel = new TokenModel(rs);

                        List<TokenPermission> permissions = getPermissions(tokenModel.getId(),
                                                                           tokenModel.getIndex());
                        for (TokenPermission permission : permissions) {
                            tokenModel.addPermissionToWorlds(permission.getPermission(),
                                                             permission.getWorlds());
                        }

                        return tokenModel;
                    } else {
                        return null;
                    }
                }
            } finally {
                try {
                    getToken.clearParameters();
                } catch (SQLException e) {
                    bootstrap.log(e);
                }
            }
        }
    }

    public List<TokenModel> getAllTokens() throws SQLException {
        List<TokenModel> tokens = new ArrayList<>();

        synchronized (getAllTokens) {
            try (ResultSet rs = getAllTokens.executeQuery()) {
                while (rs.next()) {
                    TokenModel tokenModel = new TokenModel(rs);

                    List<TokenPermission> permissions = getPermissions(tokenModel.getId(),
                                                                       tokenModel.getIndex());
                    for (TokenPermission permission : permissions) {
                        tokenModel.addPermissionToWorlds(permission.getPermission(),
                                                         permission.getWorlds());
                    }

                    tokens.add(tokenModel);
                }
            }
        }

        return tokens;
    }

    public String getNBT(@NonNull String tokenId,
                         @NonNull String tokenIndex) throws SQLException {
        synchronized (getNBT) {
            getNBT.clearParameters();

            try {
                getNBT.setString(1, tokenId);
                getNBT.setString(2, tokenIndex);

                try (ResultSet rs = getNBT.executeQuery()) {
                    if (rs.getFetchSize() > 1)
                        return null;

                    return rs.getString("nbt");
                }
            } finally {
                try {
                    getNBT.clearParameters();
                } catch (SQLException e) {
                    bootstrap.log(e);
                }
            }
        }
    }

    public int updateNBT(@NonNull String tokenId,
                         @NonNull String tokenIndex,
                         @NonNull String nbt) throws SQLException {
        synchronized (updateNBT) {
            updateNBT.clearParameters();

            try {
                updateNBT.setString(1, nbt);
                updateNBT.setString(2, tokenId);
                updateNBT.setString(3, tokenIndex);

                return updateNBT.executeUpdate();
            } finally {
                try {
                    updateNBT.clearParameters();
                } catch (SQLException e) {
                    bootstrap.log(e);
                }
            }
        }
    }

    public int[] addPermission(@NonNull String tokenId,
                               @NonNull String tokenIndex,
                               @NonNull String permission,
                               Collection<String> worlds) throws SQLException {
        List<Integer> resultsList = new ArrayList<>();

        synchronized (addPermission) {
            for (String world : worlds) {
                try {
                    addPermission.clearParameters();
                    addPermission.setString(1, tokenId);
                    addPermission.setString(2, tokenIndex);
                    addPermission.setString(3, permission);
                    addPermission.setString(4, world);

                    resultsList.add(addPermission.executeUpdate());
                } finally {
                    try {
                        addPermission.clearParameters();
                    } catch (SQLException e) {
                        bootstrap.log(e);
                    }
                }
            }
        }

        int[] results = new int[resultsList.size()];
        for (int i = 0; i < results.length; i++) {
            results[i] = resultsList.get(i);
        }

        return results;
    }

    public int[] deletePermission(@NonNull String tokenId,
                                  @NonNull String tokenIndex,
                                  @NonNull TokenPermission permission) throws SQLException {
        return deletePermission(tokenId, tokenIndex, permission.getPermission(), permission.getWorlds());
    }

    public int[] deletePermission(@NonNull String tokenId,
                                  @NonNull String tokenIndex,
                                  @NonNull String permission,
                                  Collection<String> worlds) throws SQLException {
        List<Integer> resultsList = new ArrayList<>();

        synchronized (deletePermission) {
            for (String world : worlds) {
                try {
                    deletePermission.clearParameters();
                    deletePermission.setString(1, tokenId);
                    deletePermission.setString(2, tokenIndex);
                    deletePermission.setString(3, permission);
                    deletePermission.setString(4, world);

                    resultsList.add(deletePermission.executeUpdate());
                } finally {
                    try {
                        deletePermission.clearParameters();
                    } catch (SQLException e) {
                        bootstrap.log(e);
                    }
                }
            }
        }

        int[] results = new int[resultsList.size()];
        for (int i = 0; i < results.length; i++) {
            results[i] = resultsList.get(i);
        }

        return results;
    }

    public List<TokenPermission> getPermissions(@NonNull String tokenId,
                                                @NonNull String tokenIndex) throws SQLException {
        Map<String, Set<String>> permissionMap = new HashMap<>();

        synchronized (getPermissions) {
            getPermissions.clearParameters();

            try {
                getPermissions.setString(1, tokenId);
                getPermissions.setString(2, tokenIndex);

                try (ResultSet rs = getPermissions.executeQuery()) {
                    while (rs.next()) {
                        String permission = rs.getString("permission");
                        String world      = rs.getString("world");
                        permissionMap.computeIfAbsent(permission, k -> new HashSet<>()).add(world);
                    }
                }
            } finally {
                try {
                    getPermissions.clearParameters();
                } catch (SQLException e) {
                    bootstrap.log(e);
                }
            }
        }

        List<TokenPermission> permissions = new ArrayList<>();
        permissionMap.forEach((permission, worlds) -> permissions.add(new TokenPermission(permission, worlds)));

        return permissions;
    }

    public Collection<String> getPermissionWorlds(@NonNull String tokenId,
                                                  @NonNull String tokenIndex,
                                                  @NonNull String permission) throws SQLException {
        Set<String> worlds = new HashSet<>();

        synchronized (getPermissionWorlds) {
            getPermissionWorlds.clearParameters();

            try {
                getPermissionWorlds.setString(1, tokenId);
                getPermissionWorlds.setString(2, tokenIndex);
                getPermissionWorlds.setString(3, permission);

                try (ResultSet rs = getPermissionWorlds.executeQuery()) {
                    while (rs.next()) {
                        worlds.add(rs.getString("world"));
                    }
                }
            } finally {
                try {
                    getPermissionWorlds.clearParameters();
                } catch (SQLException e) {
                    bootstrap.log(e);
                }
            }
        }

        return worlds;
    }

    public int createTrade(UUID inviterUuid,
                           int inviterIdentityId,
                           String inviterEthAddr,
                           UUID invitedUuid,
                           int invitedIdentityId,
                           String invitedEthAddr,
                           int createRequestId) throws SQLException {
        synchronized (createTrade) {
            createTrade.clearParameters();
            createTrade.setString(1, inviterUuid.toString());
            createTrade.setInt(2, inviterIdentityId);
            createTrade.setString(3, inviterEthAddr);
            createTrade.setString(4, invitedUuid.toString());
            createTrade.setInt(5, invitedIdentityId);
            createTrade.setString(6, invitedEthAddr);
            createTrade.setInt(7, createRequestId);
            createTrade.setString(8, TradeState.PENDING_CREATE.name());
            createTrade.setLong(9, OffsetDateTime.now(ZoneOffset.UTC).toEpochSecond());
            int count = createTrade.executeUpdate();

            if (count > 0) {
                try (ResultSet rs = createTrade.getGeneratedKeys()) {
                    return rs.getInt(1);
                }
            }
        }

        return -1;
    }

    public int completeTrade(int createRequestId,
                             int completeRequestId,
                             String tradeId) throws SQLException {
        synchronized (completeTrade) {
            completeTrade.clearParameters();
            completeTrade.setInt(1, completeRequestId);
            completeTrade.setString(2, tradeId);
            completeTrade.setString(3, TradeState.PENDING_COMPLETE.name());
            completeTrade.setInt(4, createRequestId);
            return completeTrade.executeUpdate();
        }
    }

    public int tradeExecuted(int completeRequestId) throws SQLException {
        synchronized (tradeExecuted) {
            tradeExecuted.clearParameters();
            tradeExecuted.setString(1, TradeState.EXECUTED.name());
            tradeExecuted.setInt(2, completeRequestId);
            return tradeExecuted.executeUpdate();
        }
    }

    public int cancelTrade(int requestId) throws SQLException {
        synchronized (cancelTrade) {
            cancelTrade.clearParameters();
            cancelTrade.setString(1, TradeState.CANCELED.name());
            cancelTrade.setInt(2, requestId);
            cancelTrade.setInt(3, requestId);
            return cancelTrade.executeUpdate();
        }
    }

    public List<TradeSession> getPendingTrades() throws SQLException {
        List<TradeSession> sessions = new ArrayList<>();

        synchronized (getPendingTrades) {
            try (ResultSet rs = getPendingTrades.executeQuery()) {
                while (rs.next()) {
                    sessions.add(new TradeSession(rs));
                }
            }
        }

        return sessions;
    }

    public TradeSession getSessionFromRequestId(int requestId) throws SQLException {
        TradeSession session = null;

        synchronized (getSessionReqId) {
            getSessionReqId.clearParameters();
            getSessionReqId.setInt(1, requestId);
            getSessionReqId.setInt(2, requestId);

            try (ResultSet rs = getSessionReqId.executeQuery()) {
                if (rs.next())
                    session = new TradeSession(rs);
            }
        }

        return session;
    }

    private String loadSqlFile(String template) throws IOException {
        try (InputStream is = bootstrap.plugin().getResource(String.format(RESOURCE_FORMAT, template))) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return br.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        }
    }

    private PreparedStatement createPreparedStatement(String template) throws SQLException, IOException {
        return conn.prepareStatement(loadSqlFile(template));
    }

}
