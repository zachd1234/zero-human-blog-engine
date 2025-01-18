package com.zho.services.DailyPosts;

import com.zho.services.DatabaseService;
import com.zho.api.wordpress.WordPressPostClient;
import com.zho.model.KeywordAnalysis;
import com.zho.model.BlogPost;

public class AutoContentWorkflowService {
    private final DatabaseService databaseService;
    private final PostWriterService postWriterService;
    private final  WordPressPostClient wordPressPostClient;
    
    public AutoContentWorkflowService() {
        this.databaseService = new DatabaseService();
        this.postWriterService = new PostWriterService();
        this.wordPressPostClient = new WordPressPostClient();
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
            //String content = postWriterService.createNewBlogPost(keyword.getKeyword());
           // BlogPost blogPost = new BlogPost(0, keyword.getKeyword(), content, null, "");
            
            // 4. Post to WordPress
            //String postUrl = wordPressPostClient.publishPost(blogPost);
            
            // 5. Update database
            //databaseService.updateKeywordStatus(Long.valueOf(keyword.getId()), "PUBLISHED");
            //databaseService.updateKeywordPostUrl(Long.valueOf(keyword.getId()), postUrl);
            
            //System.out.println("Successfully published post for: " + keyword.getKeyword());
            //System.out.println("URL: " + postUrl);
            
        } catch (Exception e) {
            System.err.println("Error in content workflow: " + e.getMessage());
            // Could add retry logic or alert system here
        }
    }
} 