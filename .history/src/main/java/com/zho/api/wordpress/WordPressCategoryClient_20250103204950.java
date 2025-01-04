package com.zho.api.wordpress;

import java.io.IOException;
import org.apache.hc.core5.http.ParseException;
import org.json.JSONObject;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.io.entity.StringEntity;
import java.net.URI;
import org.json.JSONArray;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import java.nio.charset.StandardCharsets;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import java.util.ArrayList;
import java.util.List;

public class WordPressCategoryClient extends BaseWordPressClient {
    public WordPressCategoryClient() {
        super();
    }

    public void createCategory(String title) throws IOException, ParseException {
        String url = baseUrl + "categories";
        JSONObject categoryData = new JSONObject();
        categoryData.put("name", title);
        
        HttpPost request = new HttpPost(URI.create(url));
        setAuthHeader(request);
        request.setEntity(new StringEntity(categoryData.toString(), StandardCharsets.UTF_8));
        request.setHeader("Content-Type", "application/json");
        
        executeRequest(request);
    }
    
    public void clearCategories() throws IOException {
        String url = baseUrl + "categories";
        HttpGet request = new HttpGet(URI.create(url));
        setAuthHeader(request);
        String response = executeRequest(request);
        
        JSONArray categories = new JSONArray(response);
        
        for (int i = 0; i < categories.length(); i++) {
            JSONObject category = categories.getJSONObject(i);
            int id = category.getInt("id");
            if (id != 1) {
                deleteCategory(id);
            }
        }
    }
    
    private void deleteCategory(int categoryId) throws IOException {
        String url = baseUrl + "categories/" + categoryId + "?force=true";
        HttpDelete request = new HttpDelete(URI.create(url));
        setAuthHeader(request);
        executeRequest(request);
    }
    
    public int getCategoryId(String categoryName) throws IOException {
        String encodedName = java.net.URLEncoder.encode(categoryName, StandardCharsets.UTF_8.toString());
        String url = baseUrl + "categories?search=" + encodedName;
        HttpGet request = new HttpGet(URI.create(url));
        setAuthHeader(request);
        
        String response = executeRequest(request);
        JSONArray categories = new JSONArray(response);
        
        if (categories.length() > 0) {
            JSONObject category = categories.getJSONObject(0);
            return category.getInt("id");
        }
        
        return -1;
    }

    public List<String> getAllCategories() throws IOException, ParseException {
        String url = baseUrl + "categories";
        HttpGet request = new HttpGet(URI.create(url));
        setAuthHeader(request);
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONArray categories = new JSONArray(responseBody);
            
            List<String> categoryNames = new ArrayList<>();
            for (int i = 0; i < categories.length(); i++) {
                JSONObject category = categories.getJSONObject(i);
                categoryNames.add(category.getString("name"));
            }
            return categoryNames;
        }
    }

    public static void main(String[] args) {
        try {
            WordPressCategoryClient client = new WordPressCategoryClient();
            
            // Test clearing categories
            System.out.println("Clearing existing categories...");
            client.clearCategories();
            
            // Test creating new categories
            String[] testCategories = {
                "Test Category 1",
                "Test Category 2",
                "Test Category 3"
            };
            
            System.out.println("\nCreating new categories...");
            for (String category : testCategories) {
                client.createCategory(category);
                
                // Test getting category ID
                int categoryId = client.getCategoryId(category);
                System.out.println("Created category '" + category + "' with ID: " + categoryId);
            }
            
            System.out.println("\nAll category operations completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Error during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 