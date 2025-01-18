package com.zho.services.DailyPosts;

import com.zho.services.DatabaseService;
import com.zho.api.wordpress.WordPressPostClient;
import com.zho.api.wordpress.WordPressPostClient.PostResponse;
import com.zho.model.KeywordAnalysis;
import com.zho.model.BlogPost;
import com.zho.api.OpenAIClient;
import com.zho.api.GetImgAIClient;
import java.io.IOException;
import com.zho.model.Image;
import com.zho.api.wordpress.WordPressCategoryClient;
import com.zho.api.wordpress.WordPressMediaClient;

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

            // 4. Create and publish post
            BlogPost blogPost = new BlogPost(0, keyword.getKeyword(), content, coverImage, category, categoryId);
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
        
        return openAIClient.callOpenAIWithRateLimit(prompt, 0.7);
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
        
        return openAIClient.callOpenAIWithRateLimit(prompt, 0.7);
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
        
        String slug = openAIClient.callOpenAIWithRateLimit(prompt, 0.7)
            .toLowerCase()
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .trim();
            
        System.out.println("Generated slug: " + slug);
        return slug;
    }
} 