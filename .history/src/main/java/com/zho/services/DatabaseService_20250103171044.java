package com.zho.services;

import com.zho.config.ConfigManager;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.DriverManager;

public class DatabaseService {


    public void initializeDatabase() {
        clearTopics();
    }

    //

    public void clearTopics() throws SQLException {
        String sql = "DELETE FROM topics";
        try (Connection conn = DriverManager.getConnection(
                ConfigManager.getInstance().getProperty("database.url"),
                ConfigManager.getInstance().getProperty("database.user"),
                ConfigManager.getInstance().getProperty("database.password"));
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("All topics cleared from database");
        } catch (SQLException e) {
            System.err.println("Error clearing topics: " + e.getMessage());
            throw e;
        }
    }
} 