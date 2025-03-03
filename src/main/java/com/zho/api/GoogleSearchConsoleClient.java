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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.auth.oauth2.AccessToken;
import java.net.URL;
import java.net.URLEncoder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

public class GoogleSearchConsoleClient {
    private static final String INSPECTION_API_URL = "https://searchconsole.googleapis.com/v1/urlInspection/index:inspect";
    private static final String INDEX_REQUEST_API_URL = "https://searchconsole.googleapis.com/v1/urlInspection/index:requestIndexing";
    private static final String LOCAL_CREDENTIALS_PATH = "/Users/zachderhake/Downloads/autokeywords-445618-457181be0577.json";
    
    private final OkHttpClient client;
    private final CloseableHttpClient httpClient;

    public GoogleSearchConsoleClient() throws IOException {
        client = new OkHttpClient();
        httpClient = HttpClientBuilder.create().build();
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
        if (url == null || url.trim().isEmpty()) {
            throw new IOException("URL cannot be null or empty");
        }
        url = url.trim();
        
        System.out.println("\n🔍 Starting URL Inspection...");
        System.out.println("URL to inspect: " + url);
        
        try {
            // Get credentials with webmasters scope
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                new ByteArrayInputStream(getCredentialsJson().getBytes(StandardCharsets.UTF_8)))
                .createScoped("https://www.googleapis.com/auth/webmasters");
            
            credentials.refresh();
            
            // Inspect URL
            String domain = new URL(url).getHost();
            String jsonBody = String.format(
                "{"
                + "\"inspectionUrl\": \"%s\","
                + "\"siteUrl\": \"sc-domain:%s\""
                + "}", 
                url, 
                domain
            );

            Request inspectRequest = new Request.Builder()
                .url(INSPECTION_API_URL)
                .addHeader("Authorization", "Bearer " + credentials.getAccessToken().getTokenValue())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    jsonBody))
                .build();

            try (Response response = client.newCall(inspectRequest).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                
                if (response.code() == 200) {
                    JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                    JsonObject inspectionResult = jsonResponse.getAsJsonObject("inspectionResult");
                    JsonObject indexStatus = inspectionResult.getAsJsonObject("indexStatusResult");
                    
                    System.out.println("\n📋 Inspection Results:");
                    System.out.println("Coverage State: " + indexStatus.get("coverageState").getAsString());
                    if (indexStatus.has("indexingState")) {
                        System.out.println("Indexing State: " + indexStatus.get("indexingState").getAsString());
                    }
                    if (indexStatus.has("lastCrawlTime")) {
                        System.out.println("Last Crawl: " + indexStatus.get("lastCrawlTime").getAsString());
                    }
                    
                    // Get the inspection link from the response
                    String inspectionLink = inspectionResult.get("inspectionResultLink").getAsString();
                    System.out.println("\n🔗 To request indexing, visit:");
                    System.out.println(inspectionLink);
                    System.out.println("Then click 'Request Indexing' in the Search Console interface");
                    
                } else {
                    throw new IOException("Inspection failed. Status: " + response.code());
                }
            }
            
        } catch (Exception e) {
            System.err.println("\n❌ Error during inspection:");
            System.err.println(e.getMessage());
            throw new IOException("Failed to inspect URL: " + url, e);
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

    public void notifyGoogle() throws IOException {
        String sitemapUrl = "https://ruckquest.com/sitemap_index.xml";
        String pingUrl = "https://www.google.com/ping?sitemap=" + URLEncoder.encode(sitemapUrl, StandardCharsets.UTF_8);
        
        System.out.println("\n🔔 Pinging Google with sitemap: " + sitemapUrl);
        HttpGet request = new HttpGet(pingUrl);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            System.out.println("Sitemap ping response: " + statusCode);
            if (statusCode == 200) {
                System.out.println("✅ Successfully notified Google about sitemap update");
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
                .createScoped("https://www.googleapis.com/auth/webmasters");
            
            googleCreds.refreshIfExpired();
                        
            // Continue with URL test
            String testUrl = "https://ruckquest.com/rucking-at-the-gym-guide-weighted-walks/";
            System.out.println("\n🌐 Testing URL submission for: " + testUrl);
            client.notifyGoogle();
            
        } catch (Exception e) {
            System.err.println("\n❌ Error in test: " + e.getMessage());
            e.printStackTrace();
        }
    }
}