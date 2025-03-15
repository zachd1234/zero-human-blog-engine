package com.zho.services.DailyPosts;

import com.zho.services.DatabaseService;
import com.zho.services.BlogSetup.SearchConsoleSetupService;
import com.zho.api.wordpress.WordPressPostClient;
import com.zho.api.wordpress.WordPressPostClient.PostResponse;
import com.zho.model.KeywordAnalysis;
import com.zho.model.Site;
import com.zho.model.BlogPost;
import com.zho.api.OpenAIClient;
import com.zho.api.GetImgAIClient;
import java.io.IOException;
import com.zho.model.Image;
import com.zho.api.GoogleSearchConsoleClient;
import com.zho.api.wordpress.WordPressCategoryClient;
import com.zho.api.wordpress.WordPressMediaClient;
import com.zho.api.KoalaAIClient;
import com.zho.model.BlogRequest;
import com.zho.api.BlogPostGeneratorAPI;

import org.checkerframework.common.returnsreceiver.qual.This;
import org.json.JSONObject;

import java.util.List;
import java.util.Arrays;

public class AutoContentWorkflowService {
    private final DatabaseService databaseService;
    private final PostWriterService postWriterService;
    private final  WordPressPostClient wordPressPostClient;
    private final OpenAIClient openAIClient;
    private final GetImgAIClient getImgClient;
    private final WordPressCategoryClient wpCategoryClient;
    private final WordPressMediaClient mediaClient;
    private final BlogPostGeneratorAPI blogPostGeneratorAPI;

    public AutoContentWorkflowService() {
        this.databaseService = new DatabaseService();
        this.postWriterService = new PostWriterService();
        this.wordPressPostClient = new WordPressPostClient();
        this.openAIClient = new OpenAIClient();
        this.getImgClient = new GetImgAIClient();
        this.wpCategoryClient = new WordPressCategoryClient(); 
        this.mediaClient = new WordPressMediaClient();
        this.blogPostGeneratorAPI = new BlogPostGeneratorAPI();
    }
    
    public void processNextKeyword() {
        try {
            // 1. Get next keyword from queue
            KeywordAnalysis keyword = databaseService.popNextKeyword();
            if (keyword == null) {
                System.out.println("No pending keywords found");
                return;
            }

            System.out.println("Processing keyword: " + keyword.getKeyword());
            
            String title = null;
            String content = null;

            // Generate content first
            try {
                content = blogPostGeneratorAPI.generatePost(keyword.getKeyword());
                
                // Extract title and clean content
                if (content != null) {
                    // Look for h1 tag first
                    if (content.contains("<h1>") && content.contains("</h1>")) {
                        int startIndex = content.indexOf("<h1>") + 4;
                        int endIndex = content.indexOf("</h1>");
                        title = content.substring(startIndex, endIndex).trim();
                        // Remove the h1 section from content
                        content = content.substring(endIndex + 5).trim();
                    } else {
                        // Fallback: take first line as title
                        String[] lines = content.split("\\n", 2);
                        title = lines[0].replaceAll("<[^>]+>", "").trim();
                        content = lines.length > 1 ? lines[1].trim() : content;
                    }
                }

                if (title == null || title.isEmpty()) {
                    throw new Exception("Could not extract title from content");
                }
            } catch (Exception e) {
                System.out.println("Content generation or title extraction failed: " + e.getMessage());
                databaseService.updateKeywordStatus(Long.valueOf(keyword.getId()), "FAILED");
                return;
            }

            // Only continue if we have content
            if (content == null) {
                System.err.println("No content generated. Skipping post creation.");
                databaseService.updateKeywordStatus(Long.valueOf(keyword.getId()), "FAILED");
                return;
            }

            // 3. Generate image and get category
            String imageDescription = generateCoverImagePrompt(keyword.getKeyword());
            String imageUrl = generateCoverImage(imageDescription);
            
            Image coverImage = new Image(imageUrl);

            String category = determineCategory(keyword.getKeyword());
            System.out.println("Selected category: " + category);
            Integer categoryId = wpCategoryClient.getCategoryId(category);

            String slug = generateUrlSlug(title);
            String metaDescription = generateMetaDescription(keyword.getKeyword(), content);

            // 4. Create and publish post
            BlogPost blogPost = new BlogPost(0, title, content, coverImage, category, categoryId, slug, metaDescription);
            PostResponse postResponse = wordPressPostClient.publishPost(blogPost);
            
            if (blogPost.getCoverImage() != null) {
                mediaClient.updatePostCoverImage(postResponse.getId(), blogPost.getCoverImage());
            }

            // 5. Update database
            databaseService.updateKeywordStatus(Long.valueOf(keyword.getId()), "PUBLISHED");
            databaseService.updateKeywordPostUrl(Long.valueOf(keyword.getId()), postResponse.getUrl());
            
            System.out.println("Successfully published post for: " + keyword.getKeyword());

        } catch (Exception e) {
            System.err.println("Error in content workflow: " + e.getMessage());
        }
    }

    //TO DELETE LATER
    public void PageTesting() {
        String content = "<p>This is a test article with enough content to be considered valid.</p>";
        String title = "Test Article for Indexing1";
        String category = "uncategorized";
        Integer categoryId = 1;
        String slug = "test-article-indexing1";
        String metaDescription = "This is a test article to verify indexing.";

        try {
            System.out.println("\n🚀 Starting indexing test...");
            
            BlogPost blogPost = new BlogPost(0, title, content, null, category, categoryId, slug, metaDescription);
            WordPressPostClient wordPressPostClient = new WordPressPostClient();
            
            System.out.println("📝 Publishing test post...");
            PostResponse postResponse = wordPressPostClient.publishPost(blogPost); 
            System.out.println("✅ Post published: " + postResponse.getUrl());
            
            // Add delay to ensure post is accessible
            System.out.println("⏳ Waiting for post to be accessible...");
            Thread.sleep(2000);
            
            // Index the actual post URL
            indexPage(postResponse.getUrl());
            
        } catch (Exception e) {
            System.err.println("❌ Error in indexing test: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void indexPage(String url) throws IOException {
        System.out.println("\n🔍 Starting indexing process...");
        System.out.println("URL to index: " + url);
        
        try {
            GoogleSearchConsoleClient googleSearchConsole = new GoogleSearchConsoleClient();
            googleSearchConsole.submitUrl(url);
            
        } catch (Exception e) {
            System.err.println("❌ Error during indexing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String determineCategory(String title) {
        try {
            List<String> categories = wpCategoryClient.getAllCategories();
            if (categories.isEmpty()) {
                return "uncategorized";
            }
    
            String prompt = String.format(
                "Given these categories:\n%s\n\nWhich ONE category best fits this blog post title: '%s'?\n" +
                "If none fit well, reply with 'uncategorized'. Reply with ONLY the category name.",
                String.join("\n", categories),
                title
            );
                
            String category = openAIClient.callOpenAI(prompt).trim();
                
            // Verify the category exists or default to uncategorized
            return categories.contains(category) ? category : "uncategorized";
                
        } catch (Exception e) {
            System.err.println("Error determining category: " + e.getMessage());
            return "uncategorized";
        }
    }
    
    
    private String generateCoverImagePrompt(String keyword) {
        String prompt = """
            Create a detailed description for a blog post cover image about: %s
            The description should be vivid and specific, suitable for AI image generation.
            Focus on composition, style, and mood. Keep it under 100 words.
            """;
            
        String response = openAIClient.callOpenAI(prompt.formatted(keyword));
        System.out.println("Image description: " + response);
        return response;
    }
    
    private String generateCoverImage(String description) throws IOException {
        return getImgClient.generateImageWithVertex(description, 768, 432);
    }

    private String generateTitle(String keyword) throws IOException {
        System.out.println("\n🎯 Starting title generation for: " + keyword);
        
        try {
            // 1. Get SERP data
            KoalaAIClient koalaClient = new KoalaAIClient();
            String serpResults = koalaClient.getSerpResults(keyword);
            System.out.println("📊 Retrieved SERP data");

            // 2. Generate title options
            String titleGenerationPrompt = String.format(
                "Generate 10 compelling titles for an article about '%s'. " +
                "Include a mix of how-to, list-based, question-based, and emotionally-driven titles. " +
                "Ensure each title is under 60 characters for optimal SEO performance. " +
                "Here is the SERP page that reflects the search intent of the keyword:\n\n%s",
                keyword,
                serpResults
            );

            String titleOptions = openAIClient.callOpenAI(titleGenerationPrompt);
            System.out.println("📝 Generated title options:\n" + titleOptions);

            // 3. Select best title
            String selectionPrompt = String.format(
                "Pick the best title for a blog post targeting '%s' from these options:\n\n%s\n\n" +
                "Consider:\n" +
                "- Search intent match\n" +
                "- SEO optimization\n" +
                "- Click-worthiness\n" +
                "- Natural keyword inclusion\n\n" +
                "Return only the chosen title, nothing else.",
                keyword,
                titleOptions
            );

            String finalTitle = openAIClient.callOpenAI(selectionPrompt)
                .trim()
                .replaceAll("\\*+", "");
            System.out.println("✅ Selected title: " + finalTitle);
            
            return finalTitle;

        } catch (Exception e) {
            System.err.println("❌ Error generating title: " + e.getMessage());
            
            // Fallback to simpler title generation if the process fails
            String fallbackPrompt = String.format(
                "Create a simple, SEO-optimized title for '%s' under 60 characters.",
                keyword
            );
            return openAIClient.callOpenAI(fallbackPrompt).trim();
        }
    }

    private String generateMetaDescription(String keyword, String content) throws IOException {
        System.out.println("\n📝 Starting meta description generation...");
        System.out.println("Keyword: " + keyword);
        System.out.println("Content length: " + content.length() + " characters");
        
        try {
            // Get first 300 words
            String first300Words = content.split("\\s+", 301).length > 300 
                ? String.join(" ", Arrays.copyOfRange(content.split("\\s+"), 0, 300))
                : content;
            System.out.println("\n1️⃣ Extracted first 300 words (" + first300Words.split("\\s+").length + " words)");

            // Step 1: Extract the most valuable sentence
            String extractPrompt = String.format(
                "Return the most valuable sentence from the content that directly addresses the reader's search intent for the keyword: '%s'. " +
                "This sentence should provide the most value if they could only read one sentence:\n\n%s",
                keyword,
                first300Words
            );
            System.out.println("2️⃣ Requesting best sentence from OpenAI...");
            String bestSentence = openAIClient.callOpenAI(extractPrompt).trim();
            System.out.println("✅ Best sentence: " + bestSentence);

            // Step 2: Transform into optimized meta description
            String optimizePrompt = String.format(
                "Given this sentence, make it more concise while retaining the core idea. " +
                "Simplify and strengthen the sentence for clarity, engagement, and focus on key benefits. " +
                "The final sentence should be suitable for a meta description (119-135 characters). " +
                "If it makes logical sense, and seamlessly integrates with the sentence, include a natural variation or a portion of the keyword: '%s' " +
                "near the beginning, if not already present. If the keyword is a question or does not make sense to include, do not include the keyword at all.\n\n" +
                "Base sentence: %s",
                keyword,
                bestSentence
            );
            
            System.out.println("3️⃣ Optimizing meta description...");
            String metaDescription = openAIClient.callGPT4(optimizePrompt).trim();
            System.out.println("✅ Final meta description: " + metaDescription);
            System.out.println("📊 Character count: " + metaDescription.length());
            
            return metaDescription;
            
        } catch (Exception e) {
            System.out.println("\n⚠️ Primary meta description generation failed: " + e.getMessage());
            System.out.println("Attempting fallback...");
            
            try {
                String fallbackPrompt = String.format(
                    "Create a meta description for a blog post about '%s'.",
                    keyword
                );
                System.out.println("🔄 Using fallback prompt...");
                String fallbackDescription = openAIClient.callOpenAI(fallbackPrompt).trim();
                System.out.println("✅ Fallback meta description: " + fallbackDescription);
                return fallbackDescription;
                
            } catch (Exception fallbackError) {
                System.err.println("❌ Fallback meta description generation failed: " + fallbackError.getMessage());
                return null;
            }
        }
    }

    private String generateUrlSlug(String title) throws IOException {
        String prompt = String.format(
            "Convert this title into a URL slug:\n'%s'\n\n" +
            "Requirements:\n" +
            "- Use lowercase\n" +
            "- Replace spaces with hyphens\n" +
            "- Remove special characters\n" +
            "- Keep it short but readable\n" +
            "- Include main keyword if possible\n\n" +
            "Return only the slug, nothing else.",
            title
        );
        
        String slug = openAIClient.callOpenAI(prompt)
            .toLowerCase()
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .trim();
            
        System.out.println("Generated slug: " + slug);
        return slug;
    }

    private String generateContentWithRetry(String keyword, Long keywordId) {
        System.out.println("\n🤖 Attempting to generate content with ChatGPT...");
        System.out.println("Keyword: " + keyword);
        
        try {
            String content = postWriterService.createNewBlogPost(keyword, "");
            System.out.println("✅ Content generated successfully on first attempt");
            return content;
        } catch (IOException e) {
            System.out.println("\n⚠️ First attempt failed, waiting 5 minutes before retry...");
            System.out.println("Error: " + e.getMessage());
            
            try {
                Thread.sleep(5 * 60 * 1000); // Wait 5 minutes
                String content = postWriterService.createNewBlogPost(keyword, "");
                System.out.println("✅ Content generated successfully on second attempt");
                return content;
            } catch (IOException | InterruptedException retryException) {
                System.err.println("❌ Content generation failed after retry. Skipping this keyword.");
                System.err.println("Error: " + retryException.getMessage());
                databaseService.updateKeywordStatus(keywordId, "FAILED");
                return null;
            }
        }
    }

    

    private String generateDynamicKoalaContent(String keyword, Long keywordId, String blogTopic) {
        System.out.println("\n🐨 Starting advanced content workflow...");
        System.out.println("Keyword: " + keyword);
        
        // 1. Get SERP data
        KoalaAIClient koalaClient = new KoalaAIClient();
        String serpResults = koalaClient.getSerpResults(keyword);
        
        // 2. Analyze content type with OpenAI
        String contentTypePrompt = String.format(
            "Based on these search results, determine the best content type for a new article. " +
            "Consider user intent and existing content:\n\n%s\n\n" +
            "Choose ONE type from: 'blog_post' (default), 'listicle', or 'amazon_product_roundup'. " +
            "Reply with ONLY the content type, nothing else. " +
            "Choose 'listicle' if the topic suits a numbered list format. " +
            "Choose 'amazon_product_roundup' only if it's clearly about product comparisons or reviews.",
            serpResults
        );
        
        String contentType = openAIClient.callOpenAI(contentTypePrompt).trim().toLowerCase();
        System.out.println("📊 Determined content type: " + contentType);
        
        // 3. Generate content based on type
        JSONObject articleResponse;
        switch (contentType) {
            case "listicle":
                System.out.println("📝 Creating listicle format article...");
                articleResponse = koalaClient.createListicle(keyword);
                break;
            case "amazon_product_roundup":
                System.out.println("🛍️ Creating product roundup article...");
                articleResponse = koalaClient.createAmazonRoundup(keyword);
                break;
            default:
                System.out.println("📄 Creating standard blog post...");
                articleResponse = koalaClient.createOptimizedBlogPost(keyword, "");
                break;
        }
        
        // Extract and format content
        String content = articleResponse.getJSONObject("output").getString("html");
        if (content.contains("<h1>") && content.contains("</h1>")) {
            content = content.substring(content.indexOf("</h1>") + 5).trim();
        }
        
        System.out.println("✅ Content generated successfully");
        return content;
    }

    private String generateKoalaContentWithRetry(String keyword, Long keywordId, String blogTopic, String title) {
        System.out.println("\n🐨 Starting content workflow...");
        System.out.println("Keyword: " + keyword);
        
        try {
            KoalaAIClient koalaClient = new KoalaAIClient();
            
            System.out.println("📄 Creating standard blog post...");
            JSONObject articleResponse = koalaClient.createOptimizedBlogPost(keyword, "");
            
            // Extract and format content
            String content = articleResponse.getJSONObject("output").getString("html");
            if (content.contains("<h1>") && content.contains("</h1>")) {
                content = content.substring(content.indexOf("</h1>") + 5).trim();
            }
            
            System.out.println("✅ Content generated successfully");
            return content;
            
        } catch (Exception e) {
            System.err.println("❌ Error in content workflow: " + e.getMessage());
            databaseService.updateKeywordStatus(keywordId, "FAILED");
            return null;
        }
    }

    public static void main(String[] args) {
        try {
            System.out.println("\n🚀 Testing AutoContentWorkflowService...");
            
            AutoContentWorkflowService service = new AutoContentWorkflowService();
            service.processNextKeyword();

        } catch (Exception e) {
            System.err.println("\n❌ Error during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 