package com.zho.model;

public class BlogPost {
    private final int id;
    private final String title;
    private final String content;
    private final UnsplashImage coverImage;

    public BlogPost(int id, String title, String content, UnsplashImage coverImage) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.coverImage = coverImage;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public UnsplashImage getCoverImage() { return coverImage; }
}