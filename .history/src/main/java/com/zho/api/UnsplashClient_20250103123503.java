package com.zho.api;

import com.zho.config.ConfigManager;
import com.zho.model.UnsplashImage;
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

    public List<UnsplashImage> searchImages(String query, int count) throws IOException, ParseException {
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
            
            List<UnsplashImage> images = new ArrayList<>();
            for (int i = 0; i < results.length(); i++) {
                JSONObject imageJson = results.getJSONObject(i);
                String description = imageJson.isNull("description") ? "" : imageJson.getString("description");
                String id = imageJson.getString("id");
                String imageUrl = imageJson.getJSONObject("urls").getString("regular");
                
                System.out.println("Image " + (i+1) + ": " + id + " - " + description);
                
                images.add(new UnsplashImage(id, imageUrl, description));
            }
            
            return images;
        }
    }

    public List<UnsplashImage> searchImages(String query) throws IOException, ParseException {
        return searchImages(query, 10);
    }
} 