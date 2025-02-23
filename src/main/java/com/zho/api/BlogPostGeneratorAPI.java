package com.zho.api;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.json.JSONObject;

public class BlogPostGeneratorAPI {
    private final String API_URL = "https://aicontentwriter.onrender.com/generate?keyword=";

    public String generatePost(String keyword) throws Exception {
        // URL encode the keyword
        String encodedKeyword = java.net.URLEncoder.encode(keyword, StandardCharsets.UTF_8.toString());
        URL url = new URL(API_URL + encodedKeyword);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("X-API-Key", "a2cfd05cb84440fea9aceacbc0efcca2");
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