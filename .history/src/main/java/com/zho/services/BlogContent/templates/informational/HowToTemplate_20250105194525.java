package com.zho.services.BlogContent.templates.informational;

import com.zho.services.BlogContent.templates.BlogPostTemplate;
import com.zho.services.BlogContent.templates.SearchIntentType;

public class HowToTemplate extends BlogPostTemplate {
    public HowToTemplate() {
        super(
            "HOW_TO_001",
            "Step-by-step tutorial format",
            "# How to {keyword}\n\n" +
            "Are you looking to master {keyword}? In this comprehensive guide...",
            SearchIntentType.INFORMATIONAL
        );
    }
} 