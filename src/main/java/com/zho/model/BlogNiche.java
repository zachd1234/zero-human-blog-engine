package com.zho.model;

public enum BlogNiche {
    HOME_FITNESS("Home Fitness", "A blog about home workouts and fitness tips"),
    HOMOLOGY_GROUPS("Homology Groups", "A blog about homology groups and their applications"),
    LONG_HAIRED_CATS("Long Haired Cats", "A blog about long haired cats and their care"),
    DISCOUNTED_CASHFLOW_VALUATIONS("Discounted Cash Flow Valuation", "A blog about how an appraiser values commerical real estate using discounted cash flow methodologies"),
    JAPAN_TRAVEL("Japan Travel", "A blog about travel in Japan"),
    HOCKEY_BLOG("Hockey Blog", "A blog about hockey");

    private final String topic;
    private final String description;

    BlogNiche(String topic, String description) {
        this.topic = topic;
        this.description = description;
    }

    public String getTopic() { return topic; }
    public String getDescription() { return description; }
} 