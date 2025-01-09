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
            new HowToTemplate(),
            new GuideTemplate(),
            new ExplainerTemplate()
        ));
        
        // Add other templates...
    }
    
    public List<BlogPostTemplate> getTemplatesForIntent(SearchIntentType intent) {
        return templatesByIntent.get(intent);
    }
} 