package com.zho.model;

public class AboutPage {
    private final int id;
    private final String content;

    public AboutPage(int id, String content) {
        this.id = id;
        this.content = content;
    }

    public int getId() { return id; }
    public String getContent() { return content; }
} 