package com.zho.services;

import com.zho.config.ConfigManager;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

public class DatabaseService {
    //in technical debt 

    public void initializeDatabase() throws SQLException {
        try {
            //do this for every table i have. eventually write a nice method to do this to all of them. 
            clearTopics();
        } catch (SQLException e) {
            System.err.println("Error clearing topics: " + e.getMessage());
            throw e;
        }
    }

        public List<String> getTopics() throws SQLException {
        List<String> topics = new ArrayList<>();
        String sql = "SELECT topic FROM topics";
        
        try (Connection conn = DriverManager.getConnection(
                ConfigManager.getDbUrl(),
                ConfigManager.getDbUser(),
                ConfigManager.getDbPassword());             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                topics.add(rs.getString("topic"));
            }
            
            System.out.println("Retrieved " + topics.size() + " topics from database");
            return topics;
            
        } catch (SQLException e) {
            System.err.println("Error retrieving topics: " + e.getMessage());
            throw e;
        }
    }


    public void clearTopics() throws SQLException {
        try (Connection conn = DriverManager.getConnection(
                ConfigManager.getDbUrl(),
                ConfigManager.getDbUser(),
                ConfigManager.getDbPassword());
             Statement stmt = conn.createStatement()) {
            
            // Delete all rows
            stmt.execute("DELETE FROM topics");
            // Reset the auto-increment counter
            stmt.execute("ALTER TABLE topics AUTO_INCREMENT = 1");
            
            System.out.println("All topics cleared from database and ID reset");
        } catch (SQLException e) {
            System.err.println("Error clearing topics: " + e.getMessage());
            throw e;
        }
    }


    public static void main(String[] args) {
        try {
            DatabaseService service = new DatabaseService();
            
            // Clear existing topics
            System.out.println("Clearing existing topics...");
            service.clearTopics();
                        
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 