package com.zho.business;

import com.zho.model.BlogRequest;
import com.zho.config.ConfigManager;
import com.zho.services.DatabaseService;
import com.zho.services.CategoryService;
import com.zho.services.LogoAndFaviconService;
import com.zho.services.PersonaService;
import com.zho.services.BlogPostService;
import com.zho.services.StaticContent.StaticContentService;
import com.zho.api.wordpress.WordPressMediaClient;
import com.zho.api.UnsplashClient;
import java.io.IOException;
import org.apache.hc.core5.http.ParseException;
import java.sql.SQLException;

public class BlogCreationService {
    // Configuration constants
    private static final int POST_COUNT = 3;
    private static final int TAG_COUNT = 6;
    
    private final DatabaseService dbService;
    private final CategoryService categoryService;
    private final LogoAndFaviconService logoService;
    private final BlogPostService blogPostService;
    private final StaticContentService staticContentService;
    private final PersonaService personaService;
    
    public BlogCreationService() {
        this.dbService = new DatabaseService();
        this.categoryService = new CategoryService();
        WordPressMediaClient mediaClient = new WordPressMediaClient();
        this.logoService = new LogoAndFaviconService(mediaClient);
        this.blogPostService = new BlogPostService();
        this.staticContentService = new StaticContentService();
        this.personaService = new PersonaService();
    }
    
    public void AutoBlogEngine(BlogRequest request) throws Exception {
        // 1. Set up initial configuration
        setupInitialConfig(request);
        
        // 2. Populate site
        populateSite(request);
    }
    
    private void setupInitialConfig(BlogRequest request) throws IOException, ParseException, SQLException {
        dbService.initializeDatabase();
        personaService.generateAndSetupPersona(request.getTopic());
        categoryService.setupSubtopics(request);
    }
    
    private void populateSite(BlogRequest request) throws IOException, ParseException {
        // Generate and upload visual assets
        logoService.generateAndUploadBranding(request);
        
        // Create content
        blogPostService.createAndPublishPosts(request, POST_COUNT, new int[] {23, 21, 1});
        staticContentService.populateStaticPages(request);
    }
} 