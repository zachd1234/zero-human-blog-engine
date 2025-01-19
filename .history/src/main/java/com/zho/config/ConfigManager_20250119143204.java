package com.zho.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigManager {
    private static ConfigManager instance;
    
    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    private ConfigManager() {
        // private constructor for singleton
    }

    private static final Properties properties = new Properties();
    
    static {
        try {
            // Try loading from file first (for local development)
            InputStream input = ConfigManager.class.getClassLoader().getResourceAsStream("application.properties");
            if (input != null) {
                properties.load(input);
            } else {
                // Fallback to environment variables (for Lambda)
                String dbUrl = System.getenv("DB_URL");
                String dbUser = System.getenv("DB_USER");
                String dbPassword = System.getenv("DB_PASSWORD");
                
                if (dbUrl != null && dbUser != null && dbPassword != null) {
                    properties.setProperty("db.url", dbUrl);
                    properties.setProperty("db.user", dbUser);
                    properties.setProperty("db.password", dbPassword);
                } else {
                    throw new RuntimeException("No configuration found - need either application.properties or environment variables");
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException("Unable to load configuration", ex);
        }
    }
    
    // WordPress
    public static String getWpBaseUrl() {
        String envVar = System.getenv("WP_URL");
        return envVar != null ? envVar : properties.getProperty("wordpress.base.url");
    }

    public static String getWpUsername() {
        String envVar = System.getenv("WP_USERNAME");
        return envVar != null ? envVar : properties.getProperty("wordpress.username");
    }

    public static String getWpPassword() {
        String envVar = System.getenv("WP_PASSWORD");
        return envVar != null ? envVar : properties.getProperty("wordpress.password");
    }

    public static String getWpToken() {
        String envVar = System.getenv("WP_TOKEN");
        return envVar != null ? envVar : properties.getProperty("wordpress.token");
    }

    // Database
    public static String getDbUrl() {
        String envVar = System.getenv("DB_URL");
        return envVar != null ? envVar : properties.getProperty("database.url");
    }

    public static String getDbUser() {
        String envVar = System.getenv("DB_USER");
        return envVar != null ? envVar : properties.getProperty("database.user");
    }

    public static String getDbPassword() {
        String envVar = System.getenv("DB_PASSWORD");
        return envVar != null ? envVar : properties.getProperty("database.password");
    }

    // OpenAI
    public static String getOpenAiUrl() {
        String envVar = System.getenv("OPENAI_URL");
        return envVar != null ? envVar : properties.getProperty("openai.api.url");
    }

    public static String getOpenAiKey() {
        String envVar = System.getenv("OPENAI_KEY");
        return envVar != null ? envVar : properties.getProperty("openai.api.key");
    }

    // Unsplash
    public static String getUnsplashUrl() {
        String envVar = System.getenv("UNSPLASH_URL");
        return envVar != null ? envVar : properties.getProperty("unsplash.api.url");
    }

    public static String getUnsplashKey() {
        String envVar = System.getenv("UNSPLASH_KEY");
        return envVar != null ? envVar : properties.getProperty("unsplash.access.key");
    }

    // Noun Project
    public static String getNounProjectKey() {
        String envVar = System.getenv("NOUN_PROJECT_KEY");
        return envVar != null ? envVar : properties.getProperty("nounproject.api.key");
    }

    public static String getNounProjectSecret() {
        String envVar = System.getenv("NOUN_PROJECT_SECRET");
        return envVar != null ? envVar : properties.getProperty("nounproject.api.secret");
    }

    // Google Ads
    public static String getGoogleAdsClientId() {
        String envVar = System.getenv("GOOGLE_ADS_CLIENT_ID");
        return envVar != null ? envVar : properties.getProperty("api.googleads.clientId");
    }

    public static String getGoogleAdsClientSecret() {
        String envVar = System.getenv("GOOGLE_ADS_CLIENT_SECRET");
        return envVar != null ? envVar : properties.getProperty("api.googleads.clientSecret");
    }

    public static String getGoogleAdsDeveloperToken() {
        String envVar = System.getenv("GOOGLE_ADS_DEV_TOKEN");
        return envVar != null ? envVar : properties.getProperty("api.googleads.developerToken");
    }

    public static String getGoogleAdsLoginCustomerId() {
        String envVar = System.getenv("GOOGLE_ADS_CUSTOMER_ID");
        return envVar != null ? envVar : properties.getProperty("api.googleads.loginCustomerId");
    }

    public static String getGoogleAdsRefreshToken() {
        String envVar = System.getenv("GOOGLE_ADS_REFRESH_TOKEN");
        return envVar != null ? envVar : properties.getProperty("api.googleads.refreshToken");
    }

    // GetImg.ai
    public static String getGetImgApiKey() {
        String envVar = System.getenv("GETIMG_KEY");
        return envVar != null ? envVar : properties.getProperty("getimg.ai.key");
    }

    public static String getGetImgApiUrl() {
        String envVar = System.getenv("GETIMG_URL");
        return envVar != null ? envVar : properties.getProperty("getimg.ai.url");
    }

    // Add more getters as needed

    // Add this main method to test configuration loading
    public static void main(String[] args) {
        System.out.println("Testing configuration loading...\n");
        
        // WordPress
        System.out.println("WordPress Configuration:");
        System.out.println("Base URL: " + getWpBaseUrl());
        System.out.println("Username exists: " + (getWpUsername() != null));
        System.out.println("Password exists: " + (getWpPassword() != null));
        
        // Database
        System.out.println("\nDatabase Configuration:");
        System.out.println("URL: " + getDbUrl());
        System.out.println("User exists: " + (getDbUser() != null));
        System.out.println("Password exists: " + (getDbPassword() != null));
        
        // OpenAI
        System.out.println("\nOpenAI Configuration:");
        System.out.println("API Key exists: " + (getOpenAiKey() != null));
        System.out.println("API URL: " + getOpenAiUrl());
        
        // Unsplash
        System.out.println("\nUnsplash Configuration:");
        System.out.println("API URL: " + getUnsplashUrl());
        System.out.println("Access Key exists: " + (getUnsplashKey() != null));
        
        // Noun Project
        System.out.println("\nNoun Project Configuration:");
        System.out.println("API Key exists: " + (getNounProjectKey() != null));
        System.out.println("API Secret exists: " + (getNounProjectSecret() != null));
        
        // Google Ads
        System.out.println("\nGoogle Ads Configuration:");
        System.out.println("Client ID exists: " + (getGoogleAdsClientId() != null));
        System.out.println("Client Secret exists: " + (getGoogleAdsClientSecret() != null));
        System.out.println("Developer Token exists: " + (getGoogleAdsDeveloperToken() != null));
        System.out.println("Login Customer ID exists: " + (getGoogleAdsLoginCustomerId() != null));
        System.out.println("Refresh Token exists: " + (getGoogleAdsRefreshToken() != null));
        
        System.out.println("\nAll configurations loaded successfully!");
    }
} 