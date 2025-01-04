package com.zho.services.pages;

import java.io.IOException;
import org.apache.hc.core5.http.ParseException;
import com.zho.model.BlogRequest;
import java.util.List;

public interface StaticPage {
    String getPageName();
    void updateStaticContent(BlogRequest request, List<UnsplashImage> images) throws IOException, ParseException;
    int getRequiredImageCount();
} 