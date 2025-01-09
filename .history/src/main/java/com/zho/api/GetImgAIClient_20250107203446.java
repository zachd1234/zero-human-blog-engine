package com.zho.api;

import com.zho.config.ConfigManager;
import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;

public class GetImgAIClient {
    private final String apiKey;
    private final String apiUrl;
    private final OkHttpClient client;
    
    public GetImgAIClient() {
        this.apiKey = ConfigManager.getGetImgApiKey();
        this.apiUrl = ConfigManager.getGetImgApiUrl();
        this.client = new OkHttpClient();
    }
    
    public String generateImage(String prompt) throws IOException {
        return generateImage(prompt, 1024, 1024, 4, null);
    }
    
    public String generateImage(String prompt, Integer width, Integer height, 
                              Integer steps, Integer seed) throws IOException {
        JSONObject json = new JSONObject()
            .put("prompt", prompt)
            .put("width", width)
            .put("height", height)
            .put("steps", steps)
            .put("output_format", "jpeg")
            .put("response_format", "url");  // Using URL format for easier handling
            
        if (seed != null) {
            json.put("seed", seed);
        }
            
        RequestBody body = RequestBody.create(
            json.toString(), 
            MediaType.get("application/json; charset=utf-8")
        );
        
        Request request = new Request.Builder()
            .url(apiUrl + "/flux-schnell/text-to-image")
            .header("Authorization", "Bearer " + apiKey)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .post(body)
            .build();
            
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                throw new IOException("API call failed: " + response.code() + " - " + errorBody);
            }
            
            JSONObject responseJson = new JSONObject(response.body().string());
            return responseJson.getString("url");  // Returns the image URL
        }
    }
    
    // Test method
    public static void main(String[] args) {
        try {
            GetImgAIClient client = new GetImgAIClient();
            
            String prompt = "Professional headshot of a business person, " +
                          "high quality, realistic, natural lighting";
            
            String imageUrl = client.generateImage(
                prompt,      // prompt
                1024,       // width
                1024,       // height
                4,          // steps
                null        // seed (random)
            );
            
            System.out.println("Image generated successfully!");
            System.out.println("URL: " + imageUrl);
            
        } catch (Exception e) {
            System.err.println("Error generating image: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 