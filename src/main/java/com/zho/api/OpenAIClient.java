package com.zho.api;

import com.zho.config.ConfigManager;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class OpenAIClient {
    private final String apiKey;
    private final String apiUrl;
    private final OkHttpClient client;
    
    private static final int CONTENT_DELAY_MS = 3000; // 3 second delay
    private long lastContentCallTime = 0;

    private static final String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");
    private static final String GPT4_MODEL = "gpt-4";
    private static final String GPT4_MINI_MODEL = "gpt-4-0125-preview";
    private static final String O3_MINI_MODEL = "o3-mini-2025-01-31";

    // Configuration constants

    public OpenAIClient() {
        this.apiKey = ConfigManager.getOpenAiKey();
        this.apiUrl = ConfigManager.getOpenAiUrl();
        this.client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build();
    }

    public String callOpenAI(String prompt) {
        OkHttpClient client = new OkHttpClient();
        JSONObject json = new JSONObject();
        json.put("model", "gpt-4o-mini");
        json.put("messages", new JSONArray()
            .put(new JSONObject()
                .put("role", "user")
                .put("content", prompt)));

        RequestBody body = RequestBody.create(
            json.toString(), MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
            .url(apiUrl)
            .header("Authorization", "Bearer " + apiKey)
            .post(body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            JSONObject responseBody = new JSONObject(response.body().string());
            return responseBody.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim();
        } catch (IOException e) {
            e.printStackTrace();
            return "Error generating content";
        }
    }

    public String callOpenAI(String prompt, double temperature) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build();
        
        JSONObject json = new JSONObject();
        json.put("model", "gpt-4o-mini-2024-07-18");
        json.put("temperature", temperature);
        json.put("messages", new JSONArray()
            .put(new JSONObject()
                .put("role", "user")
                .put("content", prompt)));

        RequestBody body = RequestBody.create(
            json.toString(), MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
            .url(apiUrl)
            .header("Authorization", "Bearer " + apiKey)
            .post(body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body().string();
                System.err.println("API Error: " + errorBody);
                throw new IOException("API call failed with code " + response);
            }

            JSONObject responseBody = new JSONObject(response.body().string());
            return responseBody.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim();
        } catch (IOException e) {
            e.printStackTrace();
            return "Error generating content";
        }
    }


    // New method specifically for content generation
    public String callOpenAIWithRateLimit(String prompt) throws IOException {
        // Enforce delay between content generation calls
        enforceContentRateLimit();
        
        lastContentCallTime = System.currentTimeMillis();
        return callOpenAI(prompt);
    }

    private void enforceContentRateLimit() {
        long timeSinceLastCall = System.currentTimeMillis() - lastContentCallTime;
        if (timeSinceLastCall < CONTENT_DELAY_MS) {
            try {
                long sleepTime = CONTENT_DELAY_MS - timeSinceLastCall;
                System.out.println("Content generation rate limiting: waiting " + sleepTime + "ms");
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Rate limit sleep interrupted");
            }
        }
    }

    public String callGPT4(String prompt) throws IOException {
        JSONObject json = new JSONObject();
        json.put("model", "gpt-4o");
        json.put("temperature", 1.0);
        json.put("stream", false);
        json.put("messages", new JSONArray()
            .put(new JSONObject()
                .put("role", "user")
                .put("content", prompt)));

        RequestBody body = RequestBody.create(
            json.toString(), MediaType.get("application/json; charset=utf-8"));

        String gpt4Url = "https://api.openai.com/v1/chat/completions";
        
        Request request = new Request.Builder()
            .url(gpt4Url)
            .header("Authorization", "Bearer " + apiKey)
            .post(body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                throw new IOException("API call failed with code " + response.code() + ": " + errorBody);
            }

            String responseStr = response.body().string();
            JSONObject responseBody = new JSONObject(responseStr);
            
            String content = responseBody.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim();
                
            // Clean up markdown code block markers if present
            content = content.replaceAll("```json\\s*", "")
                           .replaceAll("```\\s*", "")
                           .trim();
                           
            return content;
        } catch (Exception e) {
            System.err.println("Error in GPT-4 call: " + e.getMessage());
            return "Error generating content";
        }
    }

    // Overload with temperature parameter
    public String callGPT4(String prompt, double temperature) throws IOException {
        JSONObject json = new JSONObject();
        json.put("model", "gpt-4o");  // Back to gpt-4o
        json.put("temperature", temperature);
        json.put("messages", new JSONArray()
            .put(new JSONObject()
                .put("role", "user")
                .put("content", prompt)));

        RequestBody body = RequestBody.create(
            json.toString(), MediaType.get("application/json; charset=utf-8"));

        String gpt4Url = "https://api.openai.com/v1/chat/completions";
        
        Request request = new Request.Builder()
            .url(gpt4Url)
            .header("Authorization", "Bearer " + apiKey)
            .post(body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                throw new IOException("API call failed with code " + response.code() + ": " + errorBody);
            }

            String responseStr = response.body().string();
            JSONObject responseBody = new JSONObject(responseStr);
            
            return responseBody.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim();
        } catch (Exception e) {
            System.err.println("Error in GPT-4 call: " + e.getMessage());
            return "Error generating content";
        }
    }

    public String callOpenAIWithSystemPrompt(String prompt, String systemPrompt) {
        OkHttpClient client = new OkHttpClient();
        JSONObject json = new JSONObject();
        json.put("model", "gpt-3.5-turbo");
        json.put("messages", new JSONArray()
            .put(new JSONObject()  // First message is the system prompt
                .put("role", "system")
                .put("content", systemPrompt))
            .put(new JSONObject()  // Second message is the user prompt
                .put("role", "user")
                .put("content", prompt)));

        RequestBody body = RequestBody.create(
            json.toString(), MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
            .url(apiUrl)
            .header("Authorization", "Bearer " + apiKey)
            .post(body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            JSONObject responseBody = new JSONObject(response.body().string());
            return responseBody.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim();
        } catch (IOException e) {
            e.printStackTrace();
            return "Error generating content";
        }
    }

    public String callO3(String prompt) throws IOException {
        JSONObject json = new JSONObject()
            .put("model", O3_MINI_MODEL)
            .put("messages", new JSONArray()
                .put(new JSONObject()
                    .put("role", "user")
                    .put("content", prompt)));

        RequestBody body = RequestBody.create(
            json.toString(), MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .header("Authorization", "Bearer " + apiKey)
            .post(body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                throw new IOException("API call failed with code " + response.code() + ": " + errorBody);
            }

            String responseStr = response.body().string();
            JSONObject responseBody = new JSONObject(responseStr);
            
            return responseBody.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim();
                
        } catch (Exception e) {
            System.err.println("Error in O3 call: " + e.getMessage());
            throw new RuntimeException("Failed to call O3 API", e);
        }
    }

    public static void main(String[] args) {
        try {
            OpenAIClient client = new OpenAIClient();
            
            // Test 1: Simple prompt
            System.out.println("Test 1: Simple prompt");
            String simplePrompt = "What is artificial intelligence?";
            String response1 = client.callO3(simplePrompt);
            System.out.println("Response: " + response1);
            
           
            System.out.println("\nAll OpenAI tests completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Error during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 