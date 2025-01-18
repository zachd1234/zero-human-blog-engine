package com.zho.services;

import com.zho.config.ConfigManager;
import com.zho.model.Topic;
import com.zho.model.Persona;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.sql.PreparedStatement;
import com.zho.model.KeywordAnalysis;

public class DatabaseService {
    //in technical debt 

    public void initializeDatabase() throws SQLException {
        try {
            clearTopics();
            clearPersonas();
        } catch (SQLException e) {
            System.err.println("Error clearing database tables: " + e.getMessage());
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
        System.out.println("Attempting to update persona in database with:");
        System.out.println("Name: " + name);
        System.out.println("Expertise: " + expertise);
        System.out.println("Biography: " + biography);
        System.out.println("Writing Tone: " + writingTone);
        System.out.println("System Prompt: " + systemPrompt);
        System.out.println("Image URL: " + imageUrl);
        
        try (Connection conn = DriverManager.getConnection(
                ConfigManager.getDbUrl(),
                ConfigManager.getDbUser(),
                ConfigManager.getDbPassword());
             Statement stmt = conn.createStatement()) {
            
            // First, clear existing personas
            int deleteCount = stmt.executeUpdate("DELETE FROM personas");
            System.out.println("Deleted " + deleteCount + " existing personas");
            
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
                
                int insertCount = pstmt.executeUpdate();
                System.out.println("Inserted " + insertCount + " new persona");
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

    public void clearPersonas() throws SQLException {
        try (Connection conn = DriverManager.getConnection(
                ConfigManager.getDbUrl(),
                ConfigManager.getDbUser(),
                ConfigManager.getDbPassword());
             Statement stmt = conn.createStatement()) {
            
            // Delete all rows
            stmt.execute("DELETE FROM personas");
            // Reset the auto-increment counter
            stmt.execute("ALTER TABLE personas AUTO_INCREMENT = 1");
            
            System.out.println("All personas cleared from database and ID reset");
        } catch (SQLException e) {
            System.err.println("Error clearing personas: " + e.getMessage());
            throw e;
        }
    }

    public Persona getPersona() throws SQLException {
        String sql = "SELECT name, biography, expertise, writing_tone, system_prompt, image_url FROM personas LIMIT 1";
        
        try (Connection conn = DriverManager.getConnection(
                ConfigManager.getDbUrl(),
                ConfigManager.getDbUser(),
                ConfigManager.getDbPassword());
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                return new Persona(
                    rs.getString("name"),
                    rs.getString("biography"),
                    rs.getString("expertise"),
                    rs.getString("writing_tone"),
                    rs.getString("system_prompt"),
                    rs.getString("image_url")
                );
            }
            return null;
        }
    }

    public void saveKeywords(List<KeywordAnalysis> keywords) {
        String sql = """
            INSERT INTO keywords (
                keyword, 
                monthly_searches, 
                competition_index, 
                average_cpc, 
                score, 
                ai_analysis, 
                status
            ) VALUES (?, ?, ?, ?, ?, ?, 'PENDING')
            """;
            
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            int batchCount = 0;
            for (KeywordAnalysis kw : keywords) {
                stmt.setString(1, kw.getKeyword());
                stmt.setLong(2, kw.getMonthlySearches());
                stmt.setLong(3, kw.getCompetitionIndex());
                stmt.setDouble(4, kw.getAverageCpc());
                stmt.setDouble(5, kw.getScore());
                stmt.setString(6, kw.getAiAnalysis());
                
                stmt.addBatch();
                batchCount++;
                
                // Execute in batches of 100
                if (batchCount % 100 == 0) {
                    stmt.executeBatch();
                    System.out.println("Saved " + batchCount + " keywords");
                }
            }
            
            // Execute any remaining keywords
            if (batchCount % 100 != 0) {
                stmt.executeBatch();
                System.out.println("Saved all " + batchCount + " keywords");
            }
            
        } catch (SQLException e) {
            System.err.println("Error saving keywords to database: " + e.getMessage());
            throw new RuntimeException("Database error while saving keywords", e);
        }
    }
    
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            ConfigManager.getDbUrl(),
            ConfigManager.getDbUser(),
            ConfigManager.getDbPassword()
        );
    }

    public void clearKeywords() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Delete all rows
            stmt.execute("DELETE FROM keywords");
            // Reset the auto-increment counter
            stmt.execute("ALTER TABLE keywords AUTO_INCREMENT = 1");
            
            System.out.println("All keywords cleared from database and ID reset");
        } catch (SQLException e) {
            System.err.println("Error clearing keywords: " + e.getMessage());
            throw e;
        }
    }

    public KeywordAnalysis popNextKeyword() {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);  // Start transaction
            
            try {
                // First, get the next pending keyword
                String selectSql = """
                    SELECT 
                        id,
                        keyword, 
                        monthly_searches, 
                        competition_index, 
                        average_cpc, 
                        score, 
                        ai_analysis
                    FROM keywords 
                    WHERE status = 'PENDING'
                    ORDER BY id ASC
                    LIMIT 1
                    FOR UPDATE
                    """;
                
                KeywordAnalysis keyword = null;
                
                try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                    ResultSet rs = selectStmt.executeQuery();
                    if (rs.next()) {
                        keyword = new KeywordAnalysis(
                            rs.getLong("id"),
                            rs.getString("keyword"),
                            rs.getLong("monthly_searches"),
                            rs.getLong("competition_index"),
                            rs.getDouble("average_cpc"),
                            rs.getDouble("score"),
                            rs.getString("ai_analysis")
                        );
                    }
                }
                
                // If we found a keyword, update its status
                if (keyword != null) {
                    String updateSql = """
                        UPDATE keywords 
                        SET 
                            status = 'PROCESSING',
                            processing_started_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """;
                        
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setLong(1, keyword.getId());
                        updateStmt.executeUpdate();
                    }
                }
                
                conn.commit();
                return keyword;
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Error popping next keyword: " + e.getMessage());
            throw new RuntimeException("Database error while popping next keyword", e);
        }
    }

    public void updateKeywordStatus(Long id, String status) {
        String sql = """
            UPDATE keywords 
            SET 
                status = ?,
                published_at = CASE WHEN ? = 'PUBLISHED' THEN CURRENT_TIMESTAMP ELSE published_at END
            WHERE id = ?
            """;
            
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status);
            stmt.setString(2, status);
            stmt.setLong(3, id);
            
            int updatedRows = stmt.executeUpdate();
            if (updatedRows == 0) {
                throw new RuntimeException("No keyword found with id: " + id);
            }
            
        } catch (SQLException e) {
            System.err.println("Error updating keyword status: " + e.getMessage());
            throw new RuntimeException("Database error while updating keyword status", e);
        }
    }
    
    public void updateKeywordPostUrl(Long id, String postUrl) {
        String sql = """
            UPDATE keywords 
            SET post_url = ?
            WHERE id = ?
            """;
            
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, postUrl);
            stmt.setLong(2, id);
            
            int updatedRows = stmt.executeUpdate();
            if (updatedRows == 0) {
                throw new RuntimeException("No keyword found with id: " + id);
            }
            
        } catch (SQLException e) {
            System.err.println("Error updating keyword post URL: " + e.getMessage());
            throw new RuntimeException("Database error while updating keyword post URL", e);
        }
    }

    public static void main(String[] args) {
        DatabaseService dbService = new DatabaseService();
            
        // First clear the table
        System.out.println("Clearing existing keywords...");
        dbService.popNextKeyword();
    }
} 