package com.enjin.ecmp.spigot.storage;

import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.enums.TradeSessionStatus;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.stream.Collectors;

public class Database {

    public static final String URL_FORMAT = "jdbc:sqlite:%s";
    public static final String RESOURCE_FORMAT = "db/%s.sql";
    public static final String TEMPLATE_SETUP_DB = "SetupDatabase";
    public static final String TEMPLATE_CREATE_TRADE_SESSION = "trade/CreateSession";

    private SpigotBootstrap bootstrap;
    private File database;
    private Connection conn;

    private PreparedStatement setupSql;
    private PreparedStatement createTradeSession;

    public Database(SpigotBootstrap bootstrap) throws SQLException, IOException {
        this.bootstrap = bootstrap;
        this.database = new File(bootstrap.plugin().getDataFolder(), "ecmp.db");
        this.conn = DriverManager.getConnection(String.format(URL_FORMAT, this.database.getCanonicalPath()));

        this.setupSql = createPreparedStatement(TEMPLATE_SETUP_DB);
        this.setupSql.execute();

        this.createTradeSession = createPreparedStatement(TEMPLATE_CREATE_TRADE_SESSION);
    }

    public int createTradeSession(UUID inviterUuid,
                                  String inviterEthAddr,
                                  UUID invitedUuid,
                                  String invitedEthAddr,
                                  int createRequestId) throws SQLException {
        createTradeSession.clearParameters();
        createTradeSession.setString(1, inviterUuid.toString());
        createTradeSession.setString(2, inviterEthAddr);
        createTradeSession.setString(3, invitedUuid.toString());
        createTradeSession.setString(4, invitedEthAddr);
        createTradeSession.setInt(5, createRequestId);
        createTradeSession.setString(6, TradeSessionStatus.PENDING_CREATE.name());
        return createTradeSession.executeUpdate();
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
