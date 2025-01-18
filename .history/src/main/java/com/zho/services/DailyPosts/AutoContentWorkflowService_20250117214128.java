package com.zho.services.DailyPosts;

import com.zho.services.DatabaseService;
import com.zho.api.wordpress.WordPressPostClient;
import com.zho.model.KeywordAnalysis;
import com.zho.model.BlogPost;
import com.zho.api.OpenAIClient;
import com.zho.api.GetImgAIClient;

public class AutoContentWorkflowService {
    private final DatabaseService databaseService;
    private final PostWriterService postWriterService;
    private final  WordPressPostClient wordPressPostClient;
    private final OpenAIClient openAIClient;
    private final GetImgAIClient getImgClient;
    
    public AutoContentWorkflowService() {
        this.databaseService = new DatabaseService();
        this.postWriterService = new PostWriterService();
        this.wordPressPostClient = new WordPressPostClient();
        this.openAIClient = new OpenAIClient();
        this.getImgClient = new GetImgAIClient();
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
            

            
            //TODO: create cover image and cateogry 
            // 3. Generate content
            String content = postWriterService.createNewBlogPost(keyword.getKeyword());
            BlogPost blogPost = new BlogPost(0, keyword.getKeyword(), content, null, "");
            
            String postUrl = wordPressPostClient.publishPost(blogPost);
            
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
    
    private String generateCoverImagePrompt(String keyword) {
        String prompt = """
            Create a detailed description for a blog post cover image about: %s
            The description should be vivid and specific, suitable for AI image generation.
            Focus on composition, style, and mood. Keep it under 100 words.
            """;
            
        String response = openAIClient.sendMessage(prompt.formatted(keyword));
        System.out.println("Image description: " + response);
        return response;
    }
    
    private String generateCoverImage(String description) {
        return getImgClient.generateImage(description, 768, 432);
    }
} 