package com.zho.services.pages;

import java.io.IOException;
import org.apache.hc.core5.http.ParseException;
import com.zho.api.OpenAIClient;
import com.zho.api.wordpress.WordPressBlockClient;
import com.zho.model.BlogRequest;

public class AboutPage implements StaticPage {
    private final WordPressBlockClient blockClient;
    private final OpenAIClient openAIClient;
    private final int pageId = 318;

    public AboutPage(WordPressBlockClient blockClient, OpenAIClient openAIClient) {
        this.blockClient = blockClient;
        this.openAIClient = openAIClient;
    }

    @Override
    public void updateStaticContent(BlogRequest request) throws IOException, ParseException {
        System.out.println("\n=== Generating Value Proposition ===");
        String valueProp = generateValueProp(request);
        System.out.println("Generated value prop: " + valueProp);

        System.out.println("\n=== Generating Explanation ===");
        String explanation = generateSubheading(request);
        System.out.println("Generated explanation: " + explanation);

        System.out.println("\n=== Generating Story ===");
        String story = generateStory(request);
        System.out.println("Generated story: " + story);

        System.out.println("\n=== Generating Mission ===");
        String mission = generateExpandedMission(request);
        System.out.println("Generated mission: " + mission);

        updateValuePropSection(valueProp, explanation);
        updateStoryAndMission(story, mission);
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
        return openAIClient.callOpenAI(prompt, 0.9);
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
        return openAIClient.callOpenAI(prompt, 0.9);
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
        
        // Update patterns to match Kadence blocks
        String storyPattern = "<!-- wp:kadence/advancedheading \\{[^}]*\"uniqueID\":\"318_story\"[^}]*\\}[\\s\\S]*?<!-- /wp:kadence/advancedheading -->";
        String missionPattern = "<!-- wp:kadence/advancedheading \\{[^}]*\"uniqueID\":\"318_mission\"[^}]*\\}[\\s\\S]*?<!-- /wp:kadence/advancedheading -->";
        
        String newStory = "<!-- wp:kadence/advancedheading {" +
                "\"uniqueID\":\"318_story\"," +
                "\"htmlTag\":\"p\"" +
                "} -->\n" +
                "<p class=\"kt-adv-heading318_story wp-block-kadence-advancedheading\" " +
                "data-kb-block=\"kb-adv-heading318_story\">" + story + "</p>\n" +
                "<!-- /wp:kadence/advancedheading -->";
                
        String newMission = "<!-- wp:kadence/advancedheading {" +
                "\"uniqueID\":\"318_mission\"," +
                "\"htmlTag\":\"p\"" +
                "} -->\n" +
                "<p class=\"kt-adv-heading318_mission wp-block-kadence-advancedheading\" " +
                "data-kb-block=\"kb-adv-heading318_mission\">" + mission + "</p>\n" +
                "<!-- /wp:kadence/advancedheading -->";
        
        String updatedContent = currentContent
            .replaceAll(storyPattern, newStory)
            .replaceAll(missionPattern, newMission);
        
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

    public static void main(String[] args) {
        try {
            // Initialize dependencies
            WordPressBlockClient blockClient = new WordPressBlockClient();
            OpenAIClient openAIClient = new OpenAIClient();
            
            // Create AboutPage instance
            AboutPage aboutPage = new AboutPage(blockClient, openAIClient);
            
            // Create test BlogRequest
            BlogRequest testRequest = new BlogRequest(
                "tennis",  // topic
                "tennis blog for beginners"  // description
            );
            
            // Test the page update
            System.out.println("Testing About page update...");
            aboutPage.updateStaticContent(testRequest);
            System.out.println("About page updated successfully!");
            
        } catch (Exception e) {
            System.err.println("Error during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 