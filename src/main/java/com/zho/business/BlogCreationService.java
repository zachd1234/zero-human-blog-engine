package com.zho.business;

import com.zho.model.BlogRequest;
import com.zho.config.ConfigManager;
import com.zho.services.DatabaseService;
import com.zho.services.BlogSetup.BlogLayoutService;
import com.zho.services.BlogSetup.CategoryService;
import com.zho.services.BlogSetup.SiteBrandingService;
import com.zho.services.BlogSetup.PersonaService;
import com.zho.services.BlogSetup.StaticContent.StaticContentService;
import com.zho.services.DailyPosts.ContentEngineService;
import com.zho.services.GarbageCollectionService; 
import com.zho.api.wordpress.WordPressMediaClient;
import com.zho.api.UnsplashClient;
import java.io.IOException;
import org.apache.hc.core5.http.ParseException;
import java.sql.SQLException;
import com.zho.services.BlogSetup.BlogLayoutService;

public class BlogCreationService {
    // Configuration constants
    private static final int POST_COUNT = 3;
    private static final int TAG_COUNT = 6;
    
    private final DatabaseService dbService;
    private final CategoryService categoryService;
    private final SiteBrandingService logoService;
    private final StaticContentService staticContentService;
    private final PersonaService personaService;
    private final ContentEngineService contentEngineService;
    private final GarbageCollectionService garbageCollectionService;
    private final BlogLayoutService blogLayoutService;
    
    public BlogCreationService() {
        this.dbService = new DatabaseService();
        this.categoryService = new CategoryService();
        WordPressMediaClient mediaClient = new WordPressMediaClient();
        this.logoService = new SiteBrandingService(mediaClient);
        this.staticContentService = new StaticContentService();
        this.personaService = new PersonaService();
        this.contentEngineService = new ContentEngineService(); 
        this.garbageCollectionService = new GarbageCollectionService();
        this.blogLayoutService = new BlogLayoutService();
    }
    
    public void AutoBlogEngine(BlogRequest request) throws Exception {
        // 1. Set up initial configuration
        setupInitialConfig(request);
        
        // 2. Populate site
        populateSite(request);

        // 3. Start content engine
        contentEngineService.startContentEngine(request);
    }
    
    private void setupInitialConfig(BlogRequest request) throws IOException, ParseException, SQLException {
        dbService.initializeDatabase(request);
        personaService.generateAndSetupPersona(request.getTopic());
        categoryService.setupSubtopics(request);
        garbageCollectionService.deleteAllPosts();
    }
    
    private void populateSite(BlogRequest request) throws IOException, ParseException {
        // Generate and upload visual assets
        logoService.generateAndUploadBranding(request);
        blogLayoutService.setUpLayout(request);
        staticContentService.populateStaticPages(request);
    }
} 