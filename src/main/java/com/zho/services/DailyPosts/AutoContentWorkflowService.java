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
import com.zho.api.KoalaWriterClient;
import com.zho.model.BlogRequest;
import org.json.JSONObject;

import java.util.List;

public class AutoContentWorkflowService {
    private final DatabaseService databaseService;
    private final PostWriterService postWriterService;
    private final  WordPressPostClient wordPressPostClient;
    private final OpenAIClient openAIClient;
    private final GetImgAIClient getImgClient;
    private final WordPressCategoryClient wpCategoryClient;
    private final WordPressMediaClient mediaClient;

    public AutoContentWorkflowService() {
        this.databaseService = new DatabaseService();
        this.postWriterService = new PostWriterService();
        this.wordPressPostClient = new WordPressPostClient();
        this.openAIClient = new OpenAIClient();
        this.getImgClient = new GetImgAIClient();
        this.wpCategoryClient = new WordPressCategoryClient(); 
        this.mediaClient = new WordPressMediaClient();
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
            
            // 2. Try to generate content with one retry
            String content = null;
            BlogRequest blogInfo = databaseService.getBlogInfo();
            Site site = Site.getCurrentSite();
            
            // Choose content generation method based on blog settings
            if (site.isActive()) {
                System.out.println("Using Koala Writer");
                content = generateKoalaContentWithRetry(keyword.getKeyword(), Long.valueOf(keyword.getId()), blogInfo.getTopic());
            } else {
                try {
                    content = postWriterService.createNewBlogPost(keyword.getKeyword());
                } catch (IOException e) {
                    System.out.println("First attempt failed, waiting 5 minutes before retry...");
                    try {
                        Thread.sleep(5 * 60 * 1000); // Wait 5 minutes
                        content = postWriterService.createNewBlogPost(keyword.getKeyword());
                    } catch (IOException | InterruptedException retryException) {
                        System.err.println("Content generation failed after retry. Skipping this keyword.");
                        databaseService.updateKeywordStatus(Long.valueOf(keyword.getId()), "FAILED");
                        return;
                    }
                }
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

            String title = generateTitle(keyword.getKeyword());
            String slug = generateUrlSlug(title);
            String metaDescription = generateMetaDescription(keyword.getKeyword());

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
            System.out.println("URL: " + postResponse.getUrl());
            
        } catch (Exception e) {
            System.err.println("Error in content workflow: " + e.getMessage());
        }
    }


    private void indexPage(String url) throws IOException {
        GoogleSearchConsoleClient googleSearchConsole = new GoogleSearchConsoleClient();
        googleSearchConsole.submitUrl(url);
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
        return getImgClient.generateImage(description, 768, 432);
    }

    private String generateTitle(String keyword) throws IOException {
        String prompt = String.format(
            "Create an SEO-optimized title for a blog post about '%s'.\n" +
            "Requirements:\n" +
            "- Include the main keyword\n" +
            "- Be compelling and clear\n" +
            "- Keep under 60 characters\n" +
            "- No quotes or special characters\n\n" +
            "Return only the title, nothing else.",
            keyword
        );
        
        return openAIClient.callOpenAI(prompt);
    }

    private String generateMetaDescription(String keyword) throws IOException {
        String prompt = String.format(
            "Create a meta description for a blog post about '%s'.\n" +
            "Requirements:\n" +
            "- Include the main keyword naturally\n" +
            "- Be compelling and informative\n" +
            "- Keep between 150-160 characters\n" +
            "- Use active voice\n" +
            "- End with a call to action\n\n" +
            "Return only the meta description, nothing else.",
            keyword
        );
        
        return openAIClient.callOpenAI(prompt);
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
            String content = postWriterService.createNewBlogPost(keyword);
            System.out.println("✅ Content generated successfully on first attempt");
            return content;
        } catch (IOException e) {
            System.out.println("\n⚠️ First attempt failed, waiting 5 minutes before retry...");
            System.out.println("Error: " + e.getMessage());
            
            try {
                Thread.sleep(5 * 60 * 1000); // Wait 5 minutes
                String content = postWriterService.createNewBlogPost(keyword);
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
        KoalaWriterClient koalaClient = new KoalaWriterClient();
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
                articleResponse = koalaClient.createOptimizedBlogPost(keyword);
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

    private String generateKoalaContentWithRetry(String keyword, Long keywordId, String blogTopic) {
        System.out.println("\n🐨 Starting content workflow...");
        System.out.println("Keyword: " + keyword);
        
        try {
            KoalaWriterClient koalaClient = new KoalaWriterClient();
            
            System.out.println("📄 Creating standard blog post...");
            JSONObject articleResponse = koalaClient.createOptimizedBlogPost(keyword);
            
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
            
            // Get blog info for context
            BlogRequest blogInfo = service.databaseService.getBlogInfo();
            System.out.println("\n📚 Blog Info:");
            System.out.println("Topic: " + blogInfo.getTopic());
            System.out.println("Description: " + blogInfo.getDescription());
            
            // Process one keyword
            System.out.println("\n🎯 Processing next keyword...");
            service.processNextKeyword();
            
        } catch (Exception e) {
            System.err.println("\n❌ Error during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 