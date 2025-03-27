package com.zho.services.DailyPosts;

import com.google.ads.googleads.lib.GoogleAdsClient;
import com.google.ads.googleads.v18.services.GenerateKeywordIdeasRequest;
import com.google.ads.googleads.v18.services.KeywordPlanIdeaServiceClient;
import com.google.ads.googleads.v18.services.KeywordSeed;
import com.google.ads.googleads.v18.enums.KeywordPlanNetworkEnum.KeywordPlanNetwork;
import com.zho.model.BlogRequest;
import com.zho.api.OpenAIClient;
import com.zho.model.Topic;
import com.zho.services.DatabaseService;
import com.zho.model.KeywordAnalysis;
import com.google.ads.googleads.v18.services.GenerateKeywordIdeaResult;
import com.google.ads.googleads.v18.common.KeywordPlanHistoricalMetrics;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;

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
            int analysisLimit = keywordCount;  // Should be 1000 for production 
            
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
               - Specific long-tail keywords usually have less competition
               - Generic, short keywords usually have more competition

            2. Search Intent Match (1-10):
               - How well can a blog post satisfy this search?
               - 1 = poor match, 10 = perfect match for a blog post
            
            3. Relevance Rating:
               - Rate 0 if ANY of these are true:
                 * Location-specific query (e.g., "near me", "in New York")
                 * In a different language
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
        System.out.println("\nRaw AI Response for keyword '" + keyword.getText() + "':");
        System.out.println("----------------------------------------");
        System.out.println(analysis);
        System.out.println("----------------------------------------");


        // Parse AI response and create KeywordAnalysis object
        double score = parseAIResponse(analysis);
        
        return new KeywordAnalysis(
            keyword.getText(),
            keyword.getKeywordIdeaMetrics().getAvgMonthlySearches(),
            keyword.getKeywordIdeaMetrics().getCompetitionIndex(),
            keyword.getKeywordIdeaMetrics().getAverageCpcMicros() / 1_000_000.0,
            score,
            analysis, -1
        );
    }

    private List<String> generateSeedKeywords(BlogRequest blogRequest) throws IOException {
        List<String> seedKeywords = new ArrayList<>();
        
        seedKeywords.add(blogRequest.getTopic());
        
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
            blogRequest.getTopic()
        );
        
        String pillarsResponse = openAIClient.callGPT4(pillarTopicsPrompt);
        Arrays.stream(pillarsResponse.split("\n"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .limit(5)
            .forEach(seedKeywords::add);
        
        // Print seed keywords for testing
        System.out.println("\nGenerated Seed Keywords:");
        System.out.println("Core theme: " + blogRequest.getTopic());
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
                line = line.trim();
                if (line.startsWith("Competition:")) {
                    competition = Double.parseDouble(line.substring("Competition:".length()).trim());
                }
                if (line.startsWith("Search Intent:")) {
                    searchIntent = Double.parseDouble(line.substring("Search Intent:".length()).trim());
                }
                if (line.startsWith("Relevance:")) {
                    relevance = Double.parseDouble(line.substring("Relevance:".length()).trim());
                }
            }
            
            if (competition == null || searchIntent == null || relevance == null) {
                System.err.println("Missing required scores in response");
                return 0.0;
            }
            
            // If not relevant, return 0
            if (relevance < 5) {
                return 0.0;
            }
            
            // Average of competition and search intent scores
            return (competition + searchIntent) / 2.0;
            
        } catch (Exception e) {
            System.err.println("Error parsing AI response: " + e.getMessage());
            return 0.0;
        }
    }

    public List<KeywordAnalysis> analyzeKeywordsFromCsv(String csvFilePath, BlogRequest blogRequest) {
        List<KeywordAnalysis> analyzedKeywords = new ArrayList<>();
        
        try {
            System.out.println("\n📂 Reading keywords from CSV: " + csvFilePath);
            
            // Read keywords from CSV
            List<String> rawKeywords = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        rawKeywords.add(line.trim());
                    }
                }
            }
            System.out.println("📊 Found " + rawKeywords.size() + " raw keywords in CSV");
            
            // Refine the keyword list
            System.out.println("\n🔄 Starting keyword refinement process...");
            List<String> refinedKeywords = keywordListRefinement(rawKeywords, blogRequest);
            System.out.println("\n✨ Processing " + refinedKeywords.size() + " refined keywords...");
            
            // Create KeywordAnalysis objects
            System.out.println("\n📝 Creating keyword analysis objects...");
            for (int i = 0; i < refinedKeywords.size(); i++) {
                String keyword = refinedKeywords.get(i).trim();
                if (!keyword.isEmpty()) {
                    System.out.printf("Processing keyword %d/%d: %s%n", i + 1, refinedKeywords.size(), keyword);
                    
                    GenerateKeywordIdeaResult mockResult = GenerateKeywordIdeaResult.newBuilder()
                        .setText(keyword)
                        .setKeywordIdeaMetrics(KeywordPlanHistoricalMetrics.newBuilder()
                            .setAvgMonthlySearches(0L)
                            .setCompetitionIndex(0)
                            .setAverageCpcMicros(0L)
                            .build())
                        .build();
                    
                    KeywordAnalysis analysis = new KeywordAnalysis(
                        keyword,
                        0L,
                        0,
                        0.0,
                        9.0,
                        "Pre-filtered and ranked keyword",
                        refinedKeywords.size() - i
                    );
                    
                    analyzedKeywords.add(analysis);
                }
            }
            
            // Batch save to database
            if (!analyzedKeywords.isEmpty()) {
                System.out.println("\n💾 Saving " + analyzedKeywords.size() + " keywords to database...");
                databaseService.saveKeywords(analyzedKeywords);
                System.out.println("✅ Database update complete!");
            }
            
            System.out.println("\n🎉 Analysis Complete!");
            System.out.println("Final keyword count: " + analyzedKeywords.size());
            
        } catch (Exception e) {
            System.err.println("\n❌ Error processing CSV: " + e.getMessage());
            e.printStackTrace();
        }
        
        return analyzedKeywords;
    }

    private List<String> keywordListRefinement(List<String> rawKeywords, BlogRequest blogRequest) {
        int maxRetries = 3;
        int currentTry = 0;
        
        while (currentTry < maxRetries) {
            try {
                System.out.println("\n🔍 Starting Keyword Refinement Process");
                System.out.println("Initial keyword count: " + rawKeywords.size());
                
                // Step 1: Filter out excluded keywords
                System.out.println("\n📋 STEP 1: Filtering excluded keywords...");
                String filterPrompt = String.format(
                    "Analyze the following list of keywords and return ONLY the ones that should be KEPT, removing any that meet the following exclusion criteria:\n\n" +
                    "1️⃣ Location-Specific Queries → Keywords that contain a city, state, country, or phrases like \"near me\"\n" +
                    "2️⃣ Non-English Keywords → Any keyword not in English or containing foreign language words\n" +
                    "3️⃣ Navigational Queries → Keywords where the user is trying to access a specific website or brand\n" +
                    "4️⃣ Off-Topic Queries → Keywords that are off topic from the niche of %s\n\n" +
                    "Keywords:\n%s",
                    blogRequest.getTopic(),
                    String.join("\n", rawKeywords)
                );
                
                String filteredKeywords = openAIClient.callO3(filterPrompt);
                List<String> filteredList = Arrays.asList(filteredKeywords.split("\n"));
                System.out.println("✅ Filtering complete");
                System.out.println("Keywords removed: " + (rawKeywords.size() - filteredList.size()));
                System.out.println("Keywords remaining: " + filteredList.size());
                
                // Step 2: Remove duplicates
                System.out.println("\n📋 STEP 2: Removing duplicate keywords...");
                String dedupePrompt = String.format(
                    "Take this list of keywords and remove any that are duplicates OR semantically identical.\n" +
                    "• Consider keywords identical if they have the same meaning even if phrased differently.\n" +
                    "• Keep only the most natural and commonly used phrasing while removing redundant versions.\n" +
                    "• Do not remove keywords that are slightly different in intent.\n" +
                    "• Return the cleaned list of unique, high-value keywords.\n" +
                    "• Each keyword should be on its own line (no bullets, no extra formatting).\n\n" +
                    "Keywords:\n%s",
                    String.join("\n", filteredList)
                );
                
                String uniqueKeywords = openAIClient.callO3(dedupePrompt);
                List<String> uniqueList = Arrays.asList(uniqueKeywords.split("\n"));
                System.out.println("✅ Deduplication complete");
                System.out.println("Duplicates removed: " + (filteredList.size() - uniqueList.size()));
                System.out.println("Keywords remaining: " + uniqueList.size());
                
                // Step 3: Rank keywords
                System.out.println("\n📋 STEP 3: Ranking keywords...");
                String rankPrompt = String.format(
                    "Given the following list of keywords, assign a ranking score (0-100) to each based on:\n" +
                    "• Expected competition (lower competition = higher score)\n" +
                    "• How well we can create a high-quality blog post that matches search intent (better fit = higher score)\n\n" +
                    "Return a single list of keywords, ordered from highest to lowest score.\n" +
                    "Do not include scores or justifications—only the sorted keywords.\n\n" +
                    "Keywords:\n%s",
                    String.join("\n", uniqueList)
                );
                
                String rankedKeywords = openAIClient.callO3(rankPrompt);
                List<String> rankedList = Arrays.asList(rankedKeywords.split("\n"));
                System.out.println("✅ Ranking complete");
                System.out.println("Final keyword count: " + rankedList.size());
                
                return rankedList;
                
            } catch (Exception e) {
                currentTry++;
                if (currentTry >= maxRetries) {
                    System.err.println("❌ Error in keyword refinement after " + maxRetries + " retries: " + e.getMessage());
                    e.printStackTrace();
                } else {
                    System.out.println("\n⚠️ Timeout occurred, retrying... (Attempt " + (currentTry + 1) + " of " + maxRetries + ")");
                    try {
                        Thread.sleep(2000 * currentTry); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        return new ArrayList<>();
    }

    public static void main(String[] args) {
        KeywordResearchService service = new KeywordResearchService();
        
        // Create a test BlogRequest
        BlogRequest request = new BlogRequest("PIPA mitigation strategies", "PIPA mitigation strategies in texas");
        
        try {
            // Use the actual path to your CSV file
            List<KeywordAnalysis> keywords = service.analyzeKeywordsFromCsv("/Users/zachderhake/Downloads/phaseIESA.csv", request);

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
}