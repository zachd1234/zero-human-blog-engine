package com.zho.services.DailyPosts;

import java.io.IOException;
import org.apache.hc.core5.http.ParseException;
import java.util.List;
import java.util.ArrayList;
import com.zho.api.OpenAIClient;
import java.util.stream.Collectors;

public class PostWriterService {
    private final OpenAIClient openAIClient;
    private String currentOutline;
    private List<String> generatedSections;

    public PostWriterService() {
        this.openAIClient = new OpenAIClient();
        this.generatedSections = new ArrayList<>();
    }

    public String createNewBlogPost(String keyword, String title) throws IOException {
        currentOutline = generateOutline(keyword);
        System.out.println("Generated outline:\n" + currentOutline);

        List<OutlineSection> sectionTitles = parseSectionTitles(currentOutline);
        StringBuilder fullPost = new StringBuilder();
        
        for (OutlineSection sectionTitle : sectionTitles) {
            System.out.println("Generating section: " + sectionTitle.title);
            String section = generateSectionWithRetry(sectionTitle, 3);
            fullPost.append(section).append("\n\n");
        }

        return convertMarkdownToHtml(fullPost.toString());
    }

    private static class OutlineSection {
        String title;
        List<String> points;
        boolean isH3;
        
        OutlineSection(String title, boolean isH3) {
            this.title = title;
            this.points = new ArrayList<>();
            this.isH3 = isH3;
        }
    }

    private List<OutlineSection> parseSectionTitles(String outline) {
        List<OutlineSection> sections = new ArrayList<>();
        OutlineSection currentSection = null;
        
        // Split by newline and process each complete line
        for (String line : outline.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // Only process complete lines (not truncated)
            if (line.endsWith("...") || line.length() < 10) continue;
            
            if (line.matches("^## \\d+\\..*")) {
                String title = line.replaceFirst("^## \\d+\\.\\s*", "").trim();
                currentSection = new OutlineSection(title, false);
                sections.add(currentSection);
                System.out.println("\nFound H2 Section: " + title);
            } else if (line.matches("^### .*")) {
                String title = line.replaceFirst("^### \\s*", "").trim();
                currentSection = new OutlineSection(title, true);
                sections.add(currentSection);
                System.out.println("\nFound H3 Section: " + title);
            } else if (line.matches("^\\d+\\.\\d+\\s+.*") && currentSection != null) {
                String point = line.replaceFirst("^\\d+\\.\\d+\\s+", "").trim();
                // Check if this point is already added (avoid duplicates)
                if (!currentSection.points.contains(point)) {
                    currentSection.points.add(point);
                    System.out.println("  - Added Point: " + point);
                }
            }
        }
        
        return sections;
    }

    private String analyzeAudience(String keyword) throws IOException {
        String prompt = String.format(
            "Given the keyword '%s':\n\n" +
            "Who is the likely target audience searching for this and what are they looking for? " + 
            "respond in a short sentence",
            keyword
        );
        
        System.out.println("\n=== Analyzing Target Audience ===");
        String analysis = openAIClient.callGPT4(prompt);
        System.out.println("Audience Analysis:\n" + analysis + "\n");
        return analysis;
    }

    public String generateOutline(String keyword) throws IOException {
        // First, get audience and take
        String audienceAnalysis = analyzeAudience(keyword);
        
        String prompt = String.format(
            "Generate an outline for a blog post targeting the keyword '%s'. " + 
            "The blog should satisfy the search intent of the target audience %n\n" +
            "Structure requirements:\n" +
            "- First section MUST directly answer the search query/main question immediately\n" +
            "- Each section (H2 and H3) should have 2-4 key points\n" +
            "- Points can be brief ideas or concepts to write about\n\n" +
            "Format:\n" +
            "# Main Title\n\n" +
            "## 1. [Section Title]\n" +
            "1.1 [First point for the paragraph]\n" +
            "1.2 [Second point for the paragraph]\n" +
            "1.3 [Third point for the paragraph]\n\n" +
            "### [Subsection Title]\n" +
            "1.4 [First point for this subsection]\n" +
            "1.5 [Second point for this subsection]\n\n" +
            "## 2. [Next Section]\n" +
            "[...etc...]\n\n" +
            "Return only the outline with proper markdown formatting.",
            audienceAnalysis,
            keyword
        );
        
        this.currentOutline = openAIClient.callGPT4(prompt);
        return this.currentOutline;
    }

    public String generateFullContent(String outline) throws IOException {
        String prompt = String.format(
            "now given outline, write a 1,500 word (minimum) article using the outline above, " +
            "taking into account the audience and the keyword." + 
            " Use a neutral confident, knowledgeable,clear tone." +
            "Use list formatting as much as possible, where appropriate. " + 
            "Structure the format of the article for maximum scannability and readability. " +
            "Write with simple sentence structure and easy to read vocabulary. " +
            "Gather inspiration from other successful, high-ranking articles on the same topic. " +
            "Outline:   \n" + //
            outline);

        // Try up to 3 times with exponential backoff
        int maxRetries = 5;
        int retryCount = 0;
        int baseDelay = 2000; // 2 seconds

        while (retryCount < maxRetries) {
            try {
                return openAIClient.callGPT4(prompt);
            } catch (IOException e) {
                retryCount++;
                if (retryCount == maxRetries) {
                    throw new IOException("Failed to generate content after " + maxRetries + " attempts: " + e.getMessage(), e);
                }
                
                int delay = baseDelay * (1 << retryCount); // 2s, 4s, 8s
                System.out.printf("Content generation failed, attempt %d of %d. Retrying in %d ms...\n", 
                    retryCount, maxRetries, delay);
                
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted during retry delay", ie);
                }
            }
        }
        
        throw new IOException("Failed to generate content after " + maxRetries + " attempts");
    }

    private String generateSectionWithRetry(OutlineSection section, int maxRetries) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // Add delay between attempts (longer for later attempts)
                if (attempt > 1) {
                    int delaySeconds = attempt * 30; // 30s, 60s, 90s between retries
                    System.out.println("Waiting " + delaySeconds + " seconds before retry " + attempt + "...");
                    Thread.sleep(delaySeconds * 1000);
                }

                System.out.println("\n=== Generating Section (Attempt " + attempt + "/" + maxRetries + ") ===");
                System.out.println("Title: " + section.title);
                String content = generateSection(section);
                
                // Verify we got meaningful content
                if (content != null && content.length() > 100) {
                    return content;
                } else {
                    System.out.println("Generated content too short, retrying...");
                }
                
            } catch (IOException e) {
                System.err.println("Attempt " + attempt + " failed: " + e.getMessage());
                if (attempt == maxRetries) {
                    return "## " + section.title + "\n\n[Content generation failed after " + maxRetries + " attempts]\n\n";
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "## " + section.title + "\n\n[Content generation interrupted]\n\n";
            }
        }
        
        return "## " + section.title + "\n\n[Content generation failed]\n\n";
    }

    public String generateSection(OutlineSection section) throws IOException {
        System.out.println("\n=== Generating Section ===");
        System.out.println("Title: " + section.title);
        System.out.println("Type: " + (section.isH3 ? "H3 (subsection)" : "H2 (main section)"));
        System.out.println("Points to cover:");
        section.points.forEach(point -> System.out.println("  - " + point));
        
        String prompt = String.format(
            "You are tasked with writing a specific section in a larger blog post.\n\n" +
            "Section Title: %s\n" +
            "Section Level: %s\n\n" +
            "Use these points to form a cohesive section:\n%s\n\n" +
            "Instructions:\n" +
            "- Combine the points into a flowing section\n" +
            "- Use list formatting, where appropriate. \n" +
            "- Add transitions between points\n" +
            "- Maintain the section's heading (%s)\n" +
            "- Expand on the points naturally",
            section.title,
            section.isH3 ? "H3 (subsection)" : "H2 (main section)",
            section.points.stream()
                .map(point -> "- " + point)
                .collect(Collectors.joining("\n")),
            section.isH3 ? "### " + section.title : "## " + section.title
        );

        String content = openAIClient.callOpenAIWithRateLimit(prompt);
        System.out.println("\nGenerated content length: " + content.length() + " characters");
        return content;
    }
    
    public static void main(String[] args) {
        try {
            // Initialize dependencies
            OpenAIClient openAIClient = new OpenAIClient();
            PostWriterService engine = new PostWriterService();
            
            // Test blog post generation
            String keyword = "environmental impact assessment report for housing development";
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

    private String convertMarkdownToHtml(String markdown) {
        // Remove any "Error generating content" messages
        String html = markdown.replaceAll("(?m)^Error generating content$", "");
        
        // Convert headers (including those with numbers)
        html = html.replaceAll("(?m)^# (.+)$", "<h1>$1</h1>")
                  .replaceAll("(?m)^## (.+)$", "<h2>$1</h2>")
                  .replaceAll("(?m)^### (.+)$", "<h3>$1</h3>")
                  .replaceAll("(?m)^#### (.+)$", "<h4>$1</h4>");
        
        // Convert bold text
        html = html.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
        
        // Convert bullet points (both - and –)
        html = html.replaceAll("(?m)^[–-]\\s+(.+)$", "<ul><li>$1</li></ul>")
                  .replaceAll("(?m)^\\*\\s+(.+)$", "<ul><li>$1</li></ul>");
        
        // Fix multiple consecutive list items
        html = html.replaceAll("</ul>\\s*<ul>", "");
        
        // Convert paragraphs (lines with content)
        html = html.replaceAll("(?m)^(?!<[hul])(.+)$", "<p>$1</p>");
        
        // Clean up empty lines and extra spaces
        html = html.replaceAll("(?m)^\\s*$\\n", "")
                  .replaceAll("\\n\\n+", "\n\n")
                  .trim();
        
        return html;
    }
}