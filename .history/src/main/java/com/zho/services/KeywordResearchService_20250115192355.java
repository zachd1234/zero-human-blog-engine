package com.zho.services;

import com.google.ads.googleads.lib.GoogleAdsClient;
import com.google.ads.googleads.v18.services.*;
import com.google.ads.googleads.v18.enums.KeywordPlanNetworkEnum.KeywordPlanNetwork;
import com.zho.model.BlogRequest;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class KeywordResearchService {
    private final GoogleAdsClient googleAdsClient;
    private static final long CUSTOMER_ID = 4878872709L;
    private final List<String> categories;

    public KeywordResearchService(CategoryRepository categoryRepository) {
        try {
            this.googleAdsClient = GoogleAdsClient.newBuilder()
                .fromPropertiesFile(new File("src/main/resources/application.properties"))
                .build();
            
            // Load categories from database
            this.categories = categoryRepository.findAll()
                .stream()
                .map(Category::getName)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            System.err.println("Error initializing service: " + e.getMessage());
            throw new RuntimeException("Failed to initialize service", e);
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
        List<String> seedKeywords = new ArrayList<>();
        
        // Add the main topic as first seed keyword
        seedKeywords.add(blogRequest.getTopic());
        
        // Use categories from database instead of BlogRequest
        for (String category : this.categories) {
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