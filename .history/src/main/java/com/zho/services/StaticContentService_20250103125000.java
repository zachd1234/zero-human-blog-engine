package com.zho.services;

import java.util.*;
import com.zho.model.BlogRequest;
import com.zho.model.UnsplashImage;
import com.zho.api.wordpress.WordPressBlockClient;
import com.zho.api.OpenAIClient;
import com.zho.api.UnsplashClient;
import com.zho.services.pages.StaticPage;
import com.zho.services.pages.HomePage;
import com.zho.services.pages.AboutPage;
import java.io.IOException;
import org.apache.hc.core5.http.ParseException;

public class StaticContentService {
    private final List<StaticPage> pages;
    private final WordPressBlockClient blockClient;
    private final OpenAIClient openAIClient;
    private final UnsplashClient unsplashClient;

    public StaticContentService() {
        this.blockClient = new WordPressBlockClient();
        this.openAIClient = new OpenAIClient();
        this.unsplashClient = new UnsplashClient();
        
        // Initialize pages
        this.pages = Arrays.asList(
            new HomePage(blockClient, openAIClient),
            new AboutPage(blockClient, openAIClient)
        );
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
            "- 'Python Programming Tutorial' → 'modern workspace technology'\n" +
            "- 'Organic Chemistry Notes' → 'science laboratory'\n" +
            "- 'Day Trading Strategies' → 'financial district'\n" +
            "- 'Mediterranean Recipe Blog' → 'mediterranean food'\n" +
            "- 'Machine Learning Guide' → 'data science workspace'\n" +
            "- 'Guitar Lessons' → 'guitar music'\n\n" +
            "Respond with ONLY the search term, nothing else.",
            request.getTopic()
        );

        String searchTerm = openAIClient.callOpenAI(prompt, 0.7).toLowerCase().trim();
        System.out.println("Generated broad theme search term: " + searchTerm);
        return searchTerm;
    }

    private List<UnsplashImage> fetchSiteImages(BlogRequest request, int totalImagesNeeded) throws IOException, ParseException {
        String searchTerm = generateImageSearchTerm(request);
        System.out.println("Fetching " + totalImagesNeeded + " images for theme: " + searchTerm);
        
        List<UnsplashImage> images = unsplashClient.searchImages(searchTerm, totalImagesNeeded);
        System.out.println("Successfully fetched " + images.size() + " images");
        
        return images;
    }

    public void populateStaticPages(BlogRequest request) throws IOException, ParseException {
        // Calculate total images needed
        int totalImagesNeeded = pages.stream()
            .mapToInt(StaticPage::getRequiredImageCount)
            .sum();
        
        // Fetch all required images at once
        System.out.println("Fetching " + totalImagesNeeded + " images for static pages...");
        List<UnsplashImage> siteImages = fetchSiteImages(request, totalImagesNeeded);
        
        // Distribute images to pages
        int imageIndex = 0;
        for (StaticPage page : pages) {
            int imagesNeeded = page.getRequiredImageCount();
            List<UnsplashImage> pageImages = new ArrayList<>(
                siteImages.subList(imageIndex, imageIndex + imagesNeeded)
            );
            
            try {
                System.out.println("Updating " + page.getPageName() + " page with " + imagesNeeded + " images...");
                page.updateStaticContent(request, pageImages);
                System.out.println(page.getPageName() + " page updated successfully");
                
                imageIndex += imagesNeeded;
            } catch (Exception e) {
                System.err.println("Error updating " + page.getPageName() + " page: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
} 