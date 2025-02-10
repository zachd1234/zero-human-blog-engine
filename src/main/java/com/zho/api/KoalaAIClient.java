package com.zho.api;

import com.zho.config.ConfigManager;
import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class KoalaAIClient {
    private final String apiKey;
    private final OkHttpClient client;
    private static final String BASE_URL = "https://koala.sh/api";

    public KoalaAIClient() {
        this.apiKey = ConfigManager.getKoalaWriterKey();
        this.client = new OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .build();
    }

    public JSONObject createPost(String topic, String keyword, String instructions) {
        System.out.println("\n🐨 Creating Koala Writer post...");
        System.out.println("Topic: " + topic);
        System.out.println("Keyword: " + keyword);
        
        try {
            JSONObject requestBody = new JSONObject()
                .put("topic", topic)
                .put("keyword", keyword)
                .put("instructions", instructions)
                .put("type", "blog_post");

            Request request = new Request.Builder()
                .url(BASE_URL + "/posts/create")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(
                    requestBody.toString(), 
                    MediaType.get("application/json; charset=utf-8")))
                .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    System.err.println("❌ Koala Writer API error: " + errorBody);
                    throw new IOException("API call failed with code " + response.code());
                }

                JSONObject responseBody = new JSONObject(response.body().string());
                System.out.println("✅ Post created successfully!");
                return responseBody;
            }
        } catch (Exception e) {
            System.err.println("❌ Error creating post: " + e.getMessage());
            throw new RuntimeException("Failed to create post", e);
        }
    }

    public JSONObject createShortBlogPost(String keyword, String topic) {
        System.out.println("\n🐨 Creating short blog post...");
        System.out.println("Keyword: " + keyword);
        System.out.println("Topic: " + topic);
        
        try {
            JSONObject requestBody = new JSONObject()
                .put("targetKeyword", keyword)
                .put("gptVersion", "gpt-4o-mini")
                .put("articleType", "blog_post")
                .put("articleLength", "short")
                .put("autoPolish", true)
                .put("polishSettings", new JSONObject()
                    .put("split-up-long-paragraphs", true)
                    .put("remove-mid-article-conclusions", true)
                    .put("remove-repetitive-sentences", true)
                    .put("convert-passive-voice", false)
                    .put("simplify-complex-sentences", true))
                .put("toneOfVoiceProfile", "seo_optimized")
                .put("includeFaq", true)
                .put("readabilityMode", "8th_grade");

            Request request = new Request.Builder()
                .url(BASE_URL + "/articles/")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(
                    requestBody.toString(), 
                    MediaType.get("application/json; charset=utf-8")))
                .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    System.err.println("❌ Koala Writer API error: " + errorBody);
                    throw new IOException("API call failed with code " + response.code());
                }

                JSONObject articleResponse = new JSONObject(response.body().string());
                String articleId = articleResponse.getString("articleId");
                System.out.println("✅ Article queued with ID: " + articleId);
                
                // Poll for completion
                while (true) {
                    Thread.sleep(10000); // Wait 10 seconds between checks
                    
                    Request statusRequest = new Request.Builder()
                        .url(BASE_URL + "/articles/" + articleId)
                        .header("Authorization", "Bearer " + apiKey)
                        .get()
                        .build();
                    
                    try (Response statusResponse = client.newCall(statusRequest).execute()) {
                        JSONObject status = new JSONObject(statusResponse.body().string());
                        String articleStatus = status.getString("status");
                        String statusDetail = status.getString("statusDetail");
                        
                        System.out.println("📝 Status: " + statusDetail);
                        
                        if (articleStatus.equals("finished")) {
                            System.out.println("✅ Article completed!");
                            return status;
                        } else if (articleStatus.equals("failed")) {
                            throw new RuntimeException("Article generation failed");
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error creating blog post: " + e.getMessage());
            throw new RuntimeException("Failed to create blog post", e);
        }
    }

    public JSONObject createOptimizedBlogPost(String keyword, String title) {
        System.out.println("\n�� Creating optimized blog post with Koala Writer...");
        System.out.println("Keyword: " + keyword);
        System.out.println("Title: " + title);
        
        try {
            JSONObject requestBody = new JSONObject()
                .put("targetKeyword", keyword)
                // TESTING MODE: Use GPT-4-mini for cheaper testing
                .put("gptVersion", "gpt-4-mini")
                // PRODUCTION MODE (uncomment below line and comment out above line)
                // .put("gptVersion", "claude-3.5-sonnet")
                .put("articleType", "blog_post")
                // TESTING MODE: Shorter length for testing
                .put("articleLength", "shorter")
                // PRODUCTION MODE (uncomment below line and comment out above line)
                // .put("articleLength", "short")
                // Internal linking configuration
                .put("internalLinkingDomainId", "4b437f51-0c3e-419c-b81e-f32fc3e15e5e")  // RuckQuest.com
                // Common parameters
                .put("seoOptimizationLevel", "ai_powered")
                .put("multimediaOption", "auto")  // Both AI images and YouTube videos
                .put("toneOfVoiceProfile", "seo_optimized")
                .put("language", "English")
                .put("country", "United States")
                .put("pointOfView", "third_person")
                .put("realTimeData", true)
                .put("shouldCiteSources", true)
                .put("includeFaq", true)
                .put("includeKeyTakeaways", true)
                .put("readabilityMode", "8th_grade")
                .put("autoPolish", true)
                .put("extraTitlePrompt", "make the title exactly this: " + title)  // Add the extra title prompt
                .put("polishSettings", new JSONObject()
                    .put("split-up-long-paragraphs", true)
                    .put("remove-mid-article-conclusions", true)
                    .put("remove-repetitive-sentences", true)
                    .put("convert-passive-voice", true)
                    .put("simplify-complex-sentences", true));

            return makeArticleRequest(requestBody);
        } catch (Exception e) {
            System.err.println("❌ Error creating optimized blog post: " + e.getMessage());
            throw new RuntimeException("Failed to create optimized blog post", e);
        }
    }

    public JSONObject createListicle(String keyword) {
        System.out.println("\n🐨 Creating listicle with Koala Writer...");
        System.out.println("Keyword: " + keyword);
        
        try {
            JSONObject requestBody = new JSONObject()
                .put("targetKeyword", keyword)
                // TESTING MODE: Use GPT-4-mini for cheaper testing
                .put("gptVersion", "gpt-4-mini")
                // PRODUCTION MODE (uncomment below line and comment out above line)
                // .put("gptVersion", "gpt-4")
                .put("articleType", "listicle")
                // TESTING MODE: Minimal items for testing
                .put("enableAutomaticLength", false)
                .put("numberOfItems", 2)  // Using 2 items for testing
                // PRODUCTION MODE (uncomment below line and comment out above 2 lines)
                // .put("enableAutomaticLength", true)
                // Common parameters
                .put("language", "English")
                .put("toneOfVoiceProfile", "seo_optimized")
                .put("includeFaq", true)
                .put("readabilityMode", "8th_grade")
                .put("autoPolish", true)
                .put("polishSettings", new JSONObject()
                    .put("split-up-long-paragraphs", true)
                    .put("remove-mid-article-conclusions", true)
                    .put("remove-repetitive-sentences", true)
                    .put("convert-passive-voice", true)
                    .put("simplify-complex-sentences", true));

            return makeArticleRequest(requestBody);
        } catch (Exception e) {
            System.err.println("❌ Error creating listicle: " + e.getMessage());
            throw new RuntimeException("Failed to create listicle", e);
        }
    }

    public JSONObject createAmazonRoundup(String keyword) {
        System.out.println("\n🐨 Creating Amazon product roundup with Koala Writer...");
        System.out.println("Keyword: " + keyword);
        
        try {
            JSONObject requestBody = new JSONObject()
                .put("targetKeyword", keyword)
                // TESTING MODE: Use GPT-4-mini for cheaper testing
                .put("gptVersion", "gpt-4-mini")
                // PRODUCTION MODE (uncomment below line and comment out above line)
                // .put("gptVersion", "gpt-4")
                .put("articleType", "amazon_product_roundup")
                // TESTING MODE: Minimal products for testing
                .put("numAmazonProducts", 2)  // Using 2 products for testing
                // PRODUCTION MODE (uncomment below line and comment out above line)
                // .put("numAmazonProducts", 10)
                // Common parameters
                .put("language", "English")
                .put("toneOfVoiceProfile", "seo_optimized")
                .put("includeFaq", true)
                .put("readabilityMode", "8th_grade")
                .put("autoPolish", true)
                .put("polishSettings", new JSONObject()
                    .put("split-up-long-paragraphs", true)
                    .put("remove-mid-article-conclusions", true)
                    .put("remove-repetitive-sentences", true)
                    .put("convert-passive-voice", true)
                    .put("simplify-complex-sentences", true))
                .put("amazonDomain", "amazon.com")
                .put("enableFirstHandExperience", true);

            return makeArticleRequest(requestBody);
        } catch (Exception e) {
            System.err.println("❌ Error creating Amazon roundup: " + e.getMessage());
            throw new RuntimeException("Failed to create Amazon roundup", e);
        }
    }

    // Helper method to make the actual API request
    private JSONObject makeArticleRequest(JSONObject requestBody) throws IOException, InterruptedException {
        Request request = new Request.Builder()
            .url(BASE_URL + "/articles/")
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .post(RequestBody.create(
                requestBody.toString(), 
                MediaType.get("application/json; charset=utf-8")))
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                System.err.println("❌ Koala Writer API error: " + errorBody);
                throw new IOException("API call failed with code " + response.code());
            }

            JSONObject articleResponse = new JSONObject(response.body().string());
            String articleId = articleResponse.getString("articleId");
            System.out.println("✅ Article queued with ID: " + articleId);
            
            return pollForCompletion(articleId);
        }
    }

    // Helper method to poll for completion (existing method)
    private JSONObject pollForCompletion(String articleId) throws IOException, InterruptedException {
        while (true) {
            Thread.sleep(10000); // Wait 10 seconds between checks
            
            Request statusRequest = new Request.Builder()
                .url(BASE_URL + "/articles/" + articleId)
                .header("Authorization", "Bearer " + apiKey)
                .get()
                .build();
            
            try (Response statusResponse = client.newCall(statusRequest).execute()) {
                JSONObject status = new JSONObject(statusResponse.body().string());
                String articleStatus = status.getString("status");
                String statusDetail = status.getString("statusDetail");
                
                System.out.println("📝 Status: " + statusDetail);
                
                if (articleStatus.equals("finished")) {
                    System.out.println("✅ Article completed!");
                    return status;
                } else if (articleStatus.equals("failed")) {
                    throw new RuntimeException("Article generation failed");
                }
            }
        }
    }

    public String koalaChat(String input) {
        System.out.println("\n🤖 Sending to Koala Chat: " + input);
        
        try {
            // Don't URL encode commands that start with /
            String encodedInput;
            if (input.startsWith("/")) {
                encodedInput = input;  // Keep commands as-is
            } else {
                encodedInput = java.net.URLEncoder.encode(input, "UTF-8")
                    .replace("+", " ");  // Keep spaces readable
            }
            
            // Create JSON request body
            String jsonBody = String.format("{\"input\":\"%s\",\"realTimeData\":true}", encodedInput);
            
            // Create HTTP client
            HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
            
            // Create request
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/gpt/"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
            
            System.out.println("DEBUG - Sending request body: " + jsonBody);
            
            // Send request
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            System.out.println("DEBUG - Raw response: " + response.body());
            
            if (response.statusCode() != 200) {
                System.err.println("❌ Koala Chat API error: " + response.body());
                throw new IOException("API call failed with code " + response.statusCode());
            }
            
            String responseBody = response.body();
            
            // Handle SERP and other streaming responses that start with "data: "
            if (responseBody.startsWith("data: ")) {
                // Remove the "data: " prefix and any surrounding quotes
                return responseBody.substring(6).replaceAll("^\"|\"$", "");
            }
            
            // Handle regular JSON responses
            JSONObject chatResponse = new JSONObject(responseBody);
            String output = chatResponse.getString("output");
            System.out.println("✅ Koala Chat response received");
            return output;
            
        } catch (Exception e) {
            System.err.println("❌ Error in Koala Chat: " + e.getMessage());
            throw new RuntimeException("Failed to get chat response", e);
        }
    }

    public String getSerpResults(String keyword) {
        System.out.println("\n🔍 Getting SERP data for: " + keyword);
        String serpCommand = "/serp " + keyword;
        return koalaChat(serpCommand);
    }

    // Test method
    public static void main(String[] args) {
        try {
            KoalaAIClient client = new KoalaAIClient();
            // Print first few chars of API key to verify it's loadedç
            System.out.println("API Key starts with: " + client.apiKey.substring(0, 10) + "...");
            
            String test = client.koalaChat("hi");
            System.out.println("\nChat Response:\n" + test);

            String chatResponse = client.getSerpResults("how to start rucking");
            System.out.println("\nChat Response:\n" + chatResponse);

            
        } catch (Exception e) {
            System.err.println("Error during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 