package com.zho;

import com.zho.content.*;
import com.zho.wordpress.*;
import com.zho.model.*;
import java.util.List;
import java.awt.image.BufferedImage;

public class App {
    public static void main(String[] args) {
        try {
            // Initialize components
            AIContentGenerator generator = new AIContentGenerator();
            WordPressUpdater wpUpdater = new WordPressUpdater();
            LogoGenerator logoGenerator = new LogoGenerator();
            
            // Select your niche
            BlogNiche niche = BlogNiche.LONG_HAIRED_CATS;
            
            // 1. Generate and update logo
            System.out.println("Generating and updating logo...");
            BufferedImage logo = logoGenerator.generateLogo(niche.getDisplayName());
            wpUpdater.updateSiteLogo(logo);
            
            // 2. Generate and update about page
            System.out.println("Generating and updating about page...");
            AboutPage aboutPage = generator.generateAboutPage(niche);
            wpUpdater.updateAboutPage(aboutPage);
            
            // 3. Generate and update blog posts
            System.out.println("Generating and updating blog posts...");
            List<BlogPost> posts = generator.generateBlogPosts(niche);
            for (BlogPost post : posts) {
                wpUpdater.updatePost(post);
            }
            
            System.out.println("Blog setup completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Error setting up blog: " + e.getMessage());
            e.printStackTrace();
        }
    }
}