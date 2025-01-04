package com.zho.services;

import java.util.Arrays;
import java.util.List;
import com.zho.model.BlogRequest;
import com.zho.api.wordpress.WordPressBlockClient;
import com.zho.api.OpenAIClient;
import com.zho.services.pages.StaticPage;
import com.zho.services.pages.HomePage;
import com.zho.services.pages.AboutPage;

public class StaticContentService {
    private final List<StaticPage> pages; //needs to implement static page interface 
    private final WordPressBlockClient blockClient;
    private final OpenAIClient openAIClient;

    public StaticContentService() {
        this.blockClient = new WordPressBlockClient();
        this.openAIClient = new OpenAIClient();
        
        // Initialize pages
        this.pages = Arrays.asList(
            new HomePage(blockClient, openAIClient),
            new AboutPage(blockClient, openAIClient)
        );
    }

    public void populateStaticPages(BlogRequest request) {
        for (StaticPage page : pages) {
            try {
                System.out.println("Updating " + page.getPageName() + " page...");
                page.updateStaticContent(request);
                System.out.println(page.getPageName() + " page updated successfully");
            } catch (Exception e) {
                System.err.println("Error updating " + page.getPageName() + " page: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
} 