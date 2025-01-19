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
        try (InputStream input = ConfigManager.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new RuntimeException("Unable to find application.properties");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Error loading configuration", e);
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
        return envVar != null ? envVar : properties.getProperty("googleads.client.id");
    }

    public static String getGoogleAdsClientSecret() {
        String envVar = System.getenv("GOOGLE_ADS_CLIENT_SECRET");
        return envVar != null ? envVar : properties.getProperty("googleads.client.secret");
    }

    public static String getGoogleAdsDeveloperToken() {
        String envVar = System.getenv("GOOGLE_ADS_DEV_TOKEN");
        return envVar != null ? envVar : properties.getProperty("googleads.developer.token");
    }

    public static String getGoogleAdsLoginCustomerId() {
        String envVar = System.getenv("GOOGLE_ADS_CUSTOMER_ID");
        return envVar != null ? envVar : properties.getProperty("googleads.login.customer.id");
    }

    public static String getGoogleAdsRefreshToken() {
        String envVar = System.getenv("GOOGLE_ADS_REFRESH_TOKEN");
        return envVar != null ? envVar : properties.getProperty("googleads.refresh.token");
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
        System.out.println("Base URL: " + properties.getProperty("wordpress.base.url"));
        System.out.println("Username exists: " + (properties.getProperty("wordpress.username") != null));
        System.out.println("Password exists: " + (properties.getProperty("wordpress.password") != null));
        
        // Database
        System.out.println("\nDatabase Configuration:");
        System.out.println("URL: " + properties.getProperty("database.url"));
        System.out.println("User exists: " + (properties.getProperty("database.user") != null));
        System.out.println("Password exists: " + (properties.getProperty("database.password") != null));
        
        // OpenAI
        System.out.println("\nOpenAI Configuration:");
        System.out.println("API Key exists: " + (properties.getProperty("openai.api.key") != null));
        System.out.println("API URL: " + properties.getProperty("openai.api.url"));
        
        // Unsplash
        System.out.println("\nUnsplash Configuration:");
        System.out.println("API URL: " + properties.getProperty("unsplash.api.url"));
        System.out.println("Access Key exists: " + (properties.getProperty("unsplash.access.key") != null));
        
        // Noun Project
        System.out.println("\nNoun Project Configuration:");
        System.out.println("API Key exists: " + (properties.getProperty("nounproject.api.key") != null));
        System.out.println("API Secret exists: " + (properties.getProperty("nounproject.api.secret") != null));
        
        // Google Ads
        System.out.println("\nGoogle Ads Configuration:");
        System.out.println("Client ID exists: " + (properties.getProperty("googleads.client.id") != null));
        System.out.println("Client Secret exists: " + (properties.getProperty("googleads.client.secret") != null));
        System.out.println("Developer Token exists: " + (properties.getProperty("googleads.developer.token") != null));
        System.out.println("Login Customer ID exists: " + (properties.getProperty("googleads.login.customer.id") != null));
        System.out.println("Refresh Token exists: " + (properties.getProperty("googleads.refresh.token") != null));
        
        System.out.println("\nAll configurations loaded successfully!");
    }
} 