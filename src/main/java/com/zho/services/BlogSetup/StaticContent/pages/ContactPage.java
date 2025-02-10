package com.zho.services.BlogSetup.StaticContent.pages;

import java.io.IOException;
import org.apache.hc.core5.http.ParseException;
import com.zho.model.BlogRequest;
import com.zho.model.Image;
import java.util.List;
import com.zho.api.wordpress.WordPressBlockClient;

public class ContactPage implements StaticPage {
    private final int pageId = 290; // Update with actual page ID

    @Override
    public String getPageName() {
        return "Contact";
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
        return "Contact Us";
    }

    @Override
    public String getMetaDescriptionTemplate() {
        try {
            String siteName = new WordPressBlockClient().getSiteTitle();
            return String.format(
                "Get in touch with %s. We're here to help answer your questions and hear your feedback.",
                siteName
            );
        } catch (IOException | ParseException e) {
            return "Get in touch with us. We're here to help answer your questions and hear your feedback.";
        }
    }
} 