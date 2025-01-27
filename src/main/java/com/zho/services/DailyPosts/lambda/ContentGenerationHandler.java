package com.zho.services.DailyPosts.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.zho.model.Site;
import com.zho.services.DailyPosts.AutoContentWorkflowService; 

public class ContentGenerationHandler implements RequestHandler<Object, String> {
    @Override
    public String handleRequest(Object input, Context context) {
        try {

            // Add a test log message somewhere in your code
            System.out.println("CICD works!");
            
            Site.SwitchSite(Site.TEST);

            try {
                Thread.sleep(5000);  // Sleep for 5000 milliseconds (5 seconds)
            } catch (InterruptedException e) {
                System.err.println("Sleep interrupted: " + e.getMessage());
            }    

            if (!Site.getCurrentSite().isActive()) {
                System.out.println("Blog is inactive. Skipping content generation.");
                return "Blog inactive - no action taken";
            }
            
            System.out.println("Blog is active. Generating content...");
            AutoContentWorkflowService service = new AutoContentWorkflowService();
            service.processNextKeyword();
            
            return "Content generation successful";
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
} 