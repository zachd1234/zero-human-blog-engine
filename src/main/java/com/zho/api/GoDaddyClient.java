package com.zho.api;

import com.zho.config.ConfigManager;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.hc.core5.http.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GoDaddyClient {
    private final String apiKey;
    private final String apiSecret;
    private final CloseableHttpClient httpClient;
    private final String baseUrl;

    public GoDaddyClient() {
        // Load from ConfigManager
        this.apiKey = ConfigManager.getGoDaddyApiKey();
        this.apiSecret = ConfigManager.getGoDaddyApiSecret();
        this.httpClient = HttpClients.createDefault();
        
        // Normalize the base URL to ensure proper format
        String url = ConfigManager.getGoDaddyApiUrl();
        if (!url.endsWith("/")) {
            url += "/";
        }
        if (!url.contains("/v1/")) {
            url = url.replaceFirst("/$", "/v1/");
        }
        this.baseUrl = url;
        
        // Validate credentials
        if (this.apiKey == null || this.apiSecret == null) {
            throw new IllegalStateException("GoDaddy API credentials not found in configuration");
        }
        
        System.out.println("Initialized GoDaddy client with URL: " + this.baseUrl);
    }

    public List<String> suggestDomains(String searchTerm) throws IOException, ParseException {
        String url = baseUrl + "domains/suggest?query=" + searchTerm + "&limit=5";
        System.out.println("Calling GoDaddy API for domain suggestions: " + url);
        
        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", "sso-key " + apiKey + ":" + apiSecret);
        System.out.println("Using API Key: " + apiKey); // Debug line
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getCode();
            System.out.println("Response status code: " + statusCode); // Debug line
            
            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity);
            System.out.println("GoDaddy API Response: " + responseBody);
            
            // Check if response is an error
            if (responseBody.contains("code") && responseBody.contains("message")) {
                JSONObject error = new JSONObject(responseBody);
                throw new IOException(String.format("GoDaddy API error (Status: %d): %s", statusCode, error.getString("message")));
            }
            
            JSONArray suggestions = new JSONArray(responseBody);
            List<String> domains = new ArrayList<>();
            
            for (int i = 0; i < suggestions.length() && i < 5; i++) {
                JSONObject suggestion = suggestions.getJSONObject(i);
                domains.add(suggestion.getString("domain"));
            }
            
            return domains;
        }
    }

    public boolean isDomainAvailable(String domain) throws IOException, ParseException {
        String url = baseUrl + "domains/available?domain=" + domain;
        System.out.println("Checking domain availability: " + url);
        
        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", "sso-key " + apiKey + ":" + apiSecret);

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity);
            System.out.println("Domain availability response: " + responseBody);
            
            JSONObject availability = new JSONObject(responseBody);
            return availability.getBoolean("available");
        }
    }

    public List<String> suggestAndFilterDomains(String searchTerm, double maxPrice) throws IOException, ParseException {
        List<String> filteredDomains = new ArrayList<>();
        
        // Step 1: Get domain suggestions
        List<String> suggestions = suggestDomains(searchTerm);
        
        // Step 2: Check availability and filter by price
        for (String domain : suggestions) {
            boolean isAvailable = isDomainAvailable(domain);
            double price = getDomainPrice(domain); // Retrieve the price
            
            // Convert maxPrice to cents for comparison
            double adjustedMaxPrice = maxPrice * 1000000;
            
            // Check if the domain is available and within the price limit
            if (isAvailable && price <= adjustedMaxPrice) {
                filteredDomains.add(domain);
            }
        }
        
        return filteredDomains;
    }

    public double getDomainPrice(String domain) throws IOException, ParseException {
        String url = String.format("%s/domains/available?domain=%s", baseUrl, domain);
        System.out.println("Checking domain price for: " + url);
        
        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", String.format("sso-key %s:%s", apiKey, apiSecret));

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity);
            System.out.println("Domain price response: " + responseBody); // Debugging line
            
            // Parse the response to get the price
            JSONObject availability = new JSONObject(responseBody);
            
            // Check if the domain is available
            if (availability.getBoolean("available")) {
                // Assuming the price is available in the response
                // Adjust this line based on the actual API response structure
                return availability.getDouble("price"); // Ensure this is the correct field
            } else {
                return -1; // Domain is not available
            }
        }
    }

    //Main method for testing
    public static void main(String[] args) {
        GoDaddyClient goDaddyClient = new GoDaddyClient();

        // Test domain suggestions
        String searchTerm = "rucking"; // Replace with a relevant search term
        double maxDollars = 30; // Set a maximum price for filtering

        try {
            // Test filtered domain suggestions
            List<String> filteredDomains = goDaddyClient.suggestAndFilterDomains(searchTerm, maxDollars);
            System.out.println("Filtered Domain Suggestions for '" + searchTerm + "' under $" + maxDollars + ":");
            for (String domain : filteredDomains) {
                System.out.println(" - " + domain);
            }

            // Test domain availability and price for the first filtered suggestion
            if (!filteredDomains.isEmpty()) {
                String firstFilteredDomain = filteredDomains.get(0);
                boolean isAvailable = goDaddyClient.isDomainAvailable(firstFilteredDomain);
                double price = goDaddyClient.getDomainPrice(firstFilteredDomain);
                System.out.println("Is the domain '" + firstFilteredDomain + "' available? " + isAvailable);
                System.out.println("Price for the domain '" + firstFilteredDomain + "': $" + price);
            } else {
                System.out.println("No filtered domain suggestions found.");
            }
        } catch (IOException | ParseException e) {
            System.err.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}