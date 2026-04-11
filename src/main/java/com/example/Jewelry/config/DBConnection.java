package com.example.Jewelry.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {

    private static DBConnection instance;
    private static final String CONFIG_FILE = "application.properties";

    private final String url;
    private final String username;
    private final String password;

    private DBConnection() {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Khong tim thay SQL Server JDBC Driver", exception);
        }

        Properties properties = new Properties();
        try (InputStream inputStream = DBConnection.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (inputStream == null) {
                throw new IllegalStateException("Khong tim thay file cau hinh application.properties");
            }
            properties.load(inputStream);
        } catch (IOException exception) {
            throw new IllegalStateException("Khong the doc cau hinh database", exception);
        }

        this.url = properties.getProperty("db.sqlserver.url");
        this.username = properties.getProperty("db.sqlserver.username");
        this.password = properties.getProperty("db.sqlserver.password");

        if (isBlank(url) || isBlank(username) || password == null) {
            throw new IllegalStateException("Thieu cau hinh db.sqlserver.url/db.sqlserver.username/db.sqlserver.password");
        }
    }

    public static synchronized DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
