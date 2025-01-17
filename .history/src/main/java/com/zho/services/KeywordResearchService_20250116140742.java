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
import com.google.ads.googleads.v18.services.GenerateKeywordIdeaResult;

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

    public List<KeywordAnalysis> getLongTailKeywords(BlogRequest blogRequest, int keywordCount) {
        try {
            // Generate seed keywords based on blog topic and categories
            List<String> seedKeywords = generateSeedKeywords(blogRequest);
            
            // Print seed keywords
            System.out.println("\nUsing seed keywords:");
            seedKeywords.forEach(seed -> System.out.println("- " + seed));
            System.out.println();
            
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

            // Get response and collect keywords with their metrics
            var response = service.generateKeywordIdeas(request);
            List<GenerateKeywordIdeaResult> allKeywords = new ArrayList<>();
            
            response.iterateAll().forEach(allKeywords::add);

            // Second part: Filter and analyze keywords
            PriorityQueue<KeywordAnalysis> analyzedKeywords = new PriorityQueue<>();

            // Filter keywords with competition index < 30
            allKeywords.stream()
                .filter(keyword -> keyword.getKeywordIdeaMetrics().getCompetitionIndex() < 30)
                .forEach(keyword -> {
                    KeywordAnalysis analysis = analyzeKeywordWithAI(keyword);
                    if (analysis.getScore() >= MINIMUM_SCORE_THRESHOLD) {
                        analyzedKeywords.add(analysis);
                    }
                });

            // Convert PriorityQueue to List
            return analyzedKeywords.stream()
                .limit(keywordCount)
                .collect(Collectors.toList());
            

        } catch (Exception e) {
            System.err.println("Error generating keywords: " + e.getMessage());
            throw new RuntimeException("Failed to generate keywords", e);
        }
    }

    private static final double MINIMUM_SCORE_THRESHOLD = 7.0; // Out of 10

    private KeywordAnalysis analyzeKeywordWithAI(GenerateKeywordIdeaResult keyword) {
        String prompt = String.format("""
            Analyze this keyword: '%s'
            
            Please rate this keyword on a scale of 1-10 for each criterion:
            1. Search Intent Clarity: How clear is the user's intent when searching this?
            2. Content Potential: How much valuable content could be created for this?
            3. Conversion Potential: How likely are searchers to convert/engage?
            4. Long-term Value: Will this topic remain relevant over time?
            
            Provide ratings and brief explanations in a structured format.
            Finally, give an overall score (average of all ratings).
            """, 
            keyword.getText());

        String analysis = openAIClient.callOpenAI(prompt);
        
        // Parse AI response and create KeywordAnalysis object
        double score = parseAIResponse(analysis);
        
        return new KeywordAnalysis(
            keyword.getText(),
            keyword.getKeywordIdeaMetrics().getAvgMonthlySearches(),
            keyword.getKeywordIdeaMetrics().getCompetitionIndex(),
            keyword.getKeywordIdeaMetrics().getAverageCpcMicros() / 1_000_000.0,
            score,
            analysis
        );
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

    private double parseAIResponse(String analysis) {
        // TODO: Implement parsing logic to extract the overall score
        // For now, returning a dummy score
        return 8.0;
    }

    public static void main(String[] args) {
        KeywordResearchService service = new KeywordResearchService();
        
        // Create a test BlogRequest
        BlogRequest request = new BlogRequest("coffee brewing","A blog about coffee brewing");
        
        try {
            List<GenerateKeywordIdeaResult> keywords = service.getLongTailKeywords(request, 1000);
            
            System.out.println("Found " + keywords.size() + " keywords:");
            System.out.println("\nFormat: Keyword | Monthly Searches | Competition | Competition Index | Avg CPC (USD)");
            System.out.println("-------------------------------------------------------------------------------------");
            
            keywords.stream()
                .limit(20) // Show first 20 keywords
                .forEach(result -> {
                    var metrics = result.getKeywordIdeaMetrics();
                    System.out.printf("%-30s | %14d | %11s | %16d | $%.2f%n",
                        result.getText(),
                        metrics.getAvgMonthlySearches(),
                        metrics.getCompetition(),
                        metrics.getCompetitionIndex(),
                        metrics.getAverageCpcMicros() / 1_000_000.0  // Convert micros to dollars
                    );
                });
                
        } catch (Exception e) {
            System.err.println("Error in main: " + e.getMessage());
            e.printStackTrace();
        }
    }

// New class to hold keyword analysis results
class KeywordAnalysis implements Comparable<KeywordAnalysis> {
    private final String keyword;
    private final long monthlySearches;
    private final long competitionIndex;
    private final double averageCpc;
    private final double score;
    private final String aiAnalysis;

    public KeywordAnalysis(String keyword, long monthlySearches, 
                          long competitionIndex, double averageCpc, 
                          double score, String aiAnalysis) {
        this.keyword = keyword;
        this.monthlySearches = monthlySearches;
        this.competitionIndex = competitionIndex;
        this.averageCpc = averageCpc;
        this.score = score;
        this.aiAnalysis = aiAnalysis;
    }

    @Override
    public int compareTo(KeywordAnalysis other) {
         // Sort by score in descending order
         return Double.compare(other.score, this.score);
        }
    
        // Getters
        public String getKeyword() { return keyword; }
        public long getMonthlySearches() { return monthlySearches; }
        public long getCompetitionIndex() { return competitionIndex; }
        public double getAverageCpc() { return averageCpc; }
        public double getScore() { return score; }
        public String getAiAnalysis() { return aiAnalysis; }
    }
}