package com.zho.services;

import com.google.ads.googleads.lib.GoogleAdsClient;
import com.google.ads.googleads.v18.services.*;
import com.google.ads.googleads.v18.enums.KeywordPlanNetworkEnum.KeywordPlanNetwork;
import com.zho.dto.BlogRequest;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class KeywordResearchService {
    private final GoogleAdsClient googleAdsClient;
    private static final long CUSTOMER_ID = 4878872709L;

    public KeywordResearchService() {
        try {
            this.googleAdsClient = GoogleAdsClient.newBuilder()
                .fromPropertiesFile(new File("src/main/resources/application.properties"))
                .build();
        } catch (Exception e) {
            System.err.println("Error initializing Google Ads client: " + e.getMessage());
            throw new RuntimeException("Failed to initialize Google Ads client", e);
        }
    }

    public List<String> getLongTailKeywords(BlogRequest blogRequest, int keywordCount) {
        try {
            // Generate seed keywords based on blog topic and categories
            List<String> seedKeywords = generateSeedKeywords(blogRequest);
            
            // Get keyword ideas using Google Ads API
            KeywordPlanIdeaServiceClient service = 
                googleAdsClient.getLatestVersion().createKeywordPlanIdeaServiceClient();

            // Build the request with seed keywords
            GenerateKeywordIdeasRequest request = GenerateKeywordIdeasRequest.newBuilder()
                .setCustomerId(String.valueOf(CUSTOMER_ID))
                .setLanguage("languageConstants/1000")
                .addGeoTargetConstants("geoTargetConstants/2840")
                .setKeywordPlanNetwork(KeywordPlanNetwork.GOOGLE_SEARCH)
                .setKeywordSeed(KeywordSeed.newBuilder()
                    .addAllKeywords(seedKeywords)
                    .build())
                .build();

            // Get response and collect keywords
            var response = service.generateKeywordIdeas(request);
            List<String> keywords = new ArrayList<>();
            
            response.iterateAll().forEach(result -> {
                keywords.add(result.getText());
            });

            return keywords;

        } catch (Exception e) {
            System.err.println("Error generating keywords: " + e.getMessage());
            throw new RuntimeException("Failed to generate keywords", e);
        }
    }

    private List<String> generateSeedKeywords(BlogRequest blogRequest) {
        // Generate 5 broad seed keywords based on the blog topic and categories
        List<String> seedKeywords = new ArrayList<>();
        
        // Add the main topic as first seed keyword
        seedKeywords.add(blogRequest.getTopic());
        
        // Add variations based on categories
        for (String category : blogRequest.getCategories()) {
            seedKeywords.add(category + " " + blogRequest.getTopic());
        }
        
        // If we have less than 5 seeds, add some generic variations
        if (seedKeywords.size() < 5) {
            seedKeywords.add("how to " + blogRequest.getTopic());
            seedKeywords.add("best " + blogRequest.getTopic());
            seedKeywords.add(blogRequest.getTopic() + " guide");
        }
        
        // Limit to 5 seed keywords
        return seedKeywords.stream()
            .limit(5)
            .collect(Collectors.toList());
    }
}