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
            // First part: Get keywords from Google Ads API (existing code)
            List<String> seedKeywords = generateSeedKeywords(blogRequest);
            
            System.out.println("\nUsing seed keywords:");
            seedKeywords.forEach(seed -> System.out.println("- " + seed));
            System.out.println();
            
            KeywordPlanIdeaServiceClient service = 
                googleAdsClient.getLatestVersion().createKeywordPlanIdeaServiceClient();

            GenerateKeywordIdeasRequest request = GenerateKeywordIdeasRequest.newBuilder()
                .setCustomerId(String.valueOf(CUSTOMER_ID))
                .setLanguage("languageConstants/1000")
                .addGeoTargetConstants("geoTargetConstants/2840")
                .setKeywordPlanNetwork(KeywordPlanNetwork.GOOGLE_SEARCH)
                .setKeywordSeed(KeywordSeed.newBuilder()
                    .addAllKeywords(seedKeywords)
                    .build())
                .build();

            var response = service.generateKeywordIdeas(request);
            List<GenerateKeywordIdeaResult> allKeywords = new ArrayList<>();
            response.iterateAll().forEach(allKeywords::add);

            System.out.println("Total keywords from Google Ads: " + allKeywords.size());

            // Second part: Filter and analyze keywords
            PriorityQueue<KeywordAnalysis> analyzedKeywords = new PriorityQueue<>();
            int lowCompetitionCount = 0;
            int analysisLimit = 50;  // Limit for testing
            
            // Filter keywords with competition index < 30
            for (GenerateKeywordIdeaResult keyword : allKeywords) {
                if (keyword.getKeywordIdeaMetrics().getCompetitionIndex() < 30) {
                    lowCompetitionCount++;
                    
                    if (lowCompetitionCount > analysisLimit) {
                        System.out.println("\nReached analysis limit of " + analysisLimit + " keywords");
                        break;
                    }
                    
                    System.out.println("\nAnalyzing keyword " + lowCompetitionCount + "/" + analysisLimit + ": " + keyword.getText());
                    KeywordAnalysis analysis = analyzeKeywordWithAI(keyword, blogRequest.getTopic());
                    if (analysis.getScore() >= MINIMUM_SCORE_THRESHOLD) {
                        analyzedKeywords.add(analysis);
                    }
                }
            }

            System.out.println("\nSummary:");
            System.out.println("- Total keywords found: " + allKeywords.size());
            System.out.println("- Low competition keywords found: " + lowCompetitionCount);
            System.out.println("- Keywords analyzed: " + Math.min(lowCompetitionCount, analysisLimit));
            System.out.println("- Keywords that passed AI analysis: " + analyzedKeywords.size());

            // Convert PriorityQueue to List
            return analyzedKeywords.stream()
                .limit(keywordCount)
                .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("Error generating keywords: " + e.getMessage());
            throw new RuntimeException("Failed to generate keywords", e);
        }
    }

    private static final double MINIMUM_SCORE_THRESHOLD = 8.0; // Out of 10

    private KeywordAnalysis analyzeKeywordWithAI(GenerateKeywordIdeaResult keyword, String blogTopic) {
        String prompt = String.format("""
            Analyze this keyword: '%s'
            
            Rate from 1-10 how well we can match the search intent for a blog about %s.
            Consider:
            1. What is the user looking for?
            2. Can a blog post satisfy this search intent?
            3. Is the user looking for a specific website, product, or location?
            4. Is this a genuine informational query we can address?
            
            Format your response EXACTLY like this:
            
            Search Intent: [1-10]
            Explanation: [your explanation]
            
            Do not add any additional text or scores.
            """, 
            keyword.getText(),
            blogTopic);

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
        try {
            String[] lines = analysis.split("\n");
            
            for (String line : lines) {
                if (line.matches("Search Intent: \\d+")) {
                    String number = line.replaceAll(".*: (\\d+).*", "$1");
                    return Double.parseDouble(number);
                }
            }
            
            System.err.println("No valid score found in AI response: " + analysis);
            return 0.0;
            
        } catch (Exception e) {
            System.err.println("Error parsing AI response: " + e.getMessage());
            return 0.0;
        }
    }

    public static void main(String[] args) {
        KeywordResearchService service = new KeywordResearchService();
        
        // Create a test BlogRequest
        BlogRequest request = new BlogRequest("coffee brewing","A blog about coffee brewing");
        
        try {
            List<KeywordAnalysis> keywords = service.getLongTailKeywords(request, 1000);
            System.out.println("\nTop Keywords Analysis:");
            System.out.println("Format: Keyword | Monthly Searches | Competition Index | Avg CPC | AI Score");
            System.out.println("--------------------------------------------------------------------------------");
            
            keywords.forEach(k -> System.out.printf("%-30s | %14d | %16d | $%6.2f | %.1f\n",
                k.getKeyword(),
                k.getMonthlySearches(),
                k.getCompetitionIndex(),
                k.getAverageCpc(),
                k.getScore()
            ));
            
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