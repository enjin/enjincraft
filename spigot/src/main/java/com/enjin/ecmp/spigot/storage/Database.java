package com.enjin.ecmp.spigot.storage;

import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.enums.TradeStatus;

import java.io.*;
import java.nio.charset.Charset;
import java.sql.*;
import java.util.UUID;
import java.util.stream.Collectors;

public class Database {

    public static final String DB_FILE_NAME = "ecmp.db";
    public static final String URL_FORMAT = "jdbc:sqlite:%s";
    public static final String RESOURCE_FORMAT = "db/%s.sql";
    public static final String TEMPLATE_SETUP_DB = "SetupDatabase";
    public static final String TEMPLATE_CREATE_TRADE = "trade/CreateTrade";
    public static final String TEMPLATE_COMPLETE_TRADE = "trade/CompleteTrade";
    public static final String TEMPLATE_TRADE_EXECUTED = "trade/TradeExecuted";

    private SpigotBootstrap bootstrap;
    private File database;
    private Connection conn;

    private PreparedStatement setupSql;
    private PreparedStatement createTrade;
    private PreparedStatement completeTrade;
    private PreparedStatement tradeExecuted;

    public Database(SpigotBootstrap bootstrap) throws SQLException, IOException {
        this.bootstrap = bootstrap;
        this.database = new File(bootstrap.plugin().getDataFolder(), DB_FILE_NAME);
        this.conn = DriverManager.getConnection(String.format(URL_FORMAT, this.database.getCanonicalPath()));

        this.setupSql = createPreparedStatement(TEMPLATE_SETUP_DB);
        this.setupSql.execute();

        this.createTrade = createPreparedStatement(TEMPLATE_CREATE_TRADE);
        this.completeTrade = createPreparedStatement(TEMPLATE_COMPLETE_TRADE);
        this.tradeExecuted = createPreparedStatement(TEMPLATE_TRADE_EXECUTED);
    }

    public int createTrade(UUID inviterUuid,
                           String inviterEthAddr,
                           UUID invitedUuid,
                           String invitedEthAddr,
                           int createRequestId) throws SQLException {
        createTrade.clearParameters();
        createTrade.setString(1, inviterUuid.toString());
        createTrade.setString(2, inviterEthAddr);
        createTrade.setString(3, invitedUuid.toString());
        createTrade.setString(4, invitedEthAddr);
        createTrade.setInt(5, createRequestId);
        createTrade.setString(6, TradeStatus.PENDING_CREATE.name());
        int count = createTrade.executeUpdate();

        if (count > 0) {
            try (ResultSet rs = createTrade.getGeneratedKeys()) {
                return rs.getInt(1);
            }
        }

        return -1;
    }

    public int completeTrade(int createRequestId,
                             int completeRequestId,
                             String tradeId) throws SQLException {
        completeTrade.clearParameters();
        completeTrade.setInt(1, completeRequestId);
        completeTrade.setString(2, tradeId);
        completeTrade.setString(3, TradeStatus.PENDING_COMPLETE.name());
        completeTrade.setInt(4, createRequestId);
        return completeTrade.executeUpdate();
    }

    public int tradeExecuted(int completeRequestId) throws SQLException {
        tradeExecuted.clearParameters();
        tradeExecuted.setString(1, TradeStatus.EXECUTED.name());
        tradeExecuted.setInt(2, completeRequestId);
        return tradeExecuted.executeUpdate();
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
