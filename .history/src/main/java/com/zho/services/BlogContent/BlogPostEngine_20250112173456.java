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

    private String generateTargetAudience(String keyword) throws IOException {
        String prompt = String.format(
            "Analyze who would search for '%s' and create a detailed target audience description. " +
            "Consider:\n" +
            "- Demographics (age, gender if relevant)\n" +
            "- Level of expertise/experience\n" +
            "- Current situation/pain points\n" +
            "- What they're trying to achieve\n" +
            "Return only the audience description in a concise paragraph.",
            keyword
        );
        return openAIClient.callGPT4(prompt);
    }

    private String generateQuickTake(String keyword) throws IOException {
        String prompt = String.format(
            "Based on the search query '%s', generate an expert opinion/stance on this topic. " +
            "Consider:\n" +
            "- What's the truth behind this query?\n" +
            "- What's commonly misunderstood?\n" +
            "- What's the key insight people need?\n" +
            "Return a clear, direct take on the topic in 2-3 sentences.",
            keyword
        );
        return openAIClient.callGPT4(prompt);
    }

    public String generateOutline(String keyword) throws IOException {
        // First, get audience and take
        String audience = generateTargetAudience(keyword);
        String quickTake = generateQuickTake(keyword);
        
        String prompt = String.format(
            "You're an experienced writer with a skill for creating highly engaging blog posts " +
            "that capture the attention of your audience and deliver value.\n\n" +
            "Create an outline for blog post on the topic of '%s' for this audience:\n%s\n\n" +
            "When drafting the outline, take into account this key point of view:\n%s\n\n" +
            "Structure requirements:\n" +
            "- First section MUST directly answer the search query/main question immediately\n" +
            "- Follow with supporting evidence, details, and deeper insights\n" +
            "Only talk about things relevant to what the reader is searching for." +
            "- End with actionable takeaways or next steps\n\n" +
            "Gather inspiration from other successful articles on this topic to make sure " +
            "we're not leaving out any important points and sections. " +
            "Use SEO best practices to ensure proper use of keywords in headings.\n\n" +
            "Format your headings like this:\n" +
            "1. H1: Use '# ' for the main title (only one)\n" +
            "2. H2: Use '## ' for main sections (e.g., '## 1. Introduction')\n" +
            "3. H3: Use '### ' for subsections\n" +
            "Additional requirements:\n" +
            "- Number main sections sequentially (1, 2, 3, etc.)\n" +
            "- Include relevant subsections (H3) where needed\n" +
            "- Use keywords naturally in headings\n" +
            "- Ensure heading hierarchy is never skipped (don't go from H1 to H3)\n" +
            "Return only the outline with proper markdown formatting.",
            keyword,
            audience,
            quickTake
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