package com.zho.services.BlogSetup.StaticContent;

import java.util.*;
import com.zho.model.BlogRequest;
import com.zho.model.Image;
import com.zho.api.wordpress.WordPressBlockClient;
import com.zho.api.GetImgAIClient;
import com.zho.api.OpenAIClient;
import com.zho.api.UnsplashClient;

import java.io.IOException;
import org.apache.hc.core5.http.ParseException;
import com.zho.api.wordpress.WordPressMediaClient;
import com.zho.services.DatabaseService;
import com.zho.services.BlogSetup.StaticContent.pages.AboutPage;
import com.zho.services.BlogSetup.StaticContent.pages.EditorialPage;
import com.zho.services.BlogSetup.StaticContent.pages.HomePage;
import com.zho.services.BlogSetup.StaticContent.pages.StaticPage;
import java.util.stream.Collectors;

public class StaticContentService {
    private final List<StaticPage> pages;
    private final WordPressBlockClient blockClient;
    private final OpenAIClient openAIClient;
    private final UnsplashClient unsplashClient;
    private final WordPressMediaClient mediaClient;
    private final DatabaseService databaseService;

    public StaticContentService() {
        this.blockClient = new WordPressBlockClient();
        this.openAIClient = new OpenAIClient();
        this.unsplashClient = new UnsplashClient();
        this.mediaClient = new WordPressMediaClient();
        this.databaseService = new DatabaseService();
        
        // Initialize pages
        this.pages = Arrays.asList(
            new HomePage(blockClient, openAIClient, mediaClient, databaseService),
            new AboutPage(blockClient, openAIClient, mediaClient, databaseService),
            new EditorialPage(blockClient));
    }

    public void populateStaticPages(BlogRequest request) throws IOException, ParseException {
       
        //Step 1: Site-Wide Image Generation:
        
        // Calculate total images needed
        int totalImagesNeeded = pages.stream()
            .mapToInt(StaticPage::getRequiredImageCount)
            .sum();
        
        // Fetch all required images at once
        System.out.println("Fetching " + totalImagesNeeded + " images for static pages...");
        List<Image> siteImages = fetchSiteImages(request, totalImagesNeeded);
        
        // Step 2: Call each page's updateStaticContent and distribute images to each one 
        int imageIndex = 0;
        for (StaticPage page : pages) {
            int imagesNeeded = page.getRequiredImageCount();
            if (imageIndex + imagesNeeded <= siteImages.size()) {
                List<Image> pageImages = new ArrayList<>(
                    siteImages.subList(imageIndex, imageIndex + imagesNeeded)
                );
                
                try {
                    System.out.println("Updating " + page.getPageName() + " page with " + pageImages.size() + " images...");
                    page.updateStaticContent(request, pageImages);
                    System.out.println(page.getPageName() + " page updated successfully");
                    
                    imageIndex += imagesNeeded;
                } catch (Exception e) {
                    System.err.println("Error updating " + page.getPageName() + " page: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.err.println("Warning: Not enough images for " + page.getPageName() + " page. Needed " + imagesNeeded + " but only had " + (siteImages.size() - imageIndex) + " remaining.");
                List<Image> pageImages = new ArrayList<>(
                    siteImages.subList(imageIndex, siteImages.size())
                );
                try {
                    page.updateStaticContent(request, pageImages);
                } catch (Exception e) {
                    System.err.println("Error updating " + page.getPageName() + " page: " + e.getMessage());
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    private String generateImageSearchTerm(BlogRequest request) throws IOException {
        String prompt = String.format(
            "Generate a broad, thematic search term for finding stock photos that would work well throughout a blog about '%s'.\n" +
            "Requirements:\n" +
            "1. Term should capture the overall theme/industry of the blog\n" +
            "2. Focus on broad, professional imagery that works for headers and backgrounds\n" +
            "3. For technical/specific topics, translate to broader visual themes\n" +
            "4. Term should return plenty of high-quality, professional photos\n\n" +
            "Examples:\n" +
            "- 'Tennis Coaching Blog' → 'tennis'\n" +
            "- 'Discounted Cash Flow Analysis' → 'modern finance office'\n" +
            "- 'Python Programming Tutorial' → 'coding'\n" +
            "- 'Organic Chemistry Notes' → 'science laboratory'\n" +
            "- 'Day Trading Strategies' → 'financial district'\n" +
            "- 'Mediterranean Recipe Blog' → 'mediterranean food'\n" +
            "- 'Machine Learning Guide' → 'machine learning'\n" +
            "- 'Guitar Lessons' → 'guitar music'\n\n" +
            "Respond with ONLY the search term, nothing else.",
            request.getTopic()
        );

        String searchTerm = openAIClient.callOpenAI(prompt, 0.7).toLowerCase().trim();
        System.out.println("Generated broad theme search term: " + searchTerm);
        return searchTerm;
    }

    private List<Image> fetchSiteImages(BlogRequest request, int totalImagesNeeded) throws IOException, ParseException {
        String searchTerm = generateImageSearchTerm(request);
        System.out.println("Fetching " + totalImagesNeeded + " images for theme: " + searchTerm);
        
        // Step 1: Fetch initial images from Unsplash
        List<Image> images = unsplashClient.searchImages(searchTerm, totalImagesNeeded);
        System.out.println("Successfully fetched " + images.size() + " images");
        
        // Step 2: Validate images with ChatGPT
        List<Image> validImages = new ArrayList<>();
        int aiImagesNeeded = 0;
                
        for (Image image : images) {
            String validationPrompt = String.format(
                "Is this image description directly relevant to a blog about '%s' with description '%s'? " +
                "Image description: '%s'. " +
                "Answer with only 'yes' if it is directly related and highly relevant. Otherwise, answer with 'no'.",
                request.getTopic(),
                request.getDescription(),
                image.getDescription()
            );

            
            String response = openAIClient.callOpenAI(validationPrompt, 0.7).trim().toLowerCase();
            if (response.equals("yes")) {
                validImages.add(image);
            } else {
                aiImagesNeeded++;
            }
        }
        
        // Step 3: Generate AI image prompts if needed
        if (aiImagesNeeded > 0) {
            String promptGenerationPrompt = String.format(
                "Generate %d unique, detailed image prompts for a blog about '%s' with description '%s'. " +
                "Each prompt should be specific and visually descriptive. " +
                "Return as a numbered list, one prompt per line.",
                aiImagesNeeded,
                request.getTopic(),
                request.getDescription()
            );

            // Log the prompt being sent to OpenAI
            System.out.println("Prompt for OpenAI: " + promptGenerationPrompt);

            String promptsResponse = openAIClient.callOpenAI(promptGenerationPrompt, 0.7);
            
            // Log the response from OpenAI
            System.out.println("Response from OpenAI: " + promptsResponse);

            // Check if the response is empty or null
            if (promptsResponse == null || promptsResponse.trim().isEmpty()) {
                System.err.println("Received empty response from OpenAI. Using fallback prompt.");

                // Fallback prompt
                promptsResponse = String.format(
                    "Generate a generic image prompt for a blog about '%s' with description '%s'.",
                    request.getTopic(),
                    request.getDescription()
                );

                // Log the fallback prompt
                System.out.println("Fallback Prompt: " + promptsResponse);

                // Call OpenAI again with the fallback prompt
                promptsResponse = openAIClient.callOpenAI(promptsResponse, 0.7);
            }

            // Proceed to parse the prompts
            List<String> imagePrompts = Arrays.stream(promptsResponse.split("\n"))
                .map(prompt -> prompt.replaceAll("^\\d+\\.\\s*", "")) // Remove numbered list format
                .map(prompt -> prompt.replaceAll("^\\*\\*.*?\\*\\*:\\s*", "")) // Remove bold headers
                .map(String::trim)
                .filter(prompt -> !prompt.isEmpty()) // Filter out empty strings
                .collect(Collectors.toList());

            // Log the parsed image prompts
            System.out.println("Parsed Image Prompts: " + imagePrompts);

            // Step 4: Generate images for each prompt
            for (int i = 0; i < aiImagesNeeded && i < imagePrompts.size(); i++) {
                try {
                    Image generatedImage = generateImageFromPrompt(imagePrompts.get(i));
                    validImages.add(generatedImage);
                    System.out.println("Generated image from prompt: " + imagePrompts.get(i));
                } catch (Exception e) {
                    System.err.println("Failed to generate image for prompt: " + imagePrompts.get(i));
                    e.printStackTrace();
                }
            }
        }
        
        System.out.println("Final image count: " + validImages.size() + 
                          " (Original: " + images.size() + 
                          ", AI-generated: " + aiImagesNeeded + ")");
        
        return validImages;
    }

    // Helper method to generate an image from a prompt
    private Image generateImageFromPrompt(String prompt) throws IOException, ParseException {
        GetImgAIClient getImgAIClient = new GetImgAIClient();
        
        // Get the temporary URL from GetImg AI
        String tempImageUrl = getImgAIClient.generateImage(prompt);
        
        // Download and upload to WordPress media library
        String permanentUrl = mediaClient.uploadImageFromUrl(tempImageUrl);
        
        return new Image("", permanentUrl, prompt);  // Use the permanent WordPress URL
    }

    public static void main(String[] args) {
        try {
            // Create test blog requests
            BlogRequest testRequest = new BlogRequest("rucking", "Rooted in military training, Rucking combines strength and cardio into one powerful workout. Discover tips, gear reviews, and training plans to help you build endurance, burn calories, and enjoy the outdoors—all with just a weighted backpack");
            // Initialize service
            StaticContentService service = new StaticContentService();
            
            // Log the start of the process
            System.out.println("Starting static page population...");
            
            // Calculate total images needed (for logging)
            int totalImages = service.pages.stream()
                .mapToInt(StaticPage::getRequiredImageCount)
                .sum();
            System.out.println("Total images needed across all pages: " + totalImages);
            
            // Populate pages
            service.populateStaticPages(testRequest);
            
            System.out.println("✓ Success: Static pages populated for " + testRequest.getTopic());
            
            // Add a small delay between tests
            Thread.sleep(2000);
            
        } catch (Exception e) {
            System.err.println("Error during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 