package com.zho.services.BlogSetup.StaticContent.pages;

import java.io.IOException;
import org.apache.hc.core5.http.ParseException;
import com.zho.model.BlogRequest;
import com.zho.model.Image;
import java.util.List;

public interface StaticPage {
    String getPageName();
    void updateStaticContent(BlogRequest request, List<Image> images) throws IOException, ParseException;
    int getRequiredImageCount();
    int getPageId();
    
    // New methods for SEO optimization
    default String getTitleTemplate() {
        return "Create an SEO-optimized title for the %s page of a blog about %s. " +
               "Keep it under 60 characters and make it compelling.";
    }
    
    default String getMetaDescriptionTemplate() {
        return "Write a meta description for the %s page of a blog about %s. " +
               "Include the main value proposition and a call to action. " +
               "Keep it between 150-160 characters for optimal SEO.";
    }
    
    // New methods that indicate if the response is hardcoded
    default boolean hasHardcodedTitle() {
        return false;
    }
    
    default String getHardcodedTitle() {
        return null;
    }
} 