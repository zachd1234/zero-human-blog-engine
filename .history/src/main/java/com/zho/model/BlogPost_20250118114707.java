package com.zho.model;

public class BlogPost {
    private final int id;
    private final String title;
    private final String content;
    private final Image coverImage;
    private final String category; 
    private final int categoryId;
    private final String slug;
    private final String metaDescription;

    public BlogPost(int id, String title, String content, Image coverImage, String category) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.coverImage = coverImage;
        this.category = category;
        this.categoryId = -1;
        this.slug = "";
        this.metaDescription = "";
    }

    public BlogPost(int id, String title, String content, Image coverImage, String category, int categoryId) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.coverImage = coverImage;
        this.category = category;
        this.categoryId = categoryId;
        this.slug = "";
        this.metaDescription = "";

    }

    public BlogPost(int id, String title, String content, Image coverImage, String category, int categoryId, String slug, String metaDescription) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.coverImage = coverImage;
        this.category = category;
        this.categoryId = categoryId;
        this.slug = slug;
        this.metaDescription = metaDescription; 
    }



    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public Image getCoverImage() { return coverImage; }
    public String getCategory() { return category; }
    public int getCategoryId() { return categoryId; }
    public String getSlug() { return slug;}
    public String getMetaDescription() { return metaDescription;}
