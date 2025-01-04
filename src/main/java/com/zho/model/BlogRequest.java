package com.zho.model;

public class BlogRequest {
    private final String topic;
    private final String description;
    private final String targetAudience;  // Optional fields
    private final String tone;            // can be added as needed

    // Constructor
    public BlogRequest(String topic, String description) {
        this.topic = topic;
        this.description = description;
        this.targetAudience = null;
        this.tone = null;
    }

    // Getters (no setters as it's immutable)
    public String getTopic() { return topic; }
    public String getDescription() { return description; }
    public String getTargetAudience() { return targetAudience; }
    public String getTone() { return tone; }
}