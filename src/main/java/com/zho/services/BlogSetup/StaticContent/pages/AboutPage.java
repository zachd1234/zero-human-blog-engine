package com.zho.services.BlogSetup.StaticContent.pages;

import java.io.IOException;
import org.apache.hc.core5.http.ParseException;
import com.zho.api.OpenAIClient;
import com.zho.api.wordpress.WordPressBlockClient;
import com.zho.api.wordpress.WordPressMediaClient;
import com.zho.model.BlogRequest;
import com.zho.model.Image;
import org.json.JSONObject;
import com.zho.model.Persona;
import java.util.List;
import com.zho.services.DatabaseService;
import java.sql.SQLException;


public class AboutPage implements StaticPage {
    private final WordPressBlockClient blockClient;
    private final OpenAIClient openAIClient;
    private final WordPressMediaClient mediaClient;
    private final DatabaseService databaseService;
    private final int pageId = 318;
    private final int imagesNeeded = 1; 

    public AboutPage(WordPressBlockClient blockClient, OpenAIClient openAIClient, WordPressMediaClient mediaClient, DatabaseService databaseService) {
        this.blockClient = blockClient;
        this.openAIClient = openAIClient;
        this.mediaClient = mediaClient;
        this.databaseService = databaseService;
    }

    @Override
    public void updateStaticContent(BlogRequest request, List<Image> images) throws IOException, ParseException {
        Persona persona = null;
        try {
            persona = databaseService.getPersona();
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
            throw new IOException("Failed to get persona from database", e);
        }

        String valueProp = generateValueProp(request);
        String explanation = generateSubheading(request);

        String intro = generateIntro(persona);
        String mission = generateExpandedMission(request);

        updateValuePropSection(valueProp, explanation);
        updateMission(intro, mission);

        updateMyStoryText(persona.getBiography());
        
        // Update persona image using database content
        updatePersonaImage();
        
        // Update the infobox image if provided
        if (!images.isEmpty()) {
            System.out.println("Updating infobox image with direct method");
            mediaClient.updateInfoboxImage(pageId, "318_1fa743-7a", images.get(0));
            
            // Store the image URL in the database for reference
            try {
                databaseService.updateElementContent(pageId, "318_infobox_image", images.get(0).getUrl());
                System.out.println("Database updated with new infobox image URL");
            } catch (SQLException e) {
                System.err.println("Warning: Could not update database with infobox image URL: " + e.getMessage());
                // Continue without throwing an exception as the image has been updated in WordPress
            }
        }

        // NOTE: If you add another section that needs an image:
        // 1. Update getRequiredImageCount() 
        
    }

    private String generateValueProp(BlogRequest request) throws IOException {
        String prompt = String.format(
            "Create a short, powerful value proposition (3-6 words) for a blog about %s. " +
            "Format: The ultimate result readers will get, stated concisely. " +
            "Examples: 'Never worry about money again' or 'Truly understand your cat' or 'Be fit for life'. " +
            "Make it specific to: %s",
            request.getTopic(),
            request.getDescription()
        );
        return openAIClient.callOpenAI(prompt, 0.9);
    }

    private String generateSubheading(BlogRequest request) throws IOException {
        String prompt = String.format(
            "Create a single sentence that expands on the blog's value proposition using this format: " +
            "'We help [target audience] [achieve main result] through [topic 1], [topic 2], and [topic 3].' " +
            "Make it specific to a blog about: %s",
            request.getDescription()
        );
        return openAIClient.callOpenAIWithSystemPrompt(prompt, databaseService.getSystemPrompt());
    }

    private String generateIntro(Persona persona) throws IOException {
        return String.format(
            "Hi! I'm %s, and I'm glad you are here.",
            persona.getName()
        );
    }

    private String generateExpandedMission(BlogRequest request) throws IOException {
        String prompt = String.format(
            "Write an expanded mission statement (3-4 sentences) for a blog about %s. " +
            "Start with what readers will get from the blog. " +
            "Format: By reading this blog, you will [benefit 1], [benefit 2], and [benefit 3]. " +
            "Make it personal ('I want to help you...') and inspiring. " +
            "Focus on the transformation readers will experience.",
            request.getDescription()
        );
        return openAIClient.callOpenAIWithSystemPrompt(prompt, databaseService.getSystemPrompt());
    }

    private void updateValuePropSection(String valueProp, String explanation) throws IOException, ParseException {
        try {
            // Update value prop heading
            String currentHeading = databaseService.getElementContent(pageId, "318_heading_value-prop");
            if (currentHeading != null) {
                // Always use exact text replacement, no fallbacks
                boolean success = blockClient.findAndReplaceTextWithCertainty(pageId, currentHeading, valueProp, true);
                if (!success) {
                    throw new IOException("Failed to replace text for value proposition heading. Current text not found in page content.");
                }
                System.out.println("Updated value prop heading with exact replacement");
            } else {
                // First time update - must use uniqueID approach just once
                blockClient.updateAdvancedHeadingText(pageId, "318_heading_value-prop", valueProp);
                System.out.println("First-time update of value prop heading (uniqueID used just for initial creation)");
            }
            // Always update the database with the new content
            databaseService.updateElementContent(pageId, "318_heading_value-prop", valueProp);
            
            // Update explanation
            String currentExplanation = databaseService.getElementContent(pageId, "318_para_explanation");
            if (currentExplanation != null) {
                // Always use exact text replacement, no fallbacks
                boolean success = blockClient.findAndReplaceTextWithCertainty(pageId, currentExplanation, explanation, true);
                if (!success) {
                    throw new IOException("Failed to replace text for explanation paragraph. Current text not found in page content.");
                }
                System.out.println("Updated explanation with exact replacement");
            } else {
                // First time update - must use uniqueID approach just once
                blockClient.updateParagraphText(pageId, "318_para_explanation", explanation);
                System.out.println("First-time update of explanation (uniqueID used just for initial creation)");
            }
            // Always update the database with the new content
            databaseService.updateElementContent(pageId, "318_para_explanation", explanation);
            
        } catch (SQLException e) {
            System.err.println("Error accessing database: " + e.getMessage());
            throw new IOException("Database error while updating value proposition section", e);
        }
    }

    private void updateMission(String intro, String mission) throws IOException, ParseException {
        try {
            // Update mission heading
            String currentIntro = databaseService.getElementContent(pageId, "318_heading_mission");
            if (currentIntro != null) {
                // Always use exact text replacement, no fallbacks
                boolean success = blockClient.findAndReplaceTextWithCertainty(pageId, currentIntro, intro, true);
                if (!success) {
                    throw new IOException("Failed to replace text for mission heading. Current text not found in page content.");
                }
                System.out.println("Updated mission heading with exact replacement");
            } else {
                // First time update - must use uniqueID approach just once
                blockClient.updateHeadingText(pageId, "318_heading_mission", intro);
                System.out.println("First-time update of mission heading (uniqueID used just for initial creation)");
            }
            // Always update the database with the new content
            databaseService.updateElementContent(pageId, "318_heading_mission", intro);
            
            // Update mission paragraph
            String currentMission = databaseService.getElementContent(pageId, "318_para_mission");
            if (currentMission != null) {
                // Always use exact text replacement, no fallbacks
                boolean success = blockClient.findAndReplaceTextWithCertainty(pageId, currentMission, mission, true);
                if (!success) {
                    throw new IOException("Failed to replace text for mission paragraph. Current text not found in page content.");
                }
                System.out.println("Updated mission paragraph with exact replacement");
            } else {
                // First time update - must use uniqueID approach just once
                blockClient.updateParagraphText(pageId, "318_para_mission", mission);
                System.out.println("First-time update of mission paragraph (uniqueID used just for initial creation)");
            }
            // Always update the database with the new content
            databaseService.updateElementContent(pageId, "318_para_mission", mission);
            
        } catch (SQLException e) {
            System.err.println("Error accessing database: " + e.getMessage());
            throw new IOException("Database error while updating mission section", e);
        }
    }

    private void updateMyStoryText(String biography) throws IOException, ParseException {
        try {
            // Update biography paragraph
            String currentBio = databaseService.getElementContent(pageId, "318_para_bio");
            if (currentBio != null) {
                // Always use exact text replacement, no fallbacks
                boolean success = blockClient.findAndReplaceTextWithCertainty(pageId, currentBio, biography, true);
                if (!success) {
                    throw new IOException("Failed to replace text for biography paragraph. Current text not found in page content.");
                }
                System.out.println("Updated biography with exact replacement");
            } else {
                // First time update - must use uniqueID approach just once
                blockClient.updateParagraphText(pageId, "318_para_bio", biography);
                System.out.println("First-time update of biography (uniqueID used just for initial creation)");
            }
            // Always update the database with the new content
            databaseService.updateElementContent(pageId, "318_para_bio", biography);
            
        } catch (SQLException e) {
            System.err.println("Error accessing database: " + e.getMessage());
            throw new IOException("Database error while updating biography section", e);
        }
    }

    @Override
    public String getPageName() {
        return "About";
    }

    @Override
    public int getPageId() {
        return pageId;
    }

    @Override 
    public int getRequiredImageCount() {
        return imagesNeeded; 
    }

    @Override
    public String getTitleTemplate() {
        return "Generate a title for the About page"; // This won't be used if hasHardcodedTitle is true
    }

    @Override
    public String getMetaDescriptionTemplate() {
        try {
            BlogRequest blogInfo = new DatabaseService().getBlogInfo();
            String siteName = new WordPressBlockClient().getSiteTitle();
            
            return String.format(
                "Generate a 119-135 character meta description for the About page of %s (%s). " +
                "Focus on the blog's mission and purpose. Make it inviting and trustworthy.",
                siteName,
                blogInfo.getTopic()
            );
        } catch (IOException | ParseException | SQLException e) {
            System.err.println("Error getting blog info or site title: " + e.getMessage());
            // Fallback to a generic template
            return "Generate a 119-135 character meta description for the About page that " +
                   "focuses on the blog's mission and purpose. Make it inviting and trustworthy.";
        }
    }

    @Override
    public boolean hasHardcodedTitle() {
        return true;
    }

    @Override
    public String getHardcodedTitle() {
        return "About Us";
    }

    
    /**
     * Test method to demonstrate the exact text replacement of the mission statement
     */
    public void testMissionReplacement() throws IOException, ParseException, SQLException {
        System.out.println("Testing exact text replacement of mission statement on About page (ID: " + pageId + ")");
        
        // Get current mission from the database
        String currentMission = databaseService.getElementContent(pageId, "318_para_mission");
        if (currentMission == null) {
            System.out.println("Mission text not found in database. Please run a content generation first.");
            return;
        }
        
        System.out.println("Found mission statement in database with length: " + currentMission.length() + " chars");
        System.out.println("First 50 chars: " + currentMission.substring(0, Math.min(50, currentMission.length())));
        
        // The exact new text to replace with
        String newMission = "By reading this blog, you will delve into the dynamic world of Y Combinator with fresh insights, cutting-edge trends, and insider perspectives. I want to help you not only stay informed but be inspired to revolutionize your approach to entrepreneurship, AI innovation, and startup scaling. Together, we will embark on a journey of transformation, unlocking the full potential of your ideas and ventures as we pioneer sustainable tech solutions that shape the future.";
        
        System.out.println("Will replace current mission with new text (length: " + newMission.length() + " chars)");
        
        // Use the improved method to update the content with absolute certainty
        System.out.println("Attempting to replace the text with absolute certainty...");
        boolean success = blockClient.findAndReplaceTextWithCertainty(pageId, currentMission, newMission, true);
        
        if (!success) {
            throw new IOException("Failed to update mission with exact replacement. Text not found on page.");
        }
        
        System.out.println("Successfully updated the mission with exact replacement");
        
        // Update the database with the new content
        databaseService.updateElementContent(pageId, "318_para_mission", newMission);
        System.out.println("Database updated with new mission statement");
        
        // Verify the update in the database
        String updatedMission = databaseService.getElementContent(pageId, "318_para_mission");
        boolean verificationSuccess = newMission.equals(updatedMission);
        System.out.println("Database verification " + (verificationSuccess ? "SUCCESSFUL" : "FAILED"));
    }

    /**
     * Update the persona image on the About page using the direct WordPress API
     */
    private void updatePersonaImage() throws IOException, ParseException {
        try {
            Persona persona = databaseService.getPersona();
            String newImageUrl = "https://ruckquest.com/wp-content/uploads/2025/03/image-1742357395016.jpg";
            
            //persona.getImageUrl();
            
            System.out.println("Updating persona image with direct method");
            System.out.println("New image URL: " + newImageUrl);
            
            // Use direct update method for the image - no text replacement
            mediaClient.UpdateColumnImage(pageId, "318_8978a8-3e", newImageUrl);
            System.out.println("Persona image updated successfully with direct method");
            
            
        } catch (SQLException e) {
            System.err.println("Error accessing persona from database: " + e.getMessage());
            throw new IOException("Database error while getting persona for image update", e);
        }
    }
    
    /**
     * Test method to update just the persona image directly
     */
    public void testPersonaImageUpdate() throws IOException, ParseException {
        System.out.println("Testing persona image update on About page (ID: " + pageId + ")");
        updatePersonaImage();
        System.out.println("Persona image update completed");
    }

    public static void main(String[] args) {
        try {
            WordPressBlockClient client = new WordPressBlockClient();
            OpenAIClient openAIClient = new OpenAIClient();
            WordPressMediaClient mediaClient = new WordPressMediaClient();
            DatabaseService databaseService = new DatabaseService();
            
            AboutPage aboutPage = new AboutPage(client, openAIClient, mediaClient, databaseService);
            
            // Test the direct image update method
            aboutPage.testPersonaImageUpdate();
            
        } catch (Exception e) {
            System.err.println("Error during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
