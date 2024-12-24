package com.zho.content;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import java.io.File;
import java.util.Properties;
import java.net.HttpURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.json.JSONObject;
import org.json.JSONArray;

public class IconFetcher {
    private final OAuthConsumer consumer;

    public IconFetcher() throws IOException {
        Properties props = new Properties();
        props.load(new File("src/main/resources/noun-project.properties").toURI().toURL().openStream());
        
        String apiKey = props.getProperty("api.nounproject.key");
        String apiSecret = props.getProperty("api.nounproject.secret");
        
        this.consumer = new DefaultOAuthConsumer(apiKey, apiSecret);
    }

    public BufferedImage fetchRelatedIcon(String keyword) throws IOException {
        // URL encode the keyword to handle spaces and special characters
        String encodedKeyword = java.net.URLEncoder.encode(keyword, "UTF-8");
        
        String searchUrl = "https://api.thenounproject.com/v2/icon" + 
                         "?query=" + encodedKeyword + 
                         "&limit=1" +
                         "&thumbnail_size=200";
        
        try {
            URL url = new URL(searchUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Sign the request
            consumer.sign(connection);
            
            System.out.println("Sending request to: " + searchUrl);
            
            // Get response
            int responseCode = connection.getResponseCode();
            System.out.println("Response code: " + responseCode);
            
            // Read the JSON response
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            
            System.out.println("Response: " + response.toString());
            
            // Parse JSON to get the first icon's URL
            JSONObject json = new JSONObject(response.toString());
            JSONArray icons = json.getJSONArray("icons");
            String imageUrl = icons.getJSONObject(0).getString("thumbnail_url");
            
            // Now fetch the actual image
            return ImageIO.read(new URL(imageUrl));
            
        } catch (Exception e) {
            throw new IOException("Failed to fetch icon: " + e.getMessage(), e);
        }
    }

    public static void main(String[] args) {
        try {
            IconFetcher fetcher = new IconFetcher();
            
            // Test different search terms
            String[] searchTerms = {"coffee", "computer", "blog", "writing"};
            
            for (String term : searchTerms) {
                System.out.println("\nTesting search term: " + term);
                BufferedImage icon = fetcher.fetchRelatedIcon(term);
                
                String fileName = term + "-icon.png";
                String desktopPath = System.getProperty("user.home") + "/Desktop/" + fileName;
                ImageIO.write(icon, "png", new File(desktopPath));
                System.out.println("Saved " + fileName + " to desktop");
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 