package com.zho.services.BlogContent;

import java.io.IOException;
import org.apache.hc.core5.http.ParseException;
import java.util.List;
import com.zho.services.BlogContent.templates.SearchIntentType;
import com.zho.services.BlogContent.templates.BlogPostTemplate;

public class BlogPostEngine {
    private final TemplateManager templateManager;
    
    public BlogPostEngine() {
        this.templateManager = new TemplateManager();
    }
    
    /**
     * Generates a new blog post based on target keyword and search intent
     * @param targetKeyword The main keyword to target in the blog post
     * @param searchIntent The type of search intent (informational, commercial, etc.)
     * @return Generated blog post content
     * @throws IOException If there's an error in API communication
     * @throws ParseException If there's an error parsing responses
     */
    public String generateNewPost(String targetKeyword, String searchIntent) throws IOException, ParseException {
        SearchIntentType intentType = SearchIntentType.valueOf(searchIntent.toUpperCase());
        List<BlogPostTemplate> templates = templateManager.getTemplatesForIntent(intentType);
        
        // TODO: Select best template and generate content
        return "";
    }

    public static void main(String[] args) {
        //testing
    }
}
