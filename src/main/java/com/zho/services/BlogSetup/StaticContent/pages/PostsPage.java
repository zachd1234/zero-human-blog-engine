package com.zho.services.BlogSetup.StaticContent.pages;

import java.io.IOException;
import org.apache.hc.core5.http.ParseException;
import com.zho.model.BlogRequest;
import com.zho.model.Image;
import com.zho.api.wordpress.WordPressBlockClient;
import java.util.List;
import java.sql.SQLException;
import com.zho.services.DatabaseService;

public class PostsPage implements StaticPage {
    private final int pageId = 610;

    @Override
    public String getPageName() {
        return "Blog";
    }

    @Override
    public void updateStaticContent(BlogRequest request, List<Image> images) {
        // No dynamic content needed
    }

    @Override
    public int getRequiredImageCount() {
        return 0;
    }

    @Override
    public int getPageId() {
        return pageId;
    }

    @Override
    public boolean hasHardcodedTitle() {
        return true;
    }

    @Override
    public String getHardcodedTitle() {
        try {
            String siteName = new WordPressBlockClient().getSiteTitle();
            return siteName + " Blog";
        } catch (IOException | ParseException e) {
            return "Blog";  // Fallback
        }
    }

    @Override
    public String getMetaDescriptionTemplate() {
        try {
            BlogRequest blogInfo = new DatabaseService().getBlogInfo();
            String siteName = new WordPressBlockClient().getSiteTitle();
            
            return String.format(
                "Explore the latest %s articles, guides, and expert advice from %s. " +
                "Stay updated with our comprehensive resources and insights.",
                blogInfo.getTopic(),
                siteName
            );
        } catch (IOException | ParseException | SQLException e) {
            return "Explore our latest articles, guides, and expert advice. " +
                   "Stay updated with our comprehensive resources and insights.";
        }
    }
} 