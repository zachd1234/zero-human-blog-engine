package com.zho;

import com.zho.content.*;
import com.zho.wordpress.*;
import com.zho.model.*;
import java.util.List;
import java.awt.image.BufferedImage;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;

public class App {
    public static void main(String[] args) {
        try {
            // Initialize components
            AIContentGenerator generator = new AIContentGenerator();
            WordPressUpdater wpUpdater = new WordPressUpdater();
            LogoGenerator logoGenerator = new LogoGenerator();
            
            // Select your niche
            BlogNiche niche = BlogNiche.LONG_HAIRED_CATS;
            
            // Update mission statement and headings based on niche
            System.out.println("Updating mission statement and headings...");
            wpUpdater.updateMissionParagraph(niche);
            wpUpdater.updateHeadingAndSubheading(niche);
            wpUpdater.updateAllContent(niche); //for About Us Page. 
            //TODO: Consolidate these methods 
            
            // 1. Generate and update logo and favicon
            System.out.println("Generating and updating logo and favicon...");
            
            // Get icon first
            IconFetcher iconFetcher = new IconFetcher();
            BufferedImage icon = iconFetcher.fetchRelatedIcon(niche.getDisplayName());
            
            // Generate logo (which combines icon with text)
            BufferedImage logo = logoGenerator.generateLogo(niche.getDisplayName());
            
            // Update both
            wpUpdater.updateSiteLogo(logo);
            wpUpdater.updateSiteIcon(icon);  // Use the icon alone for favicon
            
            // 2. Generate and update about page
            System.out.println("Generating and updating about page...");
            AboutPage aboutPage = generator.generateAboutPage(niche);
            wpUpdater.updateAboutPage(aboutPage);
            
            // Initialize topic configuration
            TopicConfig topicConfig = new TopicConfig();
            System.out.println("Updating topic sections...");
            topicConfig.setupTopics(niche);
                        
            
            // 3. Generate and update blog posts
            System.out.println("Generating and updating blog posts...");
            List<BlogPost> posts = generator.generateBlogPosts(niche);
            for (BlogPost post : posts) {
                wpUpdater.updatePost(post);
            }
            
            wpUpdater.updateAllImages(niche);
            
            System.out.println("Blog setup completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Error setting up blog: " + e.getMessage());
            e.printStackTrace();
        }
    }
}