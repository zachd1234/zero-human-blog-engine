package com.zho.services;

import com.google.ads.googleads.lib.GoogleAdsClient;
import com.google.ads.googleads.v18.services.GenerateKeywordIdeasRequest;
import com.google.ads.googleads.v18.services.KeywordPlanIdeaServiceClient;
import com.google.ads.googleads.v18.services.KeywordSeed;
import com.google.ads.googleads.v18.enums.KeywordPlanNetworkEnum.KeywordPlanNetwork;
import com.zho.model.BlogRequest;
import com.zho.api.OpenAIClient;
import com.zho.model.Topic;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class KeywordResearchService {
    private final GoogleAdsClient googleAdsClient;
    private static final long CUSTOMER_ID = 4878872709L;
    private final List<String> categories;
    private final DatabaseService databaseService;
    private final OpenAIClient openAIClient;

    public KeywordResearchService() {
        try {
            this.googleAdsClient = GoogleAdsClient.newBuilder()
                .fromPropertiesFile(new File("src/main/resources/application.properties"))
                .build();
            
            this.databaseService = new DatabaseService();
            this.categories = databaseService.getTopics().stream()
                .map(Topic::getTitle)
                .collect(Collectors.toList());
            this.openAIClient = new OpenAIClient();
                
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
            System.out.println("Response class: " + response.getClass().getName());
            
            List<String> keywords = new ArrayList<>();
            
            response.iterateAll().forEach(result -> {
                System.out.println("Result class: " + result.getClass().getName());
                keywords.add(result.getText());
                // Only print the first result's class
                if (keywords.size() == 1) {
                    System.out.println("First result methods: ");
                    Arrays.stream(result.getClass().getMethods())
                        .forEach(method -> System.out.println("- " + method.getName()));
                }
            });

            return keywords;

        } catch (Exception e) {
            System.err.println("Error generating keywords: " + e.getMessage());
            throw new RuntimeException("Failed to generate keywords", e);
        }
    }

    private List<String> generateSeedKeywords(BlogRequest blogRequest) {
        List<String> seedKeywords = new ArrayList<>();
        
        // Use ChatGPT to generate a comprehensive seed keyword
        String prompt = String.format(
            "Convert this topic '%s' into a broad search phrase that someone might " +
            "use when looking for this topic." +
            "Don't use special characters or quotes. Just return the phrase itself.",
            blogRequest.getTopic()
        );
        
        String enhancedSeedKeyword = openAIClient.callOpenAI(prompt);
        seedKeywords.add(enhancedSeedKeyword);
        
        // Use categories from database
        for (String category : this.categories) {
            seedKeywords.add(category + " " + blogRequest.getTopic());
        }
        
        
        // Limit to 7 seed keywords
        return seedKeywords.stream()
            .limit(7)
            .collect(Collectors.toList());
    }

    public static void main(String[] args) {
        KeywordResearchService service = new KeywordResearchService();
        
        // Create a test BlogRequest
        BlogRequest request = new BlogRequest("coffee brewing","A blog about coffee brewing");
        
        try {
            List<String> keywords = service.getLongTailKeywords(request, 1000);
            
            System.out.println("Found " + keywords.size() + " keywords:");
            keywords.stream()
                .limit(20) // Show first 20 keywords
                .forEach(System.out::println);
                
        } catch (Exception e) {
            System.err.println("Error in main: " + e.getMessage());
            e.printStackTrace();
        }
    }
}