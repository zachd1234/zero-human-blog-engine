package com.zho.services;

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