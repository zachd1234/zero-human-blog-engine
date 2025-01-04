package com.zho.business;

import com.zho.model.BlogRequest;
import com.zho.config.ConfigManager;
import com.zho.services.DatabaseService;
import com.zho.services.CategoryService;
import com.zho.services.ImagePopulatorService;
import com.zho.services.LogoAndFaviconService;
import com.zho.services.BlogPostService;
import com.zho.services.StaticContentService;
import java.io.IOException;
import org.apache.hc.core5.http.ParseException;
import java.sql.SQLException;

public class BlogCreationService {
    // Configuration constants
    private static final int POST_COUNT = 3;
    private static final int TAG_COUNT = 6;
    
    private final DatabaseService dbService;
    private final CategoryService categoryService;
    private final ImagePopulatorService imagePopulator;
    private final LogoAndFaviconService logoService;
    private final BlogPostService blogPostService;
    private final StaticContentService staticContentService;
    
    public BlogCreationService() {
        this.dbService = new DatabaseService();
        this.categoryService = new CategoryService();
        this.imagePopulator = new ImagePopulatorService(new UnsplashClient(), new WordPressMediaClient());
        this.logoService = new LogoAndFaviconService();
        this.blogPostService = new BlogPostService();
        this.staticContentService = new StaticContentService();
    }
    
    public void AutoBlogEngine(BlogRequest request) throws Exception {
        // 1. Set up initial configuration
        setupInitialConfig(request);
        
        // 2. Populate site
        populateSite(request);
    }
    
    private void setupInitialConfig(BlogRequest request) throws IOException, ParseException, SQLException {
        dbService.initializeDatabase();
        categoryService.setupTopics(request);
    }
    
    private void populateSite(BlogRequest request) throws IOException, ParseException {
        // Generate and upload visual assets
        logoService.generateAndUploadBranding(request);
        imagePopulator.populateImages(request);
        
        // Create content
        blogPostService.createAndPublishPosts(request, POST_COUNT, new int[] {23, 21, 1});
        staticContentService.populateStaticPages(request);
    }
} 