package com.zho.services.BlogSetup.StaticContent.pages;

import java.io.IOException;
import org.apache.hc.core5.http.ParseException;
import com.zho.model.BlogRequest;
import com.zho.model.UnsplashImage;
import java.util.List;

public interface StaticPage {
    String getPageName();
    void updateStaticContent(BlogRequest request, List<UnsplashImage> images) throws IOException, ParseException;
    int getRequiredImageCount();
    int getPageId();
} 