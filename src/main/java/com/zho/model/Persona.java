package com.zho.model;
public class Persona {
    private String name;
    private String biography;
    private String expertise;
    private String writingTone;
    private String systemPrompt;
    private String imageUrl;

    // Constructor
    public Persona(String name, String biography, String expertise, 
                  String writingTone, String systemPrompt, String imageUrl) {
        this.name = name;
        this.biography = biography;
        this.expertise = expertise;
        this.writingTone = writingTone;
        this.systemPrompt = systemPrompt;
        this.imageUrl = imageUrl;
    }

    // Getters
    public String getName() { return name; }
    public String getBiography() { return biography; }
    public String getExpertise() { return expertise; }
    public String getWritingTone() { return writingTone; }
    public String getSystemPrompt() { return systemPrompt; }
    public String getImageUrl() { return imageUrl; }
} 