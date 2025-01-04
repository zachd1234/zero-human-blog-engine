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
                ConfigManager.getInstance().get("database.url"),
                ConfigManager.getInstance().get("database.user"),
                ConfigManager.getInstance().get("database.password"));
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