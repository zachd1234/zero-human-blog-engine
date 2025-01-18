package com.zho.api;

import com.zho.config.ConfigManager;
import com.zho.model.Image;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.hc.core5.http.ParseException;

public class UnsplashClient {
    private final String accessKey;
    private final CloseableHttpClient httpClient;
    private static final String API_URL = "https://api.unsplash.com";

    public UnsplashClient() {
        this.accessKey = ConfigManager.getInstance().getUnsplashKey();
        this.httpClient = HttpClients.createDefault();
    }

    public List<Image> searchImages(String query, int count) throws IOException, ParseException {
        System.out.println("Searching Unsplash for: " + query + " (requesting " + count + " images)");
        
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = String.format("%s/search/photos?query=%s&per_page=%d&orientation=landscape", API_URL, encodedQuery, count);
        
        HttpGet request = new HttpGet(URI.create(url));
        request.setHeader("Authorization", "Client-ID " + accessKey);
        request.setHeader("Accept-Version", "v1");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getCode() != 200) {
                throw new IOException("Unsplash API error: " + response.getCode());
            }

            String responseBody = EntityUtils.toString(response.getEntity());
            JSONObject jsonResponse = new JSONObject(responseBody);
            JSONArray results = jsonResponse.getJSONArray("results");
            
            System.out.println("Found " + results.length() + " images");
            
            List<Image> images = new ArrayList<>();
            for (int i = 0; i < results.length(); i++) {
                JSONObject imageJson = results.getJSONObject(i);
                String description = imageJson.isNull("description") ? "" : imageJson.getString("description");
                String id = imageJson.getString("id");
                String imageUrl = imageJson.getJSONObject("urls").getString("regular");
                
                System.out.println("Image " + (i+1) + ": " + id + " - " + description);
                
                images.add(new Image(id, imageUrl, description));
            }
            
            return images;
        }
    }

    public List<Image> searchImages(String query) throws IOException, ParseException {
        return searchImages(query, 10);
    }

    public static void main(String[] args) {
        try {
            UnsplashClient client = new UnsplashClient();
            
            // Test case 1: Single image
            System.out.println("\n=== Test 1: Fetch single image ===");
            List<Image> singleImage = client.searchImages("coffee", 1);
            System.out.println("Found " + singleImage.size() + " image:");
            printImageDetails(singleImage);
            
            Thread.sleep(1000); // Be nice to the API
            
            // Test case 2: Multiple images
            System.out.println("\n=== Test 2: Fetch multiple images ===");
            List<Image> multipleImages = client.searchImages("nature landscape", 3);
            System.out.println("Found " + multipleImages.size() + " images:");
            printImageDetails(multipleImages);
            
            Thread.sleep(1000);
            
            // Test case 3: Edge case - very specific search
            System.out.println("\n=== Test 3: Specific search term ===");
            List<Image> specificImages = client.searchImages("vintage typewriter desk", 2);
            System.out.println("Found " + specificImages.size() + " images:");
            printImageDetails(specificImages);
            
        } catch (Exception e) {
            System.err.println("Error during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void printImageDetails(List<Image> images) {
        for (int i = 0; i < images.size(); i++) {
            Image image = images.get(i);
            System.out.println("Image " + (i + 1) + ":");
            System.out.println("  ID: " + image.getId());
            System.out.println("  URL: " + image.getUrl());
            System.out.println("  Description: " + image.getDescription());
            System.out.println();
        }
    }
} 