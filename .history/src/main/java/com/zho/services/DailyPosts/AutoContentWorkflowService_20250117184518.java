package com.zho.services.DailyPosts;

import com.zho.services.DatabaseService;
import com.zho.api.wordpress.WordPressClient;
import com.zho.model.KeywordAnalysis;
import com.zho.model.BlogPost;

public class AutoContentWorkflowService {
    private final DatabaseService databaseService;
    private final PostWriterService postWriterService;
    private final WordPressClient wordPressClient;
    
    public AutoContentWorkflowService() {
        this.databaseService = new DatabaseService();
        this.postWriterService = new PostWriterService();
        this.wordPressClient = new WordPressClient();
    }
    
    public void processNextKeyword() {
        try {
            // 1. Get next keyword from queue
            KeywordAnalysis keyword = databaseService.getNextPendingKeyword();
            if (keyword == null) {
                System.out.println("No pending keywords found");
                return;
            }
            
            System.out.println("Processing keyword: " + keyword.getKeyword());
            
            // 2. Mark as processing
            databaseService.updateKeywordStatus(keyword.getId(), "PROCESSING");
            
            // 3. Generate content
            BlogPost blogPost = postWriterService.createNewBlogPost(keyword.getKeyword());
            
            // 4. Post to WordPress
            String postUrl = wordPressClient.publishPost(blogPost);
            
            // 5. Update database
            databaseService.updateKeywordStatus(keyword.getId(), "PUBLISHED");
            databaseService.updateKeywordPostUrl(keyword.getId(), postUrl);
            
            System.out.println("Successfully published post for: " + keyword.getKeyword());
            System.out.println("URL: " + postUrl);
            
        } catch (Exception e) {
            System.err.println("Error in content workflow: " + e.getMessage());
            // Could add retry logic or alert system here
        }
    }
} 