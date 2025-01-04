package com.zho.model;

public class UnsplashImage {
    private final String id;
    private final String url;
    private final String description;

    public UnsplashImage(String id, String url, String description) {
        this.id = id;
        this.url = url;
        this.description = description != null ? description : "";
    }

    public String getId() { return id; }
    public String getUrl() { return url; }
    public String getDescription() { return description; }
} 