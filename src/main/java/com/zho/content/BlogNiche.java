package com.zho.content;

public enum BlogNiche {
    HOME_FITNESS("Home Fitness", "A blog about home workouts and fitness tips"),
    HOMOLOGY_GROUPS("Homology Groups", "A blog about homology groups and their applications"),
    LONG_HAIRED_CATS("Long Haired Cats", "A blog about long haired cats and their care"),
    DISCOUNTED_CASHFLOW_VALUATIONS("Discounted Cash Flow Valuation", "A blog about how an appraiser values commerical real estate using discounted cash flow methodologies" );

    private final String displayName;
    private final String description;

    BlogNiche(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
} 