package com.zho.api;

import com.google.auth.oauth2.GoogleCredentials;
import com.zho.services.AwsSecretsManagerService;
import okhttp3.*;
import org.json.JSONObject;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class GoogleSearchConsoleClient {
    private static final String INDEXING_API_URL = "https://indexing.googleapis.com/v3/urlNotifications:publish";
    private static final String LOCAL_CREDENTIALS_PATH = "/Users/zachderhake/Downloads/autokeywords-445618-457181be0577.json";
    
    private final OkHttpClient client;

    public GoogleSearchConsoleClient() throws IOException {
        client = new OkHttpClient();
    }

    private static String getCredentialsJson() throws IOException {
        try {
            // First, try AWS Secrets Manager
            String awsCredentials = AwsSecretsManagerService.getGoogleCredentials();
            if (awsCredentials != null && !awsCredentials.isEmpty()) {
                System.out.println("Using AWS Secrets Manager credentials");
                return awsCredentials;
            }
        } catch (Exception e) {
            System.out.println("AWS Secrets Manager not available, falling back to local file");
        }

        // Fallback to local file
        try {
            System.out.println("Using local credentials file");
            return new String(java.nio.file.Files.readAllBytes(
                java.nio.file.Paths.get(LOCAL_CREDENTIALS_PATH)));
        } catch (IOException e) {
            throw new IOException("Failed to load credentials from both AWS and local file", e);
        }
    }

    public void submitUrl(String url) throws IOException {
        String credentialsJson = getCredentialsJson();
        GoogleCredentials credentials = GoogleCredentials.fromStream(
            new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8)))
            .createScoped("https://www.googleapis.com/auth/indexing");
        
        credentials.refreshIfExpired();
        
        JSONObject json = new JSONObject()
            .put("url", url)
            .put("type", "URL_UPDATED");

        RequestBody body = RequestBody.create(
            json.toString(), 
            MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
            .url(INDEXING_API_URL)
            .addHeader("Authorization", "Bearer " + credentials.getAccessToken().getTokenValue())
            .post(body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to submit URL: " + response.code() + " " + response.body().string());
            }
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
        String testUrl = "https://ruckquest.com"; // Replace with the URL you want to submit

        try {
            GoogleSearchConsoleClient client = new GoogleSearchConsoleClient();
            client.submitUrl(testUrl);
            System.out.println("Successfully submitted URL: " + testUrl);
        } catch (IOException e) {
            System.err.println("Error submitting URL: " + e.getMessage());
            e.printStackTrace();
        }
    }
}