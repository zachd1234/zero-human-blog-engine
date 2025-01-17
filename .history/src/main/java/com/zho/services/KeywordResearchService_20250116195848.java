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
import java.io.IOException;

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
            int analysisLimit = 20;  // Limit for testing
            
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

    private static final double MINIMUM_SCORE_THRESHOLD = 7.0; // Out of 10

    private KeywordAnalysis analyzeKeywordWithAI(GenerateKeywordIdeaResult keyword, String blogTopic) {
        String prompt = String.format("""
            Analyze this keyword: '%s'
            
            Provide three ratings for this keyword for a new blog about %s:

            1. Competition Rating (1-10):
               - How difficult would it be for a new blog to rank for this?
               - 1 = many established websites targeting this exact keyword
               - 10 = few or no websites targeting this specific phrase
               - Unique, less obvious, long-tail keywords usually have less competition
               - Generic, short keywords usually have more competition

            2. Search Intent Match (1-10):
               - How well can a blog post satisfy this search?
               - 1 = poor match, 10 = perfect match for a blog post
            
            3. Relevance Rating:
               - Rate 0 if ANY of these are true:
                 * Location-specific query (e.g., "near me", "in New York")
                 * Navigational query (user wants to go to specific website, e.g., "chatgpt login", "facebook")
                 * Looking for a specific product to buy
               - Rate 10 if NONE of the above are true
            
            Format your response EXACTLY like this:
            
            Competition: [1-10]
            Search Intent: [1-10]
            Relevance: [0 or 10]

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

    private List<String> generateSeedKeywords(BlogRequest blogRequest) throws IOException {
        List<String> seedKeywords = new ArrayList<>();
        
        // Step 1: Generate core theme keyword
        String coreThemePrompt = String.format(
            """
            Based on this blog topic '%s' and description '%s',
            generate a single, broad keyword that represents the core theme of the blog.
            
            Return only the keyword itself, no explanation or additional text.
            """,
            blogRequest.getTopic(),
            blogRequest.getDescription()
        );
        
        String coreKeyword = openAIClient.callGPT4(coreThemePrompt).trim();
        seedKeywords.add(coreKeyword);
        
        // Step 2: Generate pillar topics based on core keyword
        String pillarTopicsPrompt = String.format(
            """
            Generate 5 pillar topics for a blog about '%s'.
            These should be broad, high-level topics suitable for in-depth content.
            Each topic should be a keyword or short phrase that people actually search for.
            
            Format each topic as a search query:
            - Use natural search patterns (how people type in Google)
            - Put the main topic first
            Examples:
            ❌ "Benefits of green tea" → ✅ "Green tea benefits"
            ❌ "Guide to SEO" → ✅ "SEO guide"
            ❌ "Types of phase 1 ESA" → ✅ "Phase 1 ESA types"

            Format your response as a simple list of 5 topics, one per line.
            Do not add numbers, bullets, or any other text.
            """,
            coreKeyword
        );
        
        String pillarsResponse = openAIClient.callGPT4(pillarTopicsPrompt);
        Arrays.stream(pillarsResponse.split("\n"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .limit(5)
            .forEach(seedKeywords::add);
        
        // Print seed keywords for testing
        System.out.println("\nGenerated Seed Keywords:");
        System.out.println("Core theme: " + coreKeyword);
        System.out.println("Pillar topics:");
        seedKeywords.stream()
            .skip(1) // Skip core keyword as we already printed it
            .forEach(keyword -> System.out.println("- " + keyword));
        System.out.println();
        
        return seedKeywords;
    }

    private double parseAIResponse(String analysis) {
        try {
            String[] lines = analysis.split("\n");
            Double competition = null;
            Double searchIntent = null;
            Double relevance = null;
            
            for (String line : lines) {
                if (line.matches("Competition: \\d+")) {
                    String number = line.replaceAll(".*: (\\d+).*", "$1");
                    competition = Double.parseDouble(number);
                }
                if (line.matches("Search Intent: \\d+")) {
                    String number = line.replaceAll(".*: (\\d+).*", "$1");
                    searchIntent = Double.parseDouble(number);
                }
                if (line.matches("Relevance: \\d+")) {
                    String number = line.replaceAll(".*: (\\d+).*", "$1");
                    relevance = Double.parseDouble(number);
                }
            }
            
            // Check if we have all scores
            if (competition == null || searchIntent == null || relevance == null) {
                System.err.println("Missing scores in AI response: " + analysis);
                return 0.0;
            }
            
            // If not relevant, return 0
            if (relevance == 0) {
                return 0.0;
            }
            
            double competitionScore = competition;
            
            // Average of competition and search intent scores
            return (competitionScore + searchIntent) / 2.0;
            
        } catch (Exception e) {
            System.err.println("Error parsing AI response: " + e.getMessage());
            return 0.0;
        }
    }

    public static void main(String[] args) {
        KeywordResearchService service = new KeywordResearchService();
        
        // Create a test BlogRequest
        BlogRequest request = new BlogRequest("boxing", "A blog teaching begineers how to box");
        
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