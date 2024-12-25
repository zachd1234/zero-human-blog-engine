package com.zho.images;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.hc.core5.http.ParseException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class UnsplashClient {
    private static final String API_URL = "https://api.unsplash.com";
    private static final String ACCESS_KEY = "KM2z5ArvljzzRCqYyNIUGYcNms2tkvgp4MNDqO2r-R8";
    private final CloseableHttpClient httpClient;

    public UnsplashClient() {
        this.httpClient = HttpClients.createDefault();
    }

    public List<UnsplashImage> searchImages(String query) throws IOException, ParseException {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = String.format("%s/search/photos?query=%s&per_page=10", API_URL, encodedQuery);
        
        HttpGet request = new HttpGet(URI.create(url));
        request.setHeader("Authorization", "Client-ID " + ACCESS_KEY);
        request.setHeader("Accept-Version", "v1");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity);
            
            JSONObject jsonResponse = new JSONObject(responseBody);
            JSONArray results = jsonResponse.getJSONArray("results");
            
            List<UnsplashImage> images = new ArrayList<>();
            for (int i = 0; i < results.length(); i++) {
                JSONObject imageJson = results.getJSONObject(i);
                String description = imageJson.isNull("description") ? "" : imageJson.getString("description");
                UnsplashImage image = new UnsplashImage(
                    imageJson.getString("id"),
                    imageJson.getJSONObject("urls").getString("regular"),
                    description
                );
                images.add(image);
            }
            
            return images;
        }
    }

    public static void main(String[] args) {
        try {
            UnsplashClient client = new UnsplashClient();
            List<UnsplashImage> images = client.searchImages("cat");
            
            for (UnsplashImage image : images) {
                System.out.println("URL: " + image.getUrl());
                System.out.println("Description: " + image.getDescription());
                System.out.println("---");
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}