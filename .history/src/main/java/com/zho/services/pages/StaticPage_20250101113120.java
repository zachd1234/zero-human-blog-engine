package com.zho.services.pages;

import java.io.IOException;
import org.apache.hc.core5.http.ParseException;
import com.zho.model.BlogRequest;

public interface StaticPage {
    void updateStaticContent(BlogRequest request) throws IOException, ParseException;
    String getPageName();
    int getPageId();
} 