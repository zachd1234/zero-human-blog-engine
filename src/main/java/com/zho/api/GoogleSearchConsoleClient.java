package com.zho.api;

import com.google.auth.oauth2.GoogleCredentials;
import com.zho.services.AwsSecretsManagerService;
import okhttp3.*;
import org.json.JSONObject;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class GoogleSearchConsoleClient {
    private static final String INDEXING_API_URL = "https://indexing.googleapis.com/v3/urlNotifications:publish";
    private static final String LOCAL_CREDENTIALS_PATH = "/Users/zachderhake/Downloads/autokeywords-445618-457181be0577.json";
    
    private final OkHttpClient client;

    public GoogleSearchConsoleClient() throws IOException {
        client = new OkHttpClient();
    }

    private static String getCredentialsJson() throws IOException {
        // Try local file first
        try {
            System.out.println("Trying local credentials file...");
            String localCredentials = new String(java.nio.file.Files.readAllBytes(
                java.nio.file.Paths.get(LOCAL_CREDENTIALS_PATH)));
            if (localCredentials != null && !localCredentials.isEmpty()) {
                System.out.println("✅ Using local credentials file");
                return localCredentials;
            }
        } catch (IOException e) {
            System.out.println("Local file not available: " + e.getMessage());
        }

        // Fallback to AWS Secrets Manager
        try {
            System.out.println("Trying AWS Secrets Manager...");
            String awsCredentials = AwsSecretsManagerService.getGoogleCredentials();
            if (awsCredentials != null && !awsCredentials.isEmpty()) {
                System.out.println("✅ Using AWS Secrets Manager credentials");
                return awsCredentials;
            }
        } catch (Exception e) {
            System.out.println("AWS Secrets Manager not available: " + e.getMessage());
        }

        throw new IOException("Failed to load credentials from both local file and AWS");
    }

    public void submitUrl(String url) throws IOException {
        // Validate URL first
        if (url == null || url.trim().isEmpty()) {
            throw new IOException("URL cannot be null or empty");
        }
        url = url.trim();  // Remove any whitespace
        
        System.out.println("\n🔍 Starting Google Search Console submission...");
        System.out.println("URL to submit: " + url);
        
        try {
            String credentialsJson = getCredentialsJson();
            System.out.println("✅ Credentials loaded successfully");
            
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8)))
                .createScoped("https://www.googleapis.com/auth/indexing");
            System.out.println("✅ Google credentials created with indexing scope");
            
            credentials.refreshIfExpired();
            System.out.println("✅ Credentials refreshed");
            
            // Create request body
            String jsonBody = String.format("{\"url\":\"%s\",\"type\":\"URL_UPDATED\"}", url);
            System.out.println("📝 Request body: " + jsonBody);

            RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                jsonBody
            );

            Request request = new Request.Builder()
                .url(INDEXING_API_URL)
                .addHeader("Authorization", "Bearer " + credentials.getAccessToken().getTokenValue())
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();
            System.out.println("📤 Request built and ready to send");

            try (Response response = client.newCall(request).execute()) {
                int responseCode = response.code();
                String responseBody = response.body() != null ? response.body().string() : "";
                
                System.out.println("\n📥 Response from Google Search Console API:");
                System.out.println("Status Code: " + responseCode);
                System.out.println("Response Body: " + responseBody);
                
                if (responseCode != 200) {
                    throw new IOException("Failed to submit URL. Response code: " + responseCode);
                }
                
                // Verify the response contains our URL
                if (!responseBody.contains(url)) {
                    throw new IOException("Response doesn't contain the submitted URL. Possible truncation.");
                }
                
                System.out.println("\n✅ Successfully submitted URL to Google Search Console: " + url);
            }
            
        } catch (Exception e) {
            System.err.println("\n❌ Error submitting URL: " + e.getMessage());
            throw new IOException("Failed to submit URL: " + url, e);
        }
    }

    public void autoIndexPosts(String[] postUrls) {
        for (String postUrl : postUrls) {
            try {
                submitUrl(postUrl);
                System.out.println("Successfully submitted URL: " + postUrl);
            } catch (IOException e) {
                System.err.println("Error submitting URL: " + postUrl + " - " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        try {
            // First test getCredentialsJson
            GoogleSearchConsoleClient client = new GoogleSearchConsoleClient();
            System.out.println("\n🔑 Testing credentials...");
            String credentials = getCredentialsJson();
            
            // Create credentials and get access token
            GoogleCredentials googleCreds = GoogleCredentials.fromStream(
                new ByteArrayInputStream(credentials.getBytes(StandardCharsets.UTF_8)))
                .createScoped("https://www.googleapis.com/auth/indexing");
            
            googleCreds.refreshIfExpired();
                        
            // Continue with URL test
            String testUrl = "https://ruckquest.com/ultimate-guide-rucking-apple-watch-fitness/";
            System.out.println("\n🌐 Testing URL submission for: " + testUrl);
            client.submitUrl(testUrl);
            
        } catch (Exception e) {
            System.err.println("\n❌ Error in test: " + e.getMessage());
            e.printStackTrace();
        }
    }
}