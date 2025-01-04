package com.zho.model;

public class BlogPost {
    private final int id;
    private final String title;
    private final String content;
    private final UnsplashImage coverImage;
    private final String category; 

    public BlogPost(int id, String title, String content, UnsplashImage coverImage, String category) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.coverImage = coverImage;
        this.category = category;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public UnsplashImage getCoverImage() { return coverImage; }
    public String getCategory() { return category; }
}