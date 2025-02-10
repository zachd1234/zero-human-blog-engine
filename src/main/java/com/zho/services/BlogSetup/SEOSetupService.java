package com.zho.services.BlogSetup;

import com.zho.api.wordpress.WordPressPostClient;
import com.zho.api.OpenAIClient;
import com.zho.services.BlogSetup.StaticContent.StaticContentService;
import com.zho.model.BlogRequest;
import com.zho.services.BlogSetup.StaticContent.pages.StaticPage;
import java.io.IOException;
import com.zho.services.DatabaseService;

public class SEOSetupService {
    private final WordPressPostClient wordPressPostClient;
    private final OpenAIClient openAIClient;
    private final StaticContentService staticContentService;

    public SEOSetupService() {
        this.wordPressPostClient = new WordPressPostClient();
        this.openAIClient = new OpenAIClient();
        this.staticContentService = new StaticContentService();
    }

    public void setupSEO() {
        try {
            System.out.println("\n🎯 Starting SEO Setup for Static Pages...");
            BlogRequest blogInfo = new DatabaseService().getBlogInfo();
            
            // Get all static pages
            for (StaticPage page : staticContentService.getPages()) {
                optimizePageSEO(page, blogInfo.getTopic());
            }
            
            System.out.println("✅ SEO Setup completed successfully");
            
        } catch (Exception e) {
            System.err.println("❌ Error during SEO setup: " + e.getMessage());
        }
    }

    private void optimizePageSEO(StaticPage page, String blogTopic) throws IOException {
        System.out.println("\n📄 Optimizing SEO for " + page.getPageName() + " page");
        
        // Get title (either hardcoded or from OpenAI)
        String title = page.hasHardcodedTitle() 
            ? page.getHardcodedTitle()
            : openAIClient.callOpenAI(page.getTitleTemplate()).trim();
        
        // Generate meta description using page's template
        String metaDescription = openAIClient.callOpenAI(page.getMetaDescriptionTemplate()).trim();
        
        // Update the page
        try{
            wordPressPostClient.updatePageTitleAndMeta(page.getPageId(), metaDescription, title);
            System.out.println("✅ Successfully optimized " + page.getPageName() + " page");
            System.out.println("Title: " + title);
            System.out.println("Meta Description: " + metaDescription);
        } catch (Exception e) {
            System.err.println("❌ Error during SEO setup: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            SEOSetupService seoSetupService = new SEOSetupService();
            seoSetupService.setupSEO();
        } catch (Exception e) {
            System.err.println("❌ Error during SEO setup: " + e.getMessage());
        }
    }
} 