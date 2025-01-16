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

    public String generateSection(String sectionTitle) throws IOException {
        String prompt = String.format(
            "Write the next section titled '%s' for the blog post. " +
            "Use a conversational tone, first-person perspective, and include examples. " +
            "Make it engaging and scannable with lists where appropriate. " +
            "Previous sections context: %s",
            sectionTitle,
            String.join("\n\n", generatedSections)
        );

        String section = openAIClient.callGPT4(prompt);
        generatedSections.add(section);
        return section;
    }
}