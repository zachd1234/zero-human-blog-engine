package com.zho.database;

import com.zho.model.Topic;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TopicDAO {
    
    public void createTopicsTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS topics (
                id INT AUTO_INCREMENT PRIMARY KEY,
                title VARCHAR(255) NOT NULL,
                description TEXT,
                link VARCHAR(255)
            )
        """;
        
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Topics table created successfully");
        }
    }
    
    public void insertTopic(Topic topic) throws SQLException {
        String sql = "INSERT INTO topics (title, description, link) VALUES (?, ?, ?)";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, topic.getTitle());
            pstmt.setString(2, topic.getDescription());
            pstmt.setString(3, topic.getLink());
            pstmt.executeUpdate();
        }
    }
    
    public List<Topic> getAllTopics() throws SQLException {
        List<Topic> topics = new ArrayList<>();
        String sql = "SELECT * FROM topics";
        
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                topics.add(new Topic(
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getString("link")
                ));
            }
        }
        return topics;
    }

    public void clearTopics() throws SQLException {
        String sql = "DELETE FROM topics";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("All topics cleared from database");
        }
    }

    public int getTopicCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM topics";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }
} 