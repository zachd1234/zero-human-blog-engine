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
            // AI logic here. 

            if (keyword == null) {
                System.out.println("No pending keywords found");
                return;
            }

            
            System.out.println("Processing keyword: " + keyword.getKeyword());
            

            String imageDescription = generateCoverImagePrompt(keyword.getKeyword());
            String imageUrl = generateCoverImage(imageDescription);
            Image coverImage = new Image(imageUrl);

            String category = determineCategory(keyword.getKeyword());
            System.out.println("Selected category: " + category);
            Integer categoryId = wpCategoryClient.getCategoryId(category);

            //TODO: create cover image and cateogry 
            // 3. Generate content
            String content = postWriterService.createNewBlogPost(keyword.getKeyword());
            BlogPost blogPost = new BlogPost(0, keyword.getKeyword(), content, coverImage, category, categoryId);
            
            PostResponse postResponse = wordPressPostClient.publishPost(blogPost);
            int postId = postResponse.getId();
            String postUrl = postResponse.getUrl();
            
            if (blogPost.getCoverImage() != null) {
                mediaClient.updatePostCoverImage(postId, blogPost.getCoverImage());
            }

            // 5. Update database
            databaseService.updateKeywordStatus(Long.valueOf(keyword.getId()), "PUBLISHED");
            databaseService.updateKeywordPostUrl(Long.valueOf(keyword.getId()), postUrl);
            
            System.out.println("Successfully published post for: " + keyword.getKeyword());
            System.out.println("URL: " + postUrl);
            
        } catch (Exception e) {
            System.err.println("Error in content workflow: " + e.getMessage());
            // Could add retry logic or alert system here
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
} 