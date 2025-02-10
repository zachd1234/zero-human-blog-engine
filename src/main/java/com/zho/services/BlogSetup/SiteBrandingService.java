package com.zho.services.BlogSetup;

import com.zho.model.BlogRequest;
import com.zho.api.wordpress.WordPressMediaClient;
import com.zho.api.NounProjectClient;
import com.zho.api.OpenAIClient;
import org.json.JSONObject;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.awt.color.ColorSpace;
import com.zho.api.GoDaddyClient;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.hc.core5.http.ParseException;
import java.io.File;
import java.util.List;
import com.zho.api.RecraftAPIClient;
import java.net.URL;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import java.net.HttpURLConnection;
import java.io.InputStream;
import com.zho.config.ConfigManager;
import java.util.concurrent.TimeUnit;

public class SiteBrandingService {
    private final WordPressMediaClient mediaClient;
    private final NounProjectClient nounClient;
    private final OpenAIClient openAIClient;
    private final RecraftAPIClient recraftClient;

    public SiteBrandingService(WordPressMediaClient mediaClient) {
        this.mediaClient = mediaClient;
        this.nounClient = new NounProjectClient();
        this.openAIClient = new OpenAIClient();
        this.recraftClient = new RecraftAPIClient();
    }


    public void generateAndUploadBranding(BlogRequest request) throws IOException, ParseException {

        String blogName = generateBlogName(request);
        System.out.println("Generated blog name: " + blogName);

        // Select domain name
        String domain = selectDomain(blogName);
        System.out.println("Selected domain name: " + domain);

        // Generate tagline
        String tagline = generateTagline(blogName, request.getDescription());
        System.out.println("Generated tagline: " + tagline);
        String searchTerm = generateIconSearchTerm(request);
        BufferedImage icon = fetchIconFromNounProject(searchTerm);
     
        mediaClient.updateSiteName(blogName);
        mediaClient.updateSiteTagline(tagline);
        mediaClient.updateFavicon(icon);

        //logo. AI first, then fallback to simple 
        try {
            String logoUrl = recraftClient.generateAILogo(blogName, request.getDescription());
            mediaClient.updateSiteLogoFromUrl(logoUrl);

        } catch (IOException  e) {
            System.err.println("Error during AI logo download: " + e.getMessage());
            BufferedImage logo = generateSimpleLogo(icon, blogName);
            mediaClient.updateSiteLogo(logo);
            mediaClient.updateFavicon(icon);
        }
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

    private String generateBlogName(BlogRequest request) throws IOException {
        // First call: Generate 10 diverse names
        String generateNamesPrompt = String.format(
            "Generate 10 unique blog names for a blog about '%s'. Requirements:\n" +
            "- Length: 4-12 letters\n" +
            "- Be creative and innovative with names \n" +
            "- Must be memorable and brandable\n" +
            "- Can be any of these types:\n" +
            "  * Compound words (e.g., CodeCraft)\n" +
            "  * Emotive names (e.g., Wanderlust)\n" +
            "  * Descriptive names (e.g., SwiftLearn)\n" +
            "  * Acronym names (e.g., DEV)\n" +
            "  * Alphanumeric names (e.g., Tech42)\n" +
            "  * Metaphor names (e.g., Lighthouse)\n" +
            "  * Technical names (e.g., Nexus)\n\n" +
            "Format: Output ONLY the names, one per line, numbered 1-10.",
            request.getTopic()
        );

        String namesList = openAIClient.callGPT4(generateNamesPrompt, 1);
        System.out.println("Generated names:\n" + namesList);

        // Second call: Pick the best name
        String pickBestPrompt = String.format(
            "From these blog names for a '%s' blog:\n\n%s\n\n" +
            "Select the BEST name based on these criteria:\n" +
            "1. Memorability\n" +
            "2. Brand potential\n" +
            "3. Relevance to topic\n" +
            "4. Uniqueness\n" +
            "5. Easy to spell and pronounce\n\n" +
            "Respond with ONLY the chosen name, nothing else.",
            request.getTopic(),
            namesList
        );

        String finalName = openAIClient.callGPT4(pickBestPrompt);
        System.out.println("Selected name: " + finalName);
        
        return finalName.trim();
    }

    private BufferedImage generateSimpleLogo(BufferedImage icon, String blogName) throws IOException, ParseException {
        System.out.println("Creating logo with name: " + blogName);
        
        // Create combined logo
        int width = 1000;
        int height = 300;
        BufferedImage logo = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = logo.createGraphics();
        
        // Set up rendering hints
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Draw white background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);
        
        // Draw icon in teal color (#049F82)
        g2d.setColor(new Color(0x04, 0x9F, 0x82));  // Teal color
        g2d.drawImage(icon, 20, 20, 240, 240, null);
        
        // Draw text in dark gray (#222222)
        g2d.setColor(new Color(0x22, 0x22, 0x22));  // Dark gray
        g2d.setFont(new Font("Arial", Font.BOLD, 72));
        FontMetrics fm = g2d.getFontMetrics();
        int textY = (height - fm.getHeight()) / 2 + fm.getAscent();
        g2d.drawString(blogName, 220, textY);
        
        g2d.dispose();
        return logo;
    }


    private BufferedImage fetchIconFromNounProject(String searchTerm) throws IOException {
        JSONObject searchResult = nounClient.searchIcons(searchTerm);
        if (searchResult.has("icons") && searchResult.getJSONArray("icons").length() > 0) {
            String iconUrl = searchResult.getJSONArray("icons")
                .getJSONObject(0)
                .getString("thumbnail_url");
            byte[] iconData = nounClient.downloadIcon(iconUrl);
            return ImageIO.read(new ByteArrayInputStream(iconData));
        }
        throw new IOException("No icons found for search term: " + searchTerm);
    }

    private String generateTagline(String blogName, String description) throws IOException {
        String prompt = String.format(
            "Create a concise blog tagline that describes what readers will find on the blog. " +
            "Format: Focus Area + Key Benefit/Style. " +
            "Examples: " +
            "Modern Comfort Food " +
            "Quick and Easy Dinner Recipes " +
            "Vegan Baking + Desserts, Made Delicious " +
            "For this blog about: %s " +
            "Return only the tagline without quotes.",
            description
        );

        return openAIClient.callGPT4(prompt).trim();
    }

    private String selectDomain(String blogName) throws IOException, ParseException {
        // Initialize GoDaddy client
        GoDaddyClient godaddyClient = new GoDaddyClient();
        
        // Get filtered domain suggestions (max price $50)
        List<String> domains = godaddyClient.suggestAndFilterDomains(blogName, 50.0);
        
        if (domains.isEmpty()) {
            throw new IOException("No suitable domains found for: " + blogName);
        }
        
        // Convert domains list to string for the prompt
        String domainsList = String.join("\n", domains);
        
        // Create prompt for OpenAI
        String prompt = String.format(
            "Given these available domain names for a blog about '%s':\n\n%s\n\n" +
            "Select the BEST domain name based on these criteria:\n" +
            "1. Memorability and brandability\n" +
            "2. Relevance to the blog name\n" +
            "3. Length (shorter is better)\n" +
            "4. Easy to type and spell\n\n" +
            "Respond with ONLY the chosen domain name, nothing else.",
            blogName,
            domainsList
        );

        // Get OpenAI's selection
        String selectedDomain = openAIClient.callGPT4(prompt).trim();
        System.out.println("Domain to purchase: " + selectedDomain);
        
        return selectedDomain;
    }

    public static void main(String[] args) {
        try {
            // Create test blog requests
            BlogRequest[] testRequests = {
                new BlogRequest("Y Combinator", "Y Combinator News"),
            };

            SiteBrandingService logoAndFaviconService = new SiteBrandingService(new WordPressMediaClient());
            logoAndFaviconService.generateAndUploadBranding(testRequests[0]);
        } catch (Exception e) {
            System.err.println("Error during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 