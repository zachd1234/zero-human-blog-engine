package com.zho.api.wordpress;

public class WordPressClient {
    private final WordPressCategoryClient categoryClient;
    private final WordPressMediaClient mediaClient;
    private final WordPressBlockClient blockClient;
    private final WordPressPostClient postClient;

    public WordPressClient() {
        this.categoryClient = new WordPressCategoryClient();
        this.mediaClient = new WordPressMediaClient();
        this.blockClient = new WordPressBlockClient();
        this.postClient = new WordPressPostClient();
    }

    public WordPressCategoryClient categories() {
        return categoryClient;
    }

    public WordPressMediaClient media() {
        return mediaClient;
    }

    public WordPressBlockClient blocks() {
        return blockClient;
    }

    public WordPressPostClient posts() {
        return postClient;
    }
}