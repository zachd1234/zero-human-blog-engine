package com.zho.api;

import com.zho.config.ConfigManager;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class OpenAIClient {
    private final String apiKey;
    private final String apiUrl;
    private final OkHttpClient client;
    
    // Configuration constants

    public OpenAIClient() {
        this.apiKey = ConfigManager.getOpenAiKey();
        this.apiUrl = ConfigManager.getOpenAiUrl();
        this.client = new OkHttpClient();
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

    public String callOpenAI(String prompt, double temperature) {
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
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

    public static void main(String[] args) {
        try {
            OpenAIClient client = new OpenAIClient();
            
            // Test 1: Simple prompt
            System.out.println("Test 1: Simple prompt");
            String simplePrompt = "What is artificial intelligence?";
            String response1 = client.callOpenAI(simplePrompt, 0.7);
            System.out.println("Response: " + response1);
            
            // Test 2: Category generation prompt
            System.out.println("\nTest 2: Category generation");
            String categoryPrompt = "Generate 6 specific categories for a technology blog. For each category:\n" +
                                  "1. Create a clear, concise title (1-2 words maximum, no numbering)\n" +
                                  "Format: one category per line";
            String response2 = client.callOpenAI(categoryPrompt, 0.7);
            System.out.println("Response: " + response2);
            
            // Test 3: Complex prompt
            System.out.println("\nTest 3: Complex prompt");
            String complexPrompt = "Generate 6 specific categories for a Digital Marketing blog. For each category:\n" +
                                 "1. Create a clear, concise title (1-2 words maximum, no numbering)\n" +
                                 "Format: one category per line";
            String response3 = client.callOpenAI(complexPrompt, 0.7);
            System.out.println("Response: " + response3);
            
            System.out.println("\nAll OpenAI tests completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Error during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 