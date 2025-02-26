package com.zho.api;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.json.JSONObject;
import com.zho.config.ConfigManager;
import com.zho.model.Site;

public class BlogPostGeneratorAPI {
    private final String API_URL = "https://aicontentwriter.onrender.com/generate?keyword=";

    public String generatePost(String keyword) throws Exception {
        // First get and clean the base URL
        Site currentSite = Site.getCurrentSite();
        String baseUrl = currentSite.getUrl().replaceAll("/wp-json/wp/v2.*", "");
        System.out.println("Current site " + baseUrl);

        // Now encode both parameters
        String encodedKeyword = java.net.URLEncoder.encode(keyword, StandardCharsets.UTF_8.toString());
        String encodedBaseUrl = java.net.URLEncoder.encode(baseUrl, StandardCharsets.UTF_8.toString());
        
        // Construct URL with both parameters
        URL url = new URL(API_URL + "?keyword=" + encodedKeyword + "&base_url=" + encodedBaseUrl);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("X-API-Key", ConfigManager.getBlogPostGeneratorApiKey());
        con.setDoOutput(true);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            
            // Parse the JSON structure
            JSONObject responseJson = new JSONObject(response.toString());
            String htmlContent = responseJson.getJSONObject("data").getString("data");
            
            // Remove the ```html\n prefix and \n``` suffix
            return htmlContent.replace("```html\n", "").replace("\n```", "");
        }
    }

    public static void main(String[] args) {
        BlogPostGeneratorAPI api = new BlogPostGeneratorAPI();
        String keyword = "how to start rucking";
        
        try {
            String content = api.generatePost(keyword);
            System.out.println("=== Generated Content for '" + keyword + "' ===");
            System.out.println(content);
            System.out.println("=====================================");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 