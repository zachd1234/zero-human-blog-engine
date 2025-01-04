package com.zho.services;

import com.zho.model.BlogRequest;
import com.zho.api.wordpress.WordPressMediaClient;
import com.zho.api.NounProjectClient;
import com.zho.api.OpenAIClient;
import org.json.JSONObject;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.apache.hc.core5.http.ParseException;

public class LogoAndFaviconService {
    private final WordPressMediaClient mediaClient;
    private final NounProjectClient nounClient;
    private final OpenAIClient openAIClient;

    public LogoAndFaviconService(WordPressMediaClient mediaClient) {
        this.mediaClient = mediaClient;
        this.nounClient = new NounProjectClient();
        this.openAIClient = new OpenAIClient();
    }

    private String generateIconSearchTerm(BlogRequest request) throws IOException {
        String prompt = String.format(
            "Given a blog topic about '%s', suggest ONE simple, iconic symbol that would work well as a logo. " +
            "Requirements:\n" +
            "1. Must be a common, widely-recognized symbol that definitely exists in icon libraries\n" +
            "2. Should be simple enough to work as a small favicon\n" +
            "3. If the topic is very specific, suggest a more general but related symbol\n" +
            "4. Avoid complex or compound concepts\n\n" +
            "Examples:\n" +
            "- 'Advanced Quantum Computing' → 'atom'\n" +
            "- 'Mediterranean Cooking Tips' → 'chef-hat'\n" +
            "- 'Professional Dog Training' → 'paw'\n" +
            "- 'Beginner JavaScript Tutorials' → 'code'\n\n" +
            "Respond with ONLY the search term, nothing else.",
            request.getTopic()
        );

        return openAIClient.callOpenAI(prompt, 0.7).toLowerCase().trim();
    }

    public void generateAndUploadBranding(BlogRequest request) throws IOException, ParseException {
        // Generate smart search term
        String searchTerm = generateIconSearchTerm(request);
        System.out.println("Generated icon search term: " + searchTerm);

        // Get icon from Noun Project
        JSONObject searchResult = nounClient.searchIcons(searchTerm);
        
        if (searchResult.has("icons") && searchResult.getJSONArray("icons").length() > 0) {
            String iconUrl = searchResult.getJSONArray("icons")
                .getJSONObject(0)
                .getString("thumbnail_url");
            
            // Download and get the tinted icon
            byte[] iconData = nounClient.downloadIcon(iconUrl);
            
            // Convert byte array to BufferedImage
            BufferedImage iconImage = ImageIO.read(new ByteArrayInputStream(iconData));
            
            // Update favicon using the icon
            mediaClient.updateFavicon(iconImage);
            System.out.println("Successfully updated favicon with icon for: " + searchTerm);
        } else {
            // If first attempt fails, try a fallback term
            String fallbackTerm = generateIconSearchTerm(new BlogRequest("general " + request.getTopic(), "general " + request.getDescription()));
            System.out.println("Trying fallback search term: " + fallbackTerm);
            
            searchResult = nounClient.searchIcons(fallbackTerm);
            if (searchResult.has("icons") && searchResult.getJSONArray("icons").length() > 0) {
                String iconUrl = searchResult.getJSONArray("icons")
                    .getJSONObject(0)
                    .getString("thumbnail_url");
                
                byte[] iconData = nounClient.downloadIcon(iconUrl);
                BufferedImage iconImage = ImageIO.read(new ByteArrayInputStream(iconData));
                mediaClient.updateFavicon(iconImage);
                System.out.println("Successfully updated favicon with fallback icon for: " + fallbackTerm);
            } else {
                throw new IOException("No icons found for topic or fallback: " + request.getTopic());
            }
        }

        private BufferedImage generateLogo(BufferedImage icon, String topic) throws IOException {
            // TODO: Implement logo generation using the icon and topic
            // This will combine the icon with text and any other design elements
            return icon;  // Temporary return until implementation
        }


    }

    public static void main(String[] args) {
        try {
            // Create test blog requests
            BlogRequest[] testRequests = {
                new BlogRequest("Mediterranean Cooking Tips", "Authentic Mediterranean recipes and techniques"),
            };

            // Initialize service
            WordPressMediaClient mediaClient = new WordPressMediaClient();
            LogoAndFaviconService service = new LogoAndFaviconService(mediaClient);

            // Test each request
            for (BlogRequest request : testRequests) {
                System.out.println("\n=== Testing with topic: " + request.getTopic() + " ===");
                try {
                    service.generateAndUploadBranding(request);
                    System.out.println("✓ Success: Branding generated and uploaded for " + request.getTopic());
                } catch (Exception e) {
                    System.err.println("✗ Failed: " + e.getMessage());
                    e.printStackTrace();
                }
                // Add a small delay between requests
                Thread.sleep(2000);
            }

        } catch (Exception e) {
            System.err.println("Error during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 