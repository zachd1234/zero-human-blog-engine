package com.zho.api;

import com.zho.config.ConfigManager;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.json.JSONException;
import org.apache.hc.core5.http.ParseException;

public class NounProjectClient {
    private final String apiKey;
    private final String apiSecret;
    private final CloseableHttpClient httpClient;
    private static final String API_URL = "http://api.thenounproject.com/v2";

    public NounProjectClient() {
        ConfigManager config = ConfigManager.getInstance();
        this.apiKey = config.getNounProjectKey();
        this.apiSecret = config.getNounProjectSecret();
        this.httpClient = HttpClients.createDefault();
    }

    public JSONObject searchIcons(String query) throws IOException {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = String.format("%s/icons?query=%s&limit=10", API_URL, encodedQuery);

        HttpGet request = new HttpGet(URI.create(url));
        String auth = apiKey + ":" + apiSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        request.setHeader("Authorization", "Basic " + encodedAuth);

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            try {
                String responseBody = EntityUtils.toString(response.getEntity());
                return new JSONObject(responseBody);
            } catch (ParseException e) {
                throw new IOException("Failed to parse response", e);
            }
        }
    }

    public byte[] downloadIcon(String iconUrl) throws IOException {
        HttpGet request = new HttpGet(URI.create(iconUrl));
        String auth = apiKey + ":" + apiSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        request.setHeader("Authorization", "Basic " + encodedAuth);

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            return EntityUtils.toByteArray(response.getEntity());
        }
    }

    public static void main(String[] args) {
        try {
            // Create client instance
            NounProjectClient client = new NounProjectClient();
            
            // Test icon search
            System.out.println("Testing icon search...");
            String searchQuery = "tennis";
            JSONObject searchResult = client.searchIcons(searchQuery);
            System.out.println("Search results for '" + searchQuery + "':");
            System.out.println(searchResult.toString(2));  // Pretty print JSON
            
            // Test icon download (if search returned results)
            if (searchResult.has("icons") && searchResult.getJSONArray("icons").length() > 0) {
                String iconUrl = searchResult.getJSONArray("icons")
                    .getJSONObject(0)
                    .getString("preview_url");
                
                System.out.println("\nTesting icon download...");
                System.out.println("Downloading icon from: " + iconUrl);
                byte[] iconData = client.downloadIcon(iconUrl);
                System.out.println("Successfully downloaded icon: " + iconData.length + " bytes");
            }
            
            System.out.println("\nAll tests completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Error during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 