package com.zho.services.BlogSetup;

import com.zho.model.Topic;
import java.util.List;
import java.io.IOException;
import org.apache.hc.core5.http.ParseException;
import com.zho.api.wordpress.WordPressClient;
import java.util.ArrayList;
import com.zho.model.BlogRequest;
import com.zho.api.OpenAIClient;
import java.sql.SQLException;
import com.zho.services.DatabaseService;
import com.zho.model.Site;

public class CategoryService {
    private final OpenAIClient openai;
    private final WordPressClient wpClient; 
    private final DatabaseService databaseService;

    public CategoryService() {
        this.openai = new OpenAIClient();
        this.wpClient = new WordPressClient();
        this.databaseService = new DatabaseService();
    }

    public void setupSubtopics(BlogRequest niche) throws IOException, ParseException, SQLException {
        // Clear existing categories first
        wpClient.categories().clearCategories();
        
        List<Topic> topics = generateTopics(niche);
        
        for (Topic topic : topics) {
            wpClient.categories().createCategory(topic.getTitle());
            databaseService.insertTopic(topic);
        }
        

        //updateTopicsSection(topics);
    }

    private List<Topic> generateTopics(BlogRequest niche) {
        String prompt = String.format(
            "Generate 6 specific subtopics for a blog about %s. For each subtopic:" +
            "\n- Create a clear, concise title (1-2 words maximum)" +
            "\n- Write a simple engaging sentence describing the subtopic" +
            "\nFormat: title|description" +
            "\nExample format:" +
            "\nBeginner Tips|Essential tips and strategies for those just starting their journey." +
            "\nAdvanced Skills|Take your expertise to the next level with professional techniques." +
            "\n\nRespond with 6 topics in this exact format, no numbering or extra text.",
            niche.getTopic()
        );

        String response = openai.callOpenAI(prompt);
        return parseTopicsResponse(response);
    }

    private List<Topic> parseTopicsResponse(String response) {
        List<Topic> topics = new ArrayList<>();
        Site currentSite = Site.getCurrentSite();
        
        // Get the base URL from the current site and transform it to category URL
        String baseUrl = currentSite.getUrl()
            .replace("/wp-json/wp/v2/", "")  // Remove the API path
            .replace("https://", "https://")  // Remove double https if any
            .replaceAll("/$", "");  // Remove trailing slash if present
        
        String[] lines = response.split("\n");

        for (String line : lines) {
            String[] parts = line.split("\\|");
            if (parts.length == 2) {
                String title = parts[0].trim();
                // Validate title length
                if (title.split("\\s+").length > 2) {
                    // If title is too long, take first two words
                    String[] words = title.split("\\s+");
                    title = words[0] + " " + words[1];
                }
                
                // Create URL-friendly category slug
                String categorySlug = title.toLowerCase()
                    .replaceAll("\\s+", "-")     // Replace spaces with hyphens
                    .replaceAll("[^a-z0-9-]", "") // Remove special characters
                    .replaceAll("-+", "-");       // Replace multiple hyphens with single hyphen
                
                String categoryUrl = baseUrl + "/category/" + categorySlug + "/";
                
                topics.add(new Topic(
                    title,
                    parts[1].trim(),
                    categoryUrl
                ));
            }
        }
        return topics;
    }
    
    // Test method
    public static void main(String[] args) {
        try {
            CategoryService service = new CategoryService();
            BlogRequest testRequest = new BlogRequest(
                "Rucking",
                "All Things Rucking. gear reviews, training tips, fun facts, and more"
            );
            
            BlogRequest testRequest2 = new BlogRequest(
                "Y Combinator",
                "Y Combinator"
            );

            service.setupSubtopics(testRequest);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 