package com.zho.services;

import com.google.ads.googleads.lib.GoogleAdsClient;
import com.google.ads.googleads.v18.services.GenerateKeywordIdeasRequest;
import com.google.ads.googleads.v18.services.KeywordSeed;
import com.google.ads.googleads.v18.services.KeywordPlanIdeaServiceClient;
import com.google.ads.googleads.v18.enums.KeywordPlanNetworkEnum.KeywordPlanNetwork;
import java.io.File;

public class KeywordResearchService {
    private GoogleAdsClient googleAdsClient;
    private static final long CUSTOMER_ID = 4878872709L;

    public KeywordResearch() {
        try {
            this.googleAdsClient = GoogleAdsClient.newBuilder()
                .fromPropertiesFile(new File("src/main/resources/google-ads.properties"))
                .build();
            
            KeywordPlanIdeaServiceClient service = 
                googleAdsClient.getLatestVersion().createKeywordPlanIdeaServiceClient();

            GenerateKeywordIdeasRequest request = GenerateKeywordIdeasRequest.newBuilder()
                .setCustomerId(String.valueOf(CUSTOMER_ID))
                .setLanguage("en")
                .addGeoTargetConstants("2840")
                .setKeywordPlanNetwork(KeywordPlanNetwork.GOOGLE_SEARCH)
                .setKeywordSeed(KeywordSeed.newBuilder().addKeywords("digital marketing").build())
                .build();

            System.out.println("Sending keyword request...");
            var response = service.generateKeywordIdeas(request);
            
            System.out.println("\nKeyword Ideas:");
            response.iterateAll().forEach(result -> {
                System.out.printf("Keyword: %s, Monthly Searches: %d%n", 
                    result.getText(), 
                    result.getKeywordIdeaMetrics().getAvgMonthlySearches());
            });

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new KeywordResearch();
    }
}