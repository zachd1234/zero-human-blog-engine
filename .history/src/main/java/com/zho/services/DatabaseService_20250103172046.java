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


    private void clearTopics() throws SQLException {
        String sql = "DELETE FROM topics";
        try (Connection conn = DriverManager.getConnection(
                ConfigManager.getDbUrl(),
                ConfigManager.getDbUser(),
                ConfigManager.getDbPassword());
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("All topics cleared from database");
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
            
            // Add test topics through JDBC
            System.out.println("\nAdding test topics...");
            try (Connection conn = DriverManager.getConnection(
                    ConfigManager.getDbUrl(),
                    ConfigManager.getDbUser(),
                    ConfigManager.getDbPassword());
                 Statement stmt = conn.createStatement()) {
                
                // Create topics table if it doesn't exist
                String createTable = "CREATE TABLE IF NOT EXISTS topics (" +
                                   "id INT AUTO_INCREMENT PRIMARY KEY, " +
                                   "topic VARCHAR(255) NOT NULL)";
                stmt.execute(createTable);
                
                // Add test topics
                String[] testTopics = {"Tennis", "Cooking", "Programming"};
                for (String topic : testTopics) {
                    String insertSql = "INSERT INTO topics (topic) VALUES ('" + topic + "')";
                    stmt.execute(insertSql);
                    System.out.println("Added topic: " + topic);
                }
            }
            
            // Get and display topics
            System.out.println("\nRetrieving topics...");
            List<String> topics = service.getTopics();
            
            System.out.println("\nCurrent topics in database:");
            for (String topic : topics) {
                System.out.println("- " + topic);
            }
            
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 