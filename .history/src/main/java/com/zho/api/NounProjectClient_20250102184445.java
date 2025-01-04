package com.zho.api;

import com.zho.config.ConfigManager;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import org.json.JSONObject;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class NounProjectClient {
    private final OAuthConsumer consumer;
    private static final String API_URL = "https://api.thenounproject.com/v2";

    public NounProjectClient() {
        ConfigManager config = ConfigManager.getInstance();
        this.consumer = new DefaultOAuthConsumer(
            config.getNounProjectKey(),
            config.getNounProjectSecret()
        );
    }

    public JSONObject searchIcons(String query) throws IOException {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
        String searchUrl = String.format("%s/icons?query=%s&limit=10", API_URL, encodedQuery);
        
        try {
            URL url = new URL(searchUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Sign the request with OAuth
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
            return new JSONObject(response.toString());
            
        } catch (Exception e) {
            throw new IOException("Failed to search icons: " + e.getMessage(), e);
        }
    }

    public byte[] downloadIcon(String iconUrl) throws IOException {
        try {
            URL url = new URL(iconUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Sign the request
            consumer.sign(connection);
            
            // Download and tint the image
            BufferedImage originalIcon = ImageIO.read(connection.getInputStream());
            BufferedImage tintedIcon = tintIcon(originalIcon);
            
            // Convert to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(tintedIcon, "PNG", baos);
            return baos.toByteArray();
            
        } catch (Exception e) {
            throw new IOException("Failed to download icon: " + e.getMessage(), e);
        }
    }

    private BufferedImage tintIcon(BufferedImage original) {
        BufferedImage tinted = new BufferedImage(
            original.getWidth(), 
            original.getHeight(), 
            BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g2d = tinted.createGraphics();
        
        // Draw original image
        g2d.drawImage(original, 0, 0, null);
        
        // Apply teal color overlay (#049F82)
        g2d.setColor(new Color(0x04, 0x9F, 0x82));
        g2d.setComposite(AlphaComposite.SrcAtop);
        g2d.fillRect(0, 0, original.getWidth(), original.getHeight());
        
        g2d.dispose();
        return tinted;
    }

    public static void main(String[] args) {
        try {
            NounProjectClient client = new NounProjectClient();
            
            // Test icon search
            System.out.println("Testing icon search...");
            String searchQuery = "tennis";
            JSONObject searchResult = client.searchIcons(searchQuery);
            System.out.println("Search results for '" + searchQuery + "':");
            System.out.println(searchResult.toString(2));
            
            // Test icon download (if search returned results)
            if (searchResult.has("icons") && searchResult.getJSONArray("icons").length() > 0) {
                String iconUrl = searchResult.getJSONArray("icons")
                    .getJSONObject(0)
                    .getString("preview_url");
                
                System.out.println("\nTesting icon download...");
                System.out.println("Downloading icon from: " + iconUrl);
                byte[] iconData = client.downloadIcon(iconUrl);
                System.out.println("Successfully downloaded icon: " + iconData.length + " bytes");
            }
            
            System.out.println("\nAll tests completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Error during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 