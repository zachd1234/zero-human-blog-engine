package com.zho.services.pages;

import java.io.IOException;
import org.apache.hc.core5.http.ParseException;
import com.zho.api.OpenAIClient;
import com.zho.api.wordpress.WordPressBlockClient;
import com.zho.model.BlogRequest;

public class AboutPage implements StaticPage {
    private final WordPressBlockClient blockClient;
    private final OpenAIClient openAIClient;
    private final int pageId = 318;

    public AboutPage(WordPressBlockClient blockClient, OpenAIClient openAIClient) {
        this.blockClient = blockClient;
        this.openAIClient = openAIClient;
    }

    @Override
    public void updateStaticContent(BlogRequest request) throws IOException, ParseException {
        //Update the about page 
    }
    
    @Override
    public String getPageName() {
        return "About";
    }

    @Override
    public int getPageId() {
        return pageId; 
    }
} 