package com.enjin.enjincraft.spigot.storage;

import com.enjin.enjincraft.spigot.SpigotBootstrap;
import com.enjin.enjincraft.spigot.enums.TradeState;
import com.enjin.enjincraft.spigot.trade.TradeSession;

import java.io.*;
import java.nio.charset.Charset;
import java.sql.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

public class Database {

    public static final String DB_FILE_NAME = "enjincraft.db";
    public static final String URL_FORMAT = "jdbc:sqlite:%s";
    public static final String RESOURCE_FORMAT = "db/%s.sql";
    public static final String TEMPLATE_SETUP_DB = "SetupDatabase";
    public static final String TEMPLATE_CREATE_TRADE = "trade/CreateTrade";
    public static final String TEMPLATE_COMPLETE_TRADE = "trade/CompleteTrade";
    public static final String TEMPLATE_TRADE_EXECUTED = "trade/TradeExecuted";
    public static final String TEMPLATE_CANCEL_TRADE = "trade/CancelTrade";
    public static final String TEMPLATE_GET_PENDING_TRADES = "trade/GetPending";
    public static final String TEMPLATE_GET_SESSION_REQ_ID = "trade/GetSessionFromRequestId";

    private SpigotBootstrap bootstrap;
    private File database;
    private Connection conn;

    private PreparedStatement setupSql;
    private PreparedStatement createTrade;
    private PreparedStatement completeTrade;
    private PreparedStatement tradeExecuted;
    private PreparedStatement cancelTrade;
    private PreparedStatement getPendingTrades;
    private PreparedStatement getSessionReqId;

    public Database(SpigotBootstrap bootstrap) throws SQLException, IOException {
        this.bootstrap = bootstrap;
        this.database = new File(bootstrap.plugin().getDataFolder(), DB_FILE_NAME);
        this.conn = DriverManager.getConnection(String.format(URL_FORMAT, this.database.getCanonicalPath()));

        this.setupSql = createPreparedStatement(TEMPLATE_SETUP_DB);
        this.setupSql.execute();

        this.createTrade = createPreparedStatement(TEMPLATE_CREATE_TRADE);
        this.completeTrade = createPreparedStatement(TEMPLATE_COMPLETE_TRADE);
        this.tradeExecuted = createPreparedStatement(TEMPLATE_TRADE_EXECUTED);
        this.cancelTrade = createPreparedStatement(TEMPLATE_CANCEL_TRADE);
        this.getPendingTrades = createPreparedStatement(TEMPLATE_GET_PENDING_TRADES);
        this.getSessionReqId = createPreparedStatement(TEMPLATE_GET_SESSION_REQ_ID);
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
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")))) {
                return br.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        }
    }

    private PreparedStatement createPreparedStatement(String template) throws SQLException, IOException {
        return conn.prepareStatement(loadSqlFile(template));
    }

}
