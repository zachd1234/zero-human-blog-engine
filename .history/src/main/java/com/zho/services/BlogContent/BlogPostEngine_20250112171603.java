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
            "You're an experienced writer with a skill for creating highly engaging blog posts that capture the attention of your audience and deliver value." +
            "Create an outline for a blog post about: '%s'. " +
            "Gather inspiration from other successful articles on this topic to make sure we’re not leaving out any important points and sections." + 
            "Use SEO best practices to ensure proper use of keywords in headings." + 
            "Try to satisfy the search intent as fast as possible in the article. So give away the answer right away." +
            "1. H1: Use '# ' for the main title (only one)\n" +
            "2. H2: Use '## ' for main sections (e.g., '## 1. Introduction')\n" +
            "3. H3: Use '### ' for subsections\n" +
            "Additional requirements:\n" +
            "- Number main sections sequentially (1, 2, 3, etc.)\n" +
            "- Include relevant subsections (H3) where needed\n" +
            "- Use keywords naturally in headings\n" +
            "- Ensure heading hierarchy is never skipped (don't go from H1 to H3)\n" +
            "Return only the outline with proper markdown formatting.",
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
            "taking into account the same audience, topic, and tone guidelines. " +
            "Use list formatting as much as possible, where appropriate. " + 
            "Structure the format of the article for maximum scannability and readability. " +
            "Write with simple sentence structure and easy to read vocabulary. " +
            "Gather inspiration from other successful, high-ranking articles on the same topic. " +
            "Talk in the first person, speak from experience and tell stories when possible. " +
            "Use a conversational tone, first-person perspective, and include examples. " +
            "Make it engaging and scannable with lists where appropriate. " +
            "\nFormat the section with proper markdown heading hierarchy:\n" +
            "- Use '## ' for the section title\n" +
            "- Use '### ' for any subsections\n\n" +
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