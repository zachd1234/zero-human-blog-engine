package com.zho;

import com.zho.content.*;
import com.zho.wordpress.*;
import com.zho.model.*;
import java.util.List;

public class App {
    public static void main(String[] args) {
        // Select your niche

        AIContentGenerator generator = new AIContentGenerator();


        // Initialize components
        WordPressUpdater wpUpdater = new WordPressUpdater();

        BlogNiche niche = BlogNiche.DISCOUNTED_CASHFLOW_VALUATIONS;

       AboutPage aboutPage = generator.generateAboutPage(niche);
        wpUpdater.updateAboutPage(aboutPage);

        List<BlogPost> posts = generator.generateBlogPosts(niche);
        for (BlogPost post : posts) {
            wpUpdater.updatePost(post);
        }
    }
}