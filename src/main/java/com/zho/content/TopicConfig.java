package com.zho.content;

import com.zho.model.Topic;
import java.util.List;
import java.io.IOException;
import org.apache.hc.core5.http.ParseException;
import com.zho.wordpress.WordPressUpdater;
import java.util.ArrayList;
import com.zho.database.TopicDAO;
import java.sql.SQLException;

public class TopicConfig {
    private final AIContentGenerator generator;
    private final WordPressUpdater wpUpdater;
    private final TopicDAO topicDAO;

    public TopicConfig() {
        this.generator = new AIContentGenerator();
        this.wpUpdater = new WordPressUpdater();
        this.topicDAO = new TopicDAO();
    }

    public void setupTopics(BlogNiche niche) throws IOException, ParseException, SQLException {
        // Clear existing categories first
        wpUpdater.clearCategories();
        
        // Create topics table if it doesn't exist
        topicDAO.createTopicsTable();
        
        // Clear existing topics
        topicDAO.clearTopics();
        
        // Generate topics
        List<Topic> topics = generateTopics(niche);
        
        // Save to database and create WordPress categories
        for (Topic topic : topics) {
            topicDAO.insertTopic(topic);
            wpUpdater.createCategory(topic.getTitle());
        }
        
        // Print count for verification
        System.out.println("Total topics in database: " + topicDAO.getTopicCount());
        
        // Update WordPress
        updateTopicsSection(topics);
    }

    private List<Topic> generateTopics(BlogNiche niche) {
        String prompt = String.format(
            "Generate 6 specific subtopics for a blog about %s. For each subtopic:" +
            "\n1. Create a clear, concise title (1-2 words)" +
            "\n2. Write a simple engaging sentence describing the subtopic" +
            "\nFormat: title|description for each topic, one per line",
            niche.getDisplayName()
        );

        String response = generator.callOpenAI(prompt);
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

    private void updateTopicsSection(List<Topic> topics) throws IOException, ParseException {
        StringBuilder content = new StringBuilder();
        
        // Start columns container
        content.append("<!-- wp:columns {\"className\":\"nfd-text-base nfd-gap-md nfd-gap-y-xl\"} -->\n")
               .append("<div class=\"nfd-text-base nfd-gap-md nfd-gap-y-xl wp-block-columns\">");

        // Add topics (3 per row)
        for (int i = 0; i < topics.size(); i++) {
            // Debug print
            System.out.println("Processing topic " + (i+1) + " of " + topics.size());
            
            if (i % 3 == 0 && i > 0) {
                System.out.println("Starting new row");
                content.append("</div><!-- /wp:columns -->\n")
                       .append("<!-- wp:columns {\"className\":\"nfd-text-base nfd-gap-md nfd-gap-y-xl\"} -->\n")
                       .append("<div class=\"nfd-text-base nfd-gap-md nfd-gap-y-xl wp-block-columns\">");
            }
            content.append(generateTopicCard(topics.get(i), (i % 2 == 1)));
        }

        // Close columns container
        content.append("</div><!-- /wp:columns -->");

        // Update WordPress
        wpUpdater.updatePageSection(609, "topics-section", content.toString());
    }

    private String generateTopicCard(Topic topic, boolean isDark) {
        String themeClass = isDark ? " nfd-theme-dark nfd-bg-surface is-style-nfd-theme-dark" : "";
        String borderStyle = isDark ? " style=\"border-width:1px\"" : "";

        return String.format(
            "<!-- wp:column --><div class=\"wp-block-column\">" +
            "<!-- wp:group {\"className\":\"nfd-shadow-xs nfd-p-card-square nfd-rounded nfd-gap-md%s\"%s} -->" +
            "<div class=\"nfd-shadow-xs nfd-p-card-square nfd-rounded nfd-gap-md%s wp-block-group\"%s>" +
            "<!-- wp:heading {\"level\":3,\"className\":\"nfd-text-md nfd-text-contrast\"} -->" +
            "<h3 class=\"nfd-text-md nfd-text-contrast wp-block-heading\">%s</h3>" +
            "<!-- /wp:heading -->" +
            "<!-- wp:paragraph {\"className\":\"nfd-text-balance nfd-text-faded\"} -->" +
            "<p class=\"nfd-text-balance nfd-text-faded\">%s</p>" +
            "<!-- /wp:paragraph -->" +
            "<!-- wp:buttons --><div class=\"wp-block-buttons\">" +
            "<!-- wp:button --><div class=\"wp-block-button\">" +
            "<a class=\"wp-block-button__link wp-element-button\" href=\"%s\">Explore %s</a>" +
            "</div><!-- /wp:button --></div><!-- /wp:buttons -->" +
            "</div><!-- /wp:group --></div><!-- /wp:column -->",
            themeClass, borderStyle, themeClass, borderStyle,
            topic.getTitle(), topic.getDescription(), topic.getLink(), topic.getTitle()
        );
    }
} 