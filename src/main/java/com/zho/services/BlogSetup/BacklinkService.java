package com.zho.services.BlogSetup;

import com.zho.api.BlogPostGeneratorAPI;
import com.zho.model.Site;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BacklinkService {
    private static final Logger logger = LoggerFactory.getLogger(BacklinkService.class);
    private final BlogPostGeneratorAPI blogPostGeneratorAPI;
    
    public BacklinkService(BlogPostGeneratorAPI blogPostGeneratorAPI) {
        this.blogPostGeneratorAPI = blogPostGeneratorAPI;
    }
    
    public void setupBacklinkCampaign() {
        Site currentSite = Site.getCurrentSite();
        
        if (currentSite != null && currentSite.isActive()) {
            try {
                JSONObject result = blogPostGeneratorAPI.setupOutreach(String.valueOf(currentSite.getSiteId()));
                logger.info("Backlink campaign setup successful: {}", result.toString());
            } catch (Exception e) {
                logger.error("Error setting up backlink campaign: {}", e.getMessage(), e);
            }
        } else {
            logger.info("Site is not in production. Skipping email outreach for now.");
        }
    }
    
    public static void main(String[] args) {
        BlogPostGeneratorAPI api = new BlogPostGeneratorAPI();
        BacklinkService service = new BacklinkService(api);
        service.setupBacklinkCampaign();
        logger.info("Backlink campaign setup process completed");
    }
}

