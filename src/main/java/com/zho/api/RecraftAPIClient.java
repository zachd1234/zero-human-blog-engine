package com.zho.api;

import com.zho.api.wordpress.WordPressMediaClient;
import com.zho.config.ConfigManager;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.ParseException;
import org.json.JSONObject;
import com.zho.api.wordpress.WordPressMediaClient;
import org.json.JSONArray;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;

public class RecraftAPIClient {
    private final String apiKey;
    private final String baseUrl;
    private final CloseableHttpClient httpClient;

    public RecraftAPIClient() {
        this.apiKey = ConfigManager.getRecraftApiKey();
        this.baseUrl = ConfigManager.getRecraftApiUrl();
        
        if (this.apiKey == null || this.apiKey.isEmpty()) {
            throw new IllegalStateException("Recraft API key not found in configuration");
        }
        
        this.httpClient = HttpClients.createDefault();
    }

    public String generateImage(String prompt, String style) throws IOException, ParseException {
        String url = baseUrl + "/images/generations";
        
        // Create JSON request body
        JSONObject requestBody = new JSONObject();
        requestBody.put("prompt", prompt);
        requestBody.put("style", style);
        requestBody.put("model", "recraftv3");
        requestBody.put("size", "1536x1024"); // 3:2 ratio
        
        // Create HTTP POST request
        HttpPost request = new HttpPost(url);
        request.setHeader("Authorization", "Bearer " + apiKey);
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(requestBody.toString(), ContentType.APPLICATION_JSON));

        // Execute request
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity);
            
            // Parse response
            JSONObject jsonResponse = new JSONObject(responseBody);
            JSONArray data = jsonResponse.getJSONArray("data");
            
            if (data.length() > 0) {
                String imageUrl = data.getJSONObject(0).getString("url");
                return removeBackground(imageUrl);
            } else {
                throw new IOException("No image data in response");
            }
        }
    }

    private String removeBackground(String imageUrl) throws IOException, ParseException {
        String url = baseUrl + "/images/removeBackground";
        
        // Download the image first
        byte[] imageBytes = downloadImageBytes(imageUrl);
        
        // Create multipart request
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody("file", imageBytes, ContentType.IMAGE_PNG, "image.png");
        HttpEntity multipart = builder.build();

        // Create HTTP POST request
        HttpPost request = new HttpPost(url);
        request.setHeader("Authorization", "Bearer " + apiKey);
        request.setEntity(multipart);

        // Execute request
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity);
            
            // Parse response
            JSONObject jsonResponse = new JSONObject(responseBody);
            return jsonResponse.getJSONObject("image").getString("url");
        }
    }

    private byte[] downloadImageBytes(String imageUrl) throws IOException {
        HttpGet request = new HttpGet(imageUrl);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            HttpEntity entity = response.getEntity();
            return EntityUtils.toByteArray(entity);
        }
    }

    public String generateAILogo(String businessName, String description) throws IOException, ParseException {
        String prompt = String.format(
            "logo for ‘%s’, %s.",
            "The text color should be a teal green (#008F73). " +
            businessName,
            description
        );

        return generateImage(prompt, "digital_illustration");
    }

    public static void main(String[] args) {

        try{
            RecraftAPIClient client = new RecraftAPIClient();
            String logoUrl = client.generateAILogo("RuckQuest", "a blog about rucking");
            WordPressMediaClient mediaClient = new WordPressMediaClient();
            mediaClient.updateSiteLogoFromUrl(logoUrl);
        } catch(IOException | ParseException e) {
            e.printStackTrace();
        }
    }
}  