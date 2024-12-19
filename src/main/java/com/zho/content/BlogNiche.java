package com.zho.content;

public enum BlogNiche {
    HOME_FITNESS("Home Fitness", "A blog about home workouts and fitness tips"),
    URBAN_BEEKEEPING("Urban Beekeeping", "A blog about beekeeping in city environments");

    private final String displayName;
    private final String description;

    BlogNiche(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
} 