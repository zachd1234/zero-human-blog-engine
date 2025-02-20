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
    private final String API_URL = "http://localhost:8000/generate-post";

    public Map<String, Object> generatePost(String keyword) throws Exception {
        URL url = new URL("http://127.0.0.1:8000/generate-post");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);

        String jsonInputString = "{\"keyword\": \"" + keyword + "\"}";
        System.out.println("Sending request: " + jsonInputString);  // Debug line

        try (OutputStream os = con.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            System.out.println("Response code: " + con.getResponseCode());  // Debug line
            System.out.println("Response body: " + response.toString());    // Debug line
            
            JSONObject responseJson = new JSONObject(response.toString());
            return responseJson.toMap();
        }
    }

    public static void main(String[] args) {
        BlogPostGeneratorAPI api = new BlogPostGeneratorAPI();
        String keyword = "how to start rucking";
        
        try {
            Map<String, Object> result = api.generatePost(keyword);
            System.out.println("=== Generated Content for '" + keyword + "' ===");
            System.out.println("Title: " + result.get("title"));
            System.out.println("Content: " + result.get("content"));
            System.out.println("=====================================");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 