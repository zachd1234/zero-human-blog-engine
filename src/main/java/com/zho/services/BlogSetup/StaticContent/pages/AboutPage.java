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
        //update persona image. 
        mediaClient.UpdateColumnImage(pageId, "318_8978a8-3e", persona.getImageUrl());
        //hardcoded
        mediaClient.updateInfoboxImage(pageId, "318_1fa743-7a", images.get(0));

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
        // Update value prop heading
        blockClient.updateAdvancedHeadingText(
            pageId,
            "318_heading_value-prop",
            valueProp
        );
        
        // Update explanation (it's an advanced heading, not a paragraph)
        blockClient.updateParagraphText(
            pageId,
            "318_para_explanation",
            explanation
        );
    }

    private void updateMission(String intro, String mission) throws IOException, ParseException {
        // Update mission heading
        blockClient.updateHeadingText(
            pageId,
            "318_heading_mission",  // Match the HTML uniqueID
            intro
        );
        
        // Update mission paragraph
        blockClient.updateParagraphText(
            pageId,
            "318_para_mission",
            mission
        );
    }

    private void updateMyStoryText(String biography) throws IOException, ParseException {
        blockClient.updateParagraphText(
            pageId,             // 318
            "318_para_bio",    // uniqueID for the bio paragraph
            biography          // The text to update with
        );
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
}