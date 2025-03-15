package com.zho.services.DailyPosts.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.zho.model.Site;
import com.zho.services.DailyPosts.AutoContentWorkflowService;

public class ContentGenerationHandler implements RequestHandler<Object, String> {
    
    @Override
    public String handleRequest(Object input, Context context) {
        try {
            // Add a test log message
            context.getLogger().log("CICD works!");
            
            Site.SwitchSite(Site.MAIN);

            try {
                Thread.sleep(5000);  // Sleep for 5000 milliseconds (5 seconds)
            } catch (InterruptedException e) {
                context.getLogger().log("Sleep interrupted: " + e.getMessage());
            }    

            if (!Site.getCurrentSite().isActive()) {
                context.getLogger().log("Blog is inactive. Skipping content generation.");
                return "Blog inactive - no action taken";
            }
            
            context.getLogger().log("Blog is active. Generating content...");
            AutoContentWorkflowService service = new AutoContentWorkflowService();
            service.processNextKeyword();
            
            return "Content generation successful";
            
        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
} 