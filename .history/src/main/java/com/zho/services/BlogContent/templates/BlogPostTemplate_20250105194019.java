package com.zho.services.BlogContent.templates; 

public abstract class BlogPostTemplate {
    protected String templateContent;
    protected String useCase;
    protected String templateId;
    protected SearchIntentType intentType;
    
    public BlogPostTemplate(String templateId, String useCase, String content, SearchIntentType intentType) {
        this.templateId = templateId;
        this.useCase = useCase;
        this.templateContent = content;
        this.intentType = intentType;
    }
    
    // Getters
    public String getTemplateContent() { return templateContent; }
    public String getUseCase() { return useCase; }
    public String getTemplateId() { return templateId; }
    public SearchIntentType getIntentType() { return intentType; }
} 