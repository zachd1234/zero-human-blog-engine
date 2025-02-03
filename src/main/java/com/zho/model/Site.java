package com.zho.model;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.zho.services.DatabaseService;
import java.util.Properties;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

public enum Site {

    //toggle blog status here. //MAKE SURE TO ADD a row to BLOG_STATUS. 
    MAIN("https://ruckquest.com/wp-json/wp/v2/", 1, true),
    TEST("https://mbt.dsc.mybluehost.me/wp-json/wp/v2/", 2, false),
    SITE3("https://mbt.dsc.mybluehost.me/website_ef63468e/wp-json/wp/v2/", 3, false);
    //Site 3 (Url, 3, false)

    private static final Map<String, Site> sitesByName = new HashMap<>();
    private static final Map<Integer, Site> sitesById = new HashMap<>();
    private static Site CURRENT_SITE;

    static {
        // Initialize the lookup maps
        for (Site site : Site.values()) {
            sitesByName.put(site.name(), site);
            sitesById.put(site.getSiteId(), site);
        }
        // Initialize CURRENT_SITE
        CURRENT_SITE = getCurrentSite();
    }

    private final String url;
    private final int siteId;
    private boolean isActive;

    Site(String url, int siteId, boolean isActive) {
        this.url = url;
        this.siteId = siteId;
        this.isActive = isActive;
    }

    public String getUrl() {
        return url;
    }

    public boolean isActive() {
        return isActive;
    }

    public int getSiteId() {
        return siteId;
    }

    public static Site getCurrentSite() {
        //refresh
        int siteId = DatabaseService.loadCurrentSiteFromDatabase();
        return getSitebyId(siteId);
    }

    public static Site getSitebyId(int siteid) {
        Site site = sitesById.get(siteid);
        return site != null ? site : null; 
    }

    public static Site getSiteByName(String name) {
        Site site = sitesByName.get(name);
        return site != null ? site : null;
    }

    public static void SwitchSite(Site newSite) {
        CURRENT_SITE = newSite;
        DatabaseService.updateCurrentSiteInDatabase(newSite.getSiteId());
        // Only update properties file if not running in Lambda
        if (System.getenv("AWS_LAMBDA_FUNCTION_NAME") == null) {
            updatePropertiesFile();
        }
    }

    public static void printAllSites() {
        List<Site> allSites = Site.getAllSites();
        for (Site site : allSites) {
            System.out.println("Site Name: " + site.name());
            System.out.println("URL: " + site.getUrl());
            System.out.println("ID: " + site.getSiteId());
            System.out.println("----------------------");
        }
    }

    public static void updatePropertiesFile() {
        // Skip if running in Lambda
        if (System.getenv("AWS_LAMBDA_FUNCTION_NAME") != null) {
            return;
        }
        
        Properties props = new Properties();
        try {
            props.load(Site.class.getResourceAsStream("/application.properties"));
            props.setProperty("wordpress.base.url", CURRENT_SITE.getUrl());
            props.store(new FileOutputStream("src/main/resources/application.properties"), null);
        } catch (IOException e) {
            System.err.println("Error updating properties file: " + e.getMessage());
        }
    }

    public static List<Site> getAllSites() {
        return new ArrayList<>(sitesByName.values());  // Get all sites from the map
    }

    public static void main(String[] args) {
        // Step 1: Print all available sites
        System.out.println("=== Printing All Sites ===");
        Site.printAllSites();
            // Step 2: Get the current site
            System.out.println("\n=== Current Site ===");
            System.out.println("Current Site URL: " + Site.getCurrentSite().getUrl());
            System.out.println("New Current Site Database ID: " + Site.getCurrentSite().getSiteId());
        }
 } 