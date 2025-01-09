package com.zho.services.BlogContent;

import com.zho.services.BlogContent.templates.BlogPostTemplate;
import com.zho.services.BlogContent.templates.SearchIntentType;
import com.zho.services.BlogContent.templates.informational.HowToTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class TemplateManager {
    private final Map<SearchIntentType, List<BlogPostTemplate>> templatesByIntent;
    
    public TemplateManager() {
        templatesByIntent = new EnumMap<>(SearchIntentType.class);
        initializeTemplates();
    }
    
    private void initializeTemplates() {
        // Initialize each intent type with an empty list
        for (SearchIntentType intent : SearchIntentType.values()) {
            templatesByIntent.put(intent, new ArrayList<>());
        }
        
        // Add templates to appropriate lists
        templatesByIntent.get(SearchIntentType.INFORMATIONAL).addAll(Arrays.asList(
            new HowToTemplate()
        ));
        
    }
    
    public List<BlogPostTemplate> getTemplatesForIntent(SearchIntentType intent) {
        return templatesByIntent.get(intent);
    }
} 