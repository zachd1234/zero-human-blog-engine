package com.zho.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigManager {
    private static ConfigManager instance;
    private static final Properties properties;

    // Initialize properties in static block
    static {
        properties = new Properties();
        try {
            System.out.println("Loading application.properties...");
            InputStream input = ConfigManager.class.getClassLoader().getResourceAsStream("application.properties");
            if (input != null) {
                properties.load(input);
                System.out.println("Properties loaded successfully");
            } else {
                System.out.println("application.properties not found in classpath");
            }
        } catch (IOException ex) {
            System.err.println("Could not load application.properties: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private ConfigManager() {
        // private constructor
    }

    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
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

    // GoDaddy API
    public static String getGoDaddyApiKey() {
        String envVar = System.getenv("GODADDY_API_KEY");
        return envVar != null ? envVar : properties.getProperty("godaddy.api.key");
    }

    public static String getGoDaddyApiSecret() {
        String envVar = System.getenv("GODADDY_API_SECRET");
        return envVar != null ? envVar : properties.getProperty("godaddy.api.secret");
    }

    public static String getGoDaddyApiUrl() {
        String envVar = System.getenv("GODADDY_API_URL");
        return envVar != null ? envVar : properties.getProperty("godaddy.api.url", "https://api.ote-godaddy.com/v1/");
    }

    // Recraft API
    public static String getRecraftApiUrl() {
        String envVar = System.getenv("RECRAFT_API_URL");
        return envVar != null ? envVar : properties.getProperty("recraft.api.url", "https://external.api.recraft.ai/v1");
    }

    public static String getRecraftApiKey() {
        String envVar = System.getenv("RECRAFT_API_KEY");
        return envVar != null ? envVar : properties.getProperty("recraft.api.key");
    }

    // Koala Writer
    public static String getKoalaWriterKey() {
        String envVar = System.getenv("KOALA_WRITER_API_KEY");
        return envVar != null ? envVar : properties.getProperty("koala.writer.api.key");
    }
    
    public static String getKoalaWriterUrl() {
        String envVar = System.getenv("KOALA_WRITER_API_URL");
        return envVar != null ? envVar : properties.getProperty("koala.writer.api.url");
    }

    public static String getGeminiKey() {
        String envVar = System.getenv("GEMINI_AI_KEY");
        return envVar != null ? envVar : properties.getProperty("gemini.api.key");
    }

    public static String getBlogPostGeneratorApiKey() {
        String envVar = System.getenv("BLOG_POST_GENERATOR_API_KEY");
        return envVar != null ? envVar : properties.getProperty("blog.generator.api.key");
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