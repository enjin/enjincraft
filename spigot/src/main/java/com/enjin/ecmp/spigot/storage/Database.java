package com.enjin.ecmp.spigot.storage;

import org.bukkit.plugin.Plugin;

import java.io.*;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.stream.Collectors;

public class Database {

    public static final String URL_FORMAT = "jdbc:sqlite:%s";

    private File database;
    private Connection conn;

    public Database(Plugin plugin) throws SQLException, IOException {
        this.database = new File(plugin.getDataFolder(), "ecmp.db");

        InputStream is = plugin.getResource("db/setup.sql");
        if (is == null)
            throw new RuntimeException("Could not load setup.sql resource.");

        InputStreamReader isr = new InputStreamReader(is, Charset.forName("UTF-8"));
        BufferedReader br = new BufferedReader(isr);

        String setup = br.lines().collect(Collectors.joining(System.lineSeparator()));

        this.conn = DriverManager.getConnection(String.format(URL_FORMAT, this.database.getCanonicalPath()));

        PreparedStatement statement = conn.prepareStatement(setup);
        statement.execute();
    }

}
