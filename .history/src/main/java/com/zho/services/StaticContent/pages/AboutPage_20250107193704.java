package com.zho.services.StaticContent.pages;

import java.io.IOException;
import org.apache.hc.core5.http.ParseException;
import com.zho.api.OpenAIClient;
import com.zho.api.wordpress.WordPressBlockClient;
import com.zho.api.wordpress.WordPressMediaClient;
import com.zho.model.BlogRequest;
import com.zho.model.UnsplashImage;
import org.json.JSONObject;
import java.util.List;
import com.zho.services.DatabaseService;


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
    public void updateStaticContent(BlogRequest request, List<UnsplashImage> images) throws IOException, ParseException {
        String valueProp = generateValueProp(request);
        String explanation = generateSubheading(request);

        String story = generateStory(request);
        String mission = generateExpandedMission(request);

        updateValuePropSection(valueProp, explanation);
        updateStoryAndMission(story, mission);

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

    private String generateStory(BlogRequest request) throws IOException {
        String prompt = String.format(
            "Write a personal story (2-3 sentences) about why I started a blog about %s. " +
            "Format: I was living my life when I noticed a problem: couldn't find good information about %s. " +
            "Since I'm passionate about this topic, I decided to create a blog to help others. " +
            "Make it authentic and relatable.",
            request.getTopic(),
            request.getDescription()
        );
        return openAIClient.callOpenAIWithSystemPrompt(prompt, databaseService.getSystemPrompt());
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
        return openAIClient.callOpenAI(prompt, 0.9);
    }

    private void updateValuePropSection(String valueProp, String explanation) throws IOException, ParseException {
        // Get current content
        String currentContent = blockClient.getPageContent(getPageId());
        
        // Update value prop heading
        String valuePropPattern = "<!-- wp:kadence/advancedheading \\{[^}]*\"uniqueID\":\"318_388068-ef\"[^}]*\\}[\\s\\S]*?<!-- /wp:kadence/advancedheading -->";
        String newValueProp = "<!-- wp:kadence/advancedheading {" +
                "\"level\":1,\"uniqueID\":\"318_388068-ef\"," +
                "\"lineType\":\"em\",\"margin\":[0,0,48,0]," +
                "\"fontSize\":[\"xl\",\"\",38],\"fontHeight\":[1,null,null]," +
                "\"fontHeightType\":\"em\"" +
                "} -->\n" +
                "<h1 class=\"kt-adv-heading318_388068-ef wp-block-kadence-advancedheading\" " +
                "data-kb-block=\"kb-adv-heading318_388068-ef\">" + valueProp + "</h1>\n" +
                "<!-- /wp:kadence/advancedheading -->";
        
        // Update explanation
        String explanationPattern = "<!-- wp:kadence/advancedheading \\{[^}]*\"uniqueID\":\"318_5d46e2-e6\"[^}]*\\}[\\s\\S]*?<!-- /wp:kadence/advancedheading -->";
        String newExplanation = "<!-- wp:kadence/advancedheading {" +
                "\"uniqueID\":\"318_5d46e2-e6\"," +
                "\"margin\":[-10,0,32,0],\"htmlTag\":\"p\"" +
                "} -->\n" +
                "<p class=\"kt-adv-heading318_5d46e2-e6 wp-block-kadence-advancedheading\" " +
                "data-kb-block=\"kb-adv-heading318_5d46e2-e6\">" + explanation + "</p>\n" +
                "<!-- /wp:kadence/advancedheading -->";
        
        String updatedContent = currentContent
            .replaceAll(valuePropPattern, newValueProp)
            .replaceAll(explanationPattern, newExplanation);
        
        blockClient.updatePageContent(getPageId(), updatedContent);
    }

    private void updateStoryAndMission(String story, String mission) throws IOException, ParseException {
        String currentContent = blockClient.getPageContent(getPageId());
        
        // Update story - it's a standard paragraph block
        String storyPattern = "<!-- wp:paragraph -->\n<p class=\"\">.*?</p>\n<!-- /wp:paragraph -->";
        String newStory = "<!-- wp:paragraph -->\n<p class=\"\">" + story + "</p>\n<!-- /wp:paragraph -->";
        
        // Update mission - it comes after an h2 heading with "Our Mission"
        String missionPattern = "(?s)<!-- wp:heading \\{\"textAlign\":\"center\"\\} -->.*?<!-- wp:paragraph -->\n<p class=\"\">.*?</p>\n<!-- /wp:paragraph -->";
        String newMission = "<!-- wp:heading {\"textAlign\":\"center\"} -->\n" +
                           "<h2 class=\"wp-block-heading has-text-align-center\">Our Mission </h2>\n" +
                           "<!-- /wp:heading -->\n\n" +
                           "<!-- wp:paragraph -->\n<p class=\"\">" + mission + "</p>\n<!-- /wp:paragraph -->";
        
        String updatedContent = currentContent
            .replaceFirst(storyPattern, newStory)  // Use replaceFirst to only update the first paragraph
            .replaceFirst(missionPattern, newMission);
        
        blockClient.updatePageContent(getPageId(), updatedContent);
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