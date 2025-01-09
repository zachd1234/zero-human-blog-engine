package com.zho.services;

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
        String blogLink = "https://mbt.dsc.mybluehost.me/blog-2/";
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
                topics.add(new Topic(
                    title,
                    parts[1].trim(),
                    blogLink
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
                "fishing",
                "fishing tips and tricks"
            );
            
            service.setupSubtopics(testRequest);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 