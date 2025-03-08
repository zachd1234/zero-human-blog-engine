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
import com.zho.model.Site;
import java.sql.PreparedStatement;
import com.zho.model.KeywordAnalysis;
import java.util.ServiceLoader;
import java.sql.Driver;
import com.zho.model.BlogRequest;
import com.zho.model.BlogRequest;

public class DatabaseService {
    //in technical debt 
    private final int currentSiteId;

    public DatabaseService() {
        this.currentSiteId = Site.getCurrentSite().getSiteId();
    }
    public void initializeDatabase(BlogRequest request) throws SQLException {
        try {
            clearTopics();
            clearPersonas();
            clearBlogInfo();
            
            // Add new blog info
            updateBlogInfo(request.getTopic(), request.getDescription());
            
        } catch (SQLException e) {
            System.err.println("Error clearing database tables: " + e.getMessage());
            throw e;
        }
    }

    public static List<Topic> getTopics() throws SQLException {
        List<Topic> topics = new ArrayList<>();
        String sql = "SELECT title, description, link FROM topics WHERE site_id = ?";
        
        try (Connection conn = DriverManager.getConnection(
                ConfigManager.getDbUrl(),
                ConfigManager.getDbUser(),
                ConfigManager.getDbPassword());
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Site.getCurrentSite().getSiteId());
            ResultSet rs = pstmt.executeQuery();
            
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
            
            // Delete all rows for the current site
            stmt.execute("DELETE FROM topics WHERE site_id = " + currentSiteId);
            // Reset the auto-increment counter
            stmt.execute("ALTER TABLE topics AUTO_INCREMENT = 1");
            
            System.out.println("All topics cleared from database and ID reset");
        } catch (SQLException e) {
            System.err.println("Error clearing topics: " + e.getMessage());
            throw e;
        }
    }

    public void insertTopic(Topic topic) throws SQLException {
        String sql = "INSERT INTO topics (title, description, link, site_id) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(
                ConfigManager.getDbUrl(),
                ConfigManager.getDbUser(),
                ConfigManager.getDbPassword());
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, topic.getTitle());
            pstmt.setString(2, topic.getDescription());
            pstmt.setString(3, topic.getLink());
            pstmt.setInt(4, currentSiteId);  // Set site_id
            
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
            
            // First, clear existing personas for the current site
            int deleteCount = stmt.executeUpdate("DELETE FROM personas WHERE site_id = " + currentSiteId);
            System.out.println("Deleted " + deleteCount + " existing personas");
            
            // Reset the auto-increment counter
            stmt.execute("ALTER TABLE personas AUTO_INCREMENT = 1");
            
            // Then insert the new persona
            String insertSQL = "INSERT INTO personas (role, name, expertise, biography, writing_tone, system_prompt, image_url, site_id) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                pstmt.setString(1, "founder");  // Set role to "founder"
                pstmt.setString(2, name);
                pstmt.setString(3, expertise);
                pstmt.setString(4, biography);
                pstmt.setString(5, writingTone);
                pstmt.setString(6, systemPrompt);
                pstmt.setString(7, imageUrl);
                pstmt.setInt(8, currentSiteId);  // Set site_id
                
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
             PreparedStatement pstmt = conn.prepareStatement("SELECT system_prompt FROM personas WHERE role = 'founder' AND site_id = ? LIMIT 1")) {
            
            pstmt.setInt(1, currentSiteId);  // Set site_id
            ResultSet rs = pstmt.executeQuery();
            
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
            
            // Delete all rows for the current site
            stmt.execute("DELETE FROM personas WHERE site_id = " + currentSiteId);
            // Reset the auto-increment counter
            stmt.execute("ALTER TABLE personas AUTO_INCREMENT = 1");
            
            System.out.println("All personas cleared from database and ID reset");
        } catch (SQLException e) {
            System.err.println("Error clearing personas: " + e.getMessage());
            throw e;
        }
    }

    public Persona getPersona() throws SQLException {
        String sql = "SELECT name, biography, expertise, writing_tone, system_prompt, image_url FROM personas WHERE site_id = ? LIMIT 1";
        
        try (Connection conn = DriverManager.getConnection(
                ConfigManager.getDbUrl(),
                ConfigManager.getDbUser(),
                ConfigManager.getDbPassword());
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, currentSiteId);  // Set site_id
            ResultSet rs = pstmt.executeQuery();
            
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
                status,
                site_id
            ) VALUES (?, ?, ?, ?, ?, ?, 'PENDING', ?)
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
                stmt.setInt(7, currentSiteId);  // Set site_id
                
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
    
    public static Connection getConnection() {
        try {
            System.out.println("=== DATABASE CONNECTION DEBUG START ===");
            System.out.println("1. Starting connection process...");
            
            // Load drivers using ServiceLoader
            System.out.println("2. Loading MySQL drivers via ServiceLoader...");
            ServiceLoader<Driver> drivers = ServiceLoader.load(Driver.class);
            boolean foundDriver = false;
            for (Driver driver : drivers) {
                System.out.println("   Found driver: " + driver.getClass().getName());
                foundDriver = true;
            }
            
            if (!foundDriver) {
                System.out.println("   ✗ No JDBC drivers found via ServiceLoader");
            }
            
            // Get connection details
            String url = ConfigManager.getDbUrl();
            String user = ConfigManager.getDbUser();
            String password = ConfigManager.getDbPassword();
            
            System.out.println("3. Connection details:");
            System.out.println("   URL: " + url);
            System.out.println("   User: " + user);
            System.out.println("   Password: [HIDDEN]");
            
            System.out.println("4. Attempting database connection...");
            
            // Try to connect
            Connection conn = DriverManager.getConnection(url, user, password);
            
            if (conn != null) {
                System.out.println("5. ✓ Database connection successful!");
                System.out.println("   Connected to: " + conn.getCatalog());
            } else {
                System.out.println("5. ✗ Connection object is null!");
            }
            
            return conn;
            
        } catch (SQLException e) {
            System.out.println("✗ SQL Exception occurred:");
            System.out.println("Error message: " + e.getMessage());
            System.out.println("SQL State: " + e.getSQLState());
            System.out.println("Error Code: " + e.getErrorCode());
            System.out.println("Stack trace:");
            e.printStackTrace();
            return null;
        }
    }
    
    public void clearKeywords() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Delete all rows for the current site
            stmt.execute("DELETE FROM keywords WHERE site_id = " + currentSiteId);
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
                // First, get the next pending keyword for the current site
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
                    WHERE status = 'PENDING' AND site_id = ?
                    ORDER BY id ASC
                    LIMIT 1
                    FOR UPDATE
                    """;
                
                KeywordAnalysis keyword = null;
                
                try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                    selectStmt.setInt(1, currentSiteId);  // Set site_id
                    ResultSet rs = selectStmt.executeQuery();
                    if (rs.next()) {
                        keyword = new KeywordAnalysis(
                            rs.getString("keyword"),
                            rs.getLong("monthly_searches"),
                            rs.getLong("competition_index"),
                            rs.getDouble("average_cpc"),
                            rs.getDouble("score"),
                            rs.getString("ai_analysis"),
                            rs.getInt("id")
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
                        WHERE id = ? AND site_id = ?
                        """;
                        
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setLong(1, keyword.getId());
                        updateStmt.setInt(2, currentSiteId);  // Set site_id
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
            WHERE id = ? AND site_id = ?
            """;
            
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status);
            stmt.setString(2, status);
            stmt.setLong(3, id);
            stmt.setInt(4, currentSiteId);  // Set site_id
            
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
            WHERE id = ? AND site_id = ?
            """;
            
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, postUrl);
            stmt.setLong(2, id);
            stmt.setInt(3, currentSiteId);  // Set site_id
            
            int updatedRows = stmt.executeUpdate();
            if (updatedRows == 0) {
                throw new RuntimeException("No keyword found with id: " + id);
            }
            
        } catch (SQLException e) {
            System.err.println("Error updating keyword post URL: " + e.getMessage());
            throw new RuntimeException("Database error while updating keyword post URL", e);
        }
    }

    public static int loadCurrentSiteFromDatabase() {
        String sql = "SELECT site_id FROM current_site LIMIT 1";  // Adjust this query as needed
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                int siteId = rs.getInt("site_id");
                return siteId; 
            } else {
                throw new RuntimeException("No current site found in database");
            }
        } catch (SQLException e) {
            System.err.println("Error loading current site from database: " + e.getMessage());
            throw new RuntimeException("Database error while loading current site", e);
        }
    }


    public static void updateCurrentSiteInDatabase(int siteId) {
        String sql = "UPDATE current_site SET site_id = ?";  // Adjust this query as needed
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, siteId);
            int updatedRows = pstmt.executeUpdate();
            
            if (updatedRows == 0) {
                throw new RuntimeException("No current site record found to update");
            }
        } catch (SQLException e) {
            System.err.println("Error updating current site in database: " + e.getMessage());
            throw new RuntimeException("Database error while updating current site", e);
        }
    }

    public void updateBlogInfo(String topic, String description) throws SQLException {
        System.out.println("\n📝 Updating blog info...");
        System.out.println("Topic: " + topic);
        System.out.println("Description: " + description);
        
        try (Connection conn = getConnection()) {
            // First clear existing blog info for this site
            String clearSql = "DELETE FROM blog_info WHERE site_id = ?";
            try (PreparedStatement clearStmt = conn.prepareStatement(clearSql)) {
                clearStmt.setInt(1, currentSiteId);
                clearStmt.executeUpdate();
                System.out.println("✅ Cleared existing blog info");
            }
            
            // Get the URL from the current site and strip the WordPress API path if present
            String url = Site.getCurrentSite().getUrl();
            if (url.contains("/wp-json/wp/v2/")) {
                url = url.substring(0, url.indexOf("/wp-json/wp/v2/"));
            }
            
            // Then insert new blog info
            String insertSql = "INSERT INTO blog_info (site_id, topic, description, url, blog_name) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setInt(1, currentSiteId);
                insertStmt.setString(2, topic);
                insertStmt.setString(3, description);
                insertStmt.setString(4, url);
                insertStmt.setString(5, ""); // Leave blog_name blank for now
                insertStmt.executeUpdate();
                System.out.println("✅ Inserted new blog info with URL: " + url);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error updating blog info: " + e.getMessage());
            throw e;
        }
    }

    public void clearBlogInfo() throws SQLException {
        System.out.println("\n🗑️ Clearing blog info...");
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM blog_info WHERE site_id = ?")) {
            stmt.setInt(1, currentSiteId);
            int rowsDeleted = stmt.executeUpdate();
            System.out.println("✅ Cleared " + rowsDeleted + " blog info entries");
        } catch (SQLException e) {
            System.err.println("❌ Error clearing blog info: " + e.getMessage());
            throw e;
        }
    }

    public BlogRequest getBlogInfo() throws SQLException {
        System.out.println("\n📚 Retrieving blog info...");
        
        String sql = "SELECT topic, description, url, blog_name FROM blog_info WHERE site_id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, currentSiteId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                BlogRequest info = new BlogRequest(
                    rs.getString("topic"),
                    rs.getString("description")
                );
                // You'll need to update the BlogRequest class to handle these new fields
                // or modify how you're using the returned data
                System.out.println("✅ Retrieved blog info for site " + currentSiteId);
                return info;
            } else {
                System.out.println("⚠️ No blog info found for site " + currentSiteId);
                return null;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error retrieving blog info: " + e.getMessage());
            throw e;
        }
    }
    
    public static void main(String[] args) {
        DatabaseService dbService = new DatabaseService();
            
        try{
            dbService.clearTopics();
        } catch (SQLException e) {
            System.err.println("Error clearing keywords: " + e.getMessage());
            throw new RuntimeException("Database error while clearing keywords", e);
        }
    }

    public void updateBlogName(String blogName) throws SQLException {
        System.out.println("\n📝 Updating blog name to: " + blogName);
        
        String sql = "UPDATE blog_info SET blog_name = ? WHERE site_id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, blogName);
            stmt.setInt(2, currentSiteId);
            
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("✅ Successfully updated blog name to: " + blogName);
            } else {
                System.out.println("⚠️ No blog info record found to update for site ID: " + currentSiteId);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error updating blog name: " + e.getMessage());
            throw e;
        }
    }
} 