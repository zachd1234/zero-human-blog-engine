package com.zho.api;

import com.google.auth.oauth2.GoogleCredentials;
import okhttp3.*;
import org.json.JSONObject;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;

public class GoogleSearchConsoleClient {
    private static final String INDEXING_API_URL = "https://indexing.googleapis.com/v3/urlNotifications:publish";
    private final OkHttpClient client;
    private final GoogleCredentials credentials;

    public GoogleSearchConsoleClient(String credentialsPath) throws IOException {
        credentials = GoogleCredentials.fromStream(new FileInputStream(credentialsPath))
                .createScoped(Collections.singleton("https://www.googleapis.com/auth/indexing"));
        client = new OkHttpClient();
    }

    public void submitUrl(String url) throws IOException {
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
        String credentialsPath = "/Users/zachderhake/Downloads/autokeywords-445618-457181be0577.json"; // Updated path
        String testUrl = "https://ruckquest.com"; // Replace with the URL you want to submit

        try {
            GoogleSearchConsoleClient client = new GoogleSearchConsoleClient(credentialsPath);
            client.submitUrl(testUrl);
            System.out.println("Successfully submitted URL: " + testUrl);
        } catch (IOException e) {
            System.err.println("Error submitting URL: " + e.getMessage());
            e.printStackTrace();
        }
    }
}