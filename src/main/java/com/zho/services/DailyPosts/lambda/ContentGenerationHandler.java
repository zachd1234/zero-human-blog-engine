package com.zho.services.DailyPosts.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.zho.model.Site;
import com.zho.services.DailyPosts.AutoContentWorkflowService;
import java.io.File;
import java.io.FileWriter;

public class ContentGenerationHandler implements RequestHandler<Object, String> {
    
    /**
     * Sets up Google credentials for Vertex AI
     * @param context Lambda context for logging
     */
    private void setupGoogleCredentials(Context context) {
        try {
            // Get credentials JSON from environment variable
            String credentialsJson = System.getenv("GOOGLE_CREDENTIALS_JSON");
            if (credentialsJson == null || credentialsJson.isEmpty()) {
                context.getLogger().log("ERROR: GOOGLE_CREDENTIALS_JSON environment variable not set");
                return;
            }
            
            // Write to temp file (Lambda allows writing to /tmp)
            File credentialsFile = new File("/tmp/google-credentials.json");
            try (FileWriter writer = new FileWriter(credentialsFile)) {
                writer.write(credentialsJson);
            }
            
            // Set system property to point to this file
            System.setProperty("GOOGLE_APPLICATION_CREDENTIALS", credentialsFile.getAbsolutePath());
            
            context.getLogger().log("Google credentials set up successfully at: " + credentialsFile.getAbsolutePath());
        } catch (Exception e) {
            context.getLogger().log("Error setting up Google credentials: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public String handleRequest(Object input, Context context) {
        try {
            // Set up Google credentials first
            setupGoogleCredentials(context);
            
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