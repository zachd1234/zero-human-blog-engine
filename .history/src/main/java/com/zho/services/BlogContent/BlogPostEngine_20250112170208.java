package com.zho.services.BlogContent;

import java.io.IOException;
import org.apache.hc.core5.http.ParseException;
import java.util.List;
import java.util.ArrayList;
import com.zho.api.OpenAIClient;

public class BlogPostEngine {
    private final OpenAIClient openAIClient;
    private String currentOutline;
    private List<String> generatedSections;

    public BlogPostEngine(OpenAIClient openAIClient) {
        this.openAIClient = openAIClient;
        this.generatedSections = new ArrayList<>();
    }

    public String createNewBlogPost(String keyword) throws IOException {
        currentOutline = generateOutline(keyword);
        System.out.println("Generated outline:\n" + currentOutline);

        // Parse outline into sections 
        List<String> sectionTitles = parseSectionTitles(currentOutline);
        
        // Generate each section
        StringBuilder fullPost = new StringBuilder();
        for (String sectionTitle : sectionTitles) {
            System.out.println("Generating section: " + sectionTitle);
            String section = generateSection(sectionTitle, keyword);
            fullPost.append(section).append("\n\n");
        }

        return fullPost.toString();
    }


    private List<String> parseSectionTitles(String outline) {
        List<String> titles = new ArrayList<>();
        for (String line : outline.split("\n")) {
            // Match lines starting with "## number." format
            if (line.matches("^## \\d+\\..*")) {
                // Remove the "## " and number prefix
                String title = line.replaceFirst("^## \\d+\\.\\s*", "").trim();
                titles.add(title);
            }
        }
        return titles;
    }

    public String generateOutline(String keyword) throws IOException {
        String prompt = String.format(
            "You're an experienced writer with a skill for creating highly engaging blog posts. " +
            "Create an outline for a blog post about: '%s'. " +
            "Use SEO best practices for headings. " +
            "Format the outline with clear section numbers. " +
            "Return only the outline.",
            keyword
        );
        
        this.currentOutline = openAIClient.callGPT4(prompt);
        return this.currentOutline;
    }

    public String generateSection(String sectionTitle, String keyword) throws IOException {
        String prompt = String.format(
            "Writing a blog post about '%s'. " +
            "Full outline:\n%s\n\n" +
            "Write the next section titled '%s'. " +
            "Use a conversational tone, first-person perspective, and include examples. " +
            "Make it engaging and scannable with lists where appropriate. " +
            "Context from previous sections:\n%s",
            keyword,
            currentOutline,
            sectionTitle,
            String.join("\n\n", generatedSections)
        );

        String section = openAIClient.callGPT4(prompt);
        generatedSections.add(section);
        return section;
    }

    public static void main(String[] args) {
        try {
            // Initialize dependencies
            OpenAIClient openAIClient = new OpenAIClient();
            BlogPostEngine engine = new BlogPostEngine(openAIClient);
            
            // Test blog post generation
            String keyword = "do pickles lower testosterone";
            System.out.println("Testing blog post generation for: " + keyword);
            // Generate full post
            String fullPost = engine.createNewBlogPost(keyword);
            System.out.println("\nGenerated Full Post:");
            System.out.println(fullPost);
            
        } catch (Exception e) {
            System.err.println("Error during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
}