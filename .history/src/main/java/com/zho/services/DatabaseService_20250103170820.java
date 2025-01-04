package com.zho.services;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

public class DatabaseService {
    public void initializeDatabase() {

    }

    public void clearTopics() throws SQLException {
        String sql = "DELETE FROM topics";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("All topics cleared from database");
        }
    }

} 