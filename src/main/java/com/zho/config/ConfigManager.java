package com.zho.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigManager {
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
        return properties.getProperty("wordpress.base.url");
    }
    
    // Database
    public static String getDbUrl() {
        return properties.getProperty("database.url");
    }
    
    // OpenAI
    public static String getOpenAiKey() {
        return properties.getProperty("openai.api.key");
    }
    
    // Unsplash
    public static String getUnsplashKey() {
        return properties.getProperty("unsplash.access.key");
    }
    
    // Noun Project
    public static String getNounProjectKey() {
        return properties.getProperty("nounproject.api.key");
    }
    
    // Add more getters as needed
} 