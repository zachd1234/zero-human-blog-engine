package com.zho.model;

public class Topic {
    private final String title;
    private final String description;
    private final String link;

    public Topic(String title, String description, String link) {
        this.title = title;
        this.description = description;
        this.link = link;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getLink() { return link; }

    public String toString() {
        return String.format("%s|%s|%s", title, description, link);
    }
} 