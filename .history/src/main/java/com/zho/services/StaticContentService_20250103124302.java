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

    private List<UnsplashImage> fetchSiteImages(BlogRequest request, int totalImagesNeeded) throws IOException, ParseException {
        String searchQuery = request.getTopic() + " " + request.getDescription();
        System.out.println("Fetching " + totalImagesNeeded + " images for query: " + searchQuery);
        
        List<UnsplashImage> images = unsplashClient.searchImages(searchQuery, totalImagesNeeded);
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