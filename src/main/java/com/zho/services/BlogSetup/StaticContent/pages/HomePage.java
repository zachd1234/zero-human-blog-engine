package com.zho.services.BlogSetup.StaticContent.pages;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.json.JSONObject;
import org.json.JSONArray;
import com.zho.model.BlogRequest;
import com.zho.model.Image;
import com.zho.model.Topic;
import com.zho.services.DatabaseService;
import java.util.List;
import java.sql.SQLException;
import com.zho.api.OpenAIClient;
import com.zho.api.wordpress.WordPressBlockClient;
import com.zho.api.wordpress.WordPressMediaClient;
import com.zho.services.DatabaseService;

public class HomePage implements StaticPage {
    private final WordPressBlockClient blockClient;
    private final OpenAIClient openAIClient;
    private final WordPressMediaClient mediaClient;
    private final DatabaseService databaseService;
    private final int pageId = 609;
    private final int imagesNeeded = 2; 

    public HomePage(WordPressBlockClient blockClient, OpenAIClient openAIClient, WordPressMediaClient mediaClient, DatabaseService databaseService) {
        this.blockClient = blockClient;
        this.openAIClient = openAIClient;
        this.mediaClient = mediaClient;
        this.databaseService = databaseService;
    }

    @Override
    public void updateStaticContent(BlogRequest request, List<Image> images) throws IOException, ParseException {
        updateHeadingAndSubheading(request);
        updateMissionParagraph(request);

        try {
            blockClient.updateTopicsSection(DatabaseService.getTopics());
        } catch (SQLException e) {
            throw new IOException("Failed to get topics from database", e);
        }
        
        // Update images
        if (!images.isEmpty()) {
            mediaClient.updateSimpleImage(pageId, images.get(0));
            if (images.size() > 1) {
                mediaClient.updateBackgroundImage(pageId, "609_fc7adf-33", images.get(1));
            }
        }
        
        // NOTE: If you add another section that needs an image:
        // 1. Update getRequiredImageCount() to return the new total
    }

    @Override
    public String getPageName() {
        return "Home";
    }

    @Override
    public int getPageId() {
        return pageId;
    }

    @Override 
    public int getRequiredImageCount() {
        return imagesNeeded; 
    }

    private String capitalizeWords(String text) {
        String[] words = text.split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1))
                      .append(" ");
            }
        }
        return result.toString().trim();
    }

    private void updateHeadingAndSubheading(BlogRequest request) throws IOException, ParseException {
        // Generate content
        System.out.println("\n=== Generating Heading ===");
        String headingPrompt = String.format(
            "Create a heading that ONLY identifies the audience and their pain point for a blog about %s. " +
            "Format: EXACTLY this format with no additions: 'For [Specific Target Audience] Who [Specific Pain Point/Challenge]' " +
            "Example: 'For Busy Professionals Who Cannot Find Time To Cook:' " +
            "Bad example: 'For busy professionals who can't find time to cook, we offer solutions' " +
            "Keep it short and focused. No solutions or additional context. " +
            "Topic: %s",
            request.getTopic(),
            request.getDescription()
        );
        String heading = capitalizeWords(openAIClient.callOpenAI(headingPrompt, 0.9));
        System.out.println("Generated heading: " + heading);

        System.out.println("\n=== Generating Subheading ===");
        String subheadingPrompt = String.format(
            "Create a fun, creative subheading that completes the story for: '%s' " +
            "Format: Start with 'We Provide' but make it exciting and memorable. " +
            "Examples: " +
            "'We Provide The Secret Sauce That Turns Kitchen Disasters Into Michelin-Worthy Magic' or " +
            "'We Provide The Digital Toolbox That Transforms Code Newbies Into Programming Rockstars' or " +
            "'We Provide The Fitness Blueprint That Turns Couch Potatoes Into Unstoppable Athletes' " +
            "Use creative metaphors, be playful but professional. Make it specific to this blog topic: %s",
            heading,
            request.getTopic()
        );
        String subheading = capitalizeWords(openAIClient.callOpenAI(subheadingPrompt, 1.0));
        System.out.println("Generated subheading: " + subheading);

        String currentContent = blockClient.getPageContent(getPageId());
        
        // Update pattern to match the entire heading block with all styling
        String headingPattern = "<!-- wp:kadence/advancedheading \\{[^}]*\"uniqueID\":\"609_a8d80a-ca\"[^}]*\\}[\\s\\S]*?<!-- /wp:kadence/advancedheading -->";
        String newHeading = "<!-- wp:kadence/advancedheading {" +
                "\"level\":1," +
                "\"uniqueID\":\"609_a8d80a-ca\"," +
                "\"align\":\"center\"," +
                "\"color\":\"palette2\"," +
                "\"typography\":\"Jost\"," +
                "\"googleFont\":true," +
                "\"fontSubset\":\"latin\"," +
                "\"fontVariant\":\"700\"," +
                "\"fontWeight\":\"700\"," +
                "\"markBorder\":\"\"," +
                "\"markBorderStyles\":[{\"top\":[null,\"\",\"\"],\"right\":[null,\"\",\"\"],\"bottom\":[null,\"\",\"\"],\"left\":[null,\"\",\"\"],\"unit\":\"px\"}]," +
                "\"tabletMarkBorderStyles\":[{\"top\":[null,\"\",\"\"],\"right\":[null,\"\",\"\"],\"bottom\":[null,\"\",\"\"],\"left\":[null,\"\",\"\"],\"unit\":\"px\"}]," +
                "\"mobileMarkBorderStyles\":[{\"top\":[null,\"\",\"\"],\"right\":[null,\"\",\"\"],\"bottom\":[null,\"\",\"\"],\"left\":[null,\"\",\"\"],\"unit\":\"px\"}]," +
                "\"textTransform\":\"none\"," +
                "\"colorClass\":\"theme-palette2\"," +
                "\"enableTextShadow\":true," +
                "\"textShadow\":[{\"enable\":false,\"color\":\"palette9\",\"blur\":1,\"hOffset\":3,\"vOffset\":3}]," +
                "\"fontSize\":[60,null,40]," +
                "\"fontHeight\":[68,null,48]," +
                "\"fontHeightType\":\"px\"" +
                "} -->\n" +
                "<h1 class=\"kt-adv-heading609_a8d80a-ca wp-block-kadence-advancedheading has-theme-palette-2-color has-text-color\" " +
                "data-kb-block=\"kb-adv-heading609_a8d80a-ca\">" + heading + "</h1>\n" +
                "<!-- /wp:kadence/advancedheading -->";
        
        // Use the proven pattern for subheading
        String subheadingPattern = "<!-- wp:kadence/advancedheading \\{[^}]*\"uniqueID\":\"609_e56131-4a\"[^}]*\\}[\\s\\S]*?<!-- /wp:kadence/advancedheading -->";
        String newSubheading = "<!-- wp:kadence/advancedheading {" +
                "\"uniqueID\":\"609_e56131-4a\"," +
                "\"htmlTag\":\"h2\"," +
                "\"color\":\"#ffffff\"," +
                "\"enableTextShadow\":true," +
                "\"fontHeight\":[null,\"\",\"\"]," +
                "\"fontSize\":[\"xl\",\"\",\"\"]," +
                "\"align\":\"center\"," +
                "\"textShadow\":[{\"vOffset\":3,\"color\":\"palette2\",\"enable\":false,\"blur\":1,\"hOffset\":4}]" +
                "} -->\n" +
                "<h2 class=\"kt-adv-heading609_e56131-4a wp-block-kadence-advancedheading\" " +
                "data-kb-block=\"kb-adv-heading609_e56131-4a\">" + subheading + "</h2>\n" +
                "<!-- /wp:kadence/advancedheading -->";
        
        String updatedContent = currentContent
            .replaceFirst(headingPattern, newHeading)
            .replaceFirst(subheadingPattern, newSubheading);
        
        blockClient.updatePageContent(getPageId(), updatedContent);
        System.out.println("WordPress update completed");
    }

    private void updateMissionParagraph(BlogRequest request) throws IOException, ParseException {
        System.out.println("\n=== Generating Mission ===");
        String missionPrompt = String.format(
            "Write a clever, memorable 2-3 sentence mission statement for a blog about %s. " +
            "First sentence must start with 'Our mission is to' followed by a creative way to say we help people. " +
            "Second sentence should explain how we do it. Optional third sentence can add personality. " +
            "Examples: " +
            "'Our mission is to turn coffee-fueled developers into coding ninjas. We blend cutting-edge tutorials with " +
            "real-world wisdom, serving up bite-sized lessons that stick. Think of us as your digital dojo, where " +
            "every bug is just another chance to level up.' " +
            "Make it fun but professional, specific to this topic: '%s'.",
            request.getDescription(),
            request.getTopic()
        );
        String mission = openAIClient.callOpenAIWithSystemPrompt(missionPrompt, databaseService.getSystemPrompt());
        System.out.println("Generated mission: " + mission);
        System.out.println("WordPress update completed");

        // Update WordPress content
        JSONObject missionProps = new JSONObject()
            .put("uniqueID", "609_29304f-69")
            .put("markBorder", "")
            .put("htmlTag", "p");

        blockClient.updateBlock(
            getPageId(),
            WordPressBlockClient.BlockType.KADENCE_HEADING,
            "609_29304f-69",
            mission,
            missionProps
        );
    }

    @Override
    public String getTitleTemplate() {
        return "Generate a title for the Home page"; // Won't be used
    }

    @Override
    public String getMetaDescriptionTemplate() {
        try {
            BlogRequest blogInfo = new DatabaseService().getBlogInfo();
            String siteName = new WordPressBlockClient().getSiteTitle();
            return String.format(
                "Write a single compelling sentence (120-150 characters) that explains how %s helps readers with %s. " +
                "Focus on the main value proposition.",
                siteName,
                blogInfo.getTopic()
            );
        } catch (IOException | ParseException | SQLException e) {
            System.err.println("Error getting blog info: " + e.getMessage());
            return "Write a single compelling sentence (120-150 characters) that explains the blog's main value proposition.";
        }
    }

    @Override
    public boolean hasHardcodedTitle() {
        return true;
    }

    @Override
    public String getHardcodedTitle() {
        try {
            WordPressBlockClient wpClient = new WordPressBlockClient();
            String siteTitle = wpClient.getSiteTitle();
            String siteSlogan = wpClient.getSiteSlogan();
            return String.format("%s – %s", siteTitle, siteSlogan);
        } catch (IOException | ParseException e) {
            System.err.println("Error getting site title or slogan: " + e.getMessage());
            return "Home"; // Fallback
        }
    }

    public static void main(String[] args) {
        try {
            // Initialize dependencies
            WordPressBlockClient blockClient = new WordPressBlockClient();
            OpenAIClient openAIClient = new OpenAIClient();
            WordPressMediaClient mediaClient = new WordPressMediaClient();
            DatabaseService databaseService = new DatabaseService();
            // Create HomePage instance
            HomePage homePage = new HomePage(blockClient, openAIClient, mediaClient, databaseService);
            

            blockClient.updateTopicsSection(DatabaseService.getTopics());
            // Test updateHeadingAndSubheading
            System.out.println("Testing heading and subheading update...");
            
            
        } catch (Exception e) {
            System.err.println("Error during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }

} 