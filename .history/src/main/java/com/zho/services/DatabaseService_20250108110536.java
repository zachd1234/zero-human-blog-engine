package com.zho.services;

import com.zho.config.ConfigManager;
import com.zho.model.Topic;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.sql.PreparedStatement;

public class DatabaseService {
    //in technical debt 

    public void initializeDatabase() throws SQLException {
        try {
            //do this for every table i have. eventually write a nice method to do this to all of them. 
            clearTopics();
            clearPersonas();
        } catch (SQLException e) {
            System.err.println("Error clearing topics: " + e.getMessage());
            throw e;
        }
    }

    public static List<Topic> getTopics() throws SQLException {
        List<Topic> topics = new ArrayList<>();
        String sql = "SELECT title, description, link FROM topics";
        
        try (Connection conn = DriverManager.getConnection(
                ConfigManager.getDbUrl(),
                ConfigManager.getDbUser(),
                ConfigManager.getDbPassword());             
                Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Topic topic = new Topic(
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getString("link")
                );
                topics.add(topic);
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

    public void insertTopic(Topic topic) throws SQLException {
        String sql = "INSERT INTO topics (title, description, link) VALUES (?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(
                ConfigManager.getDbUrl(),
                ConfigManager.getDbUser(),
                ConfigManager.getDbPassword());
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, topic.getTitle());
            pstmt.setString(2, topic.getDescription());
            pstmt.setString(3, topic.getLink());
            
            pstmt.executeUpdate();
            System.out.println("Inserted topic: " + topic.getTitle());
            
        } catch (SQLException e) {
            System.err.println("Error inserting topic: " + topic.getTitle() + " - " + e.getMessage());
            throw e;
        }
    }

    public void updatePersona(String name, String expertise, String biography, 
                            String writingTone, String systemPrompt, String imageUrl) throws SQLException {
        try (Connection conn = DriverManager.getConnection(
                ConfigManager.getDbUrl(),
                ConfigManager.getDbUser(),
                ConfigManager.getDbPassword());
             Statement stmt = conn.createStatement()) {
            
            // First, clear existing personas
            stmt.executeUpdate("DELETE FROM personas");
            
            // Then insert the new persona
            String insertSQL = "INSERT INTO personas (role, name, expertise, biography, writing_tone, system_prompt, image_url) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                pstmt.setString(1, "founder");  // Set role to "founder"
                pstmt.setString(2, name);
                pstmt.setString(3, expertise);
                pstmt.setString(4, biography);
                pstmt.setString(5, writingTone);
                pstmt.setString(6, systemPrompt);
                pstmt.setString(7, imageUrl);
                
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error updating persona in database: " + e.getMessage());
            throw e;
        }
    }

    public String getSystemPrompt() {
        try (Connection conn = DriverManager.getConnection(
                ConfigManager.getDbUrl(),
                ConfigManager.getDbUser(),
                ConfigManager.getDbPassword());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT system_prompt FROM personas WHERE role = 'founder' LIMIT 1")) {
            
            if (rs.next()) {
                return rs.getString("system_prompt");
            } else {
                throw new RuntimeException("No founder persona found in database");
            }
            
        } catch (SQLException e) {
            System.err.println("Error retrieving system prompt: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        try {
            // Clear existing topics
            DatabaseService service = new DatabaseService();
            service.initializeDatabase();
                        
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 