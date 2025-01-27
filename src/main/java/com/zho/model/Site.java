package com.zho.model;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.zho.services.DatabaseService;
import java.util.Properties;
import java.util.HashMap;
import java.util.Map;

public enum Site {

    //toggle blog status here
    MAIN("https://ruckquest.com/wp-json/wp/v2/", 1, true),
    TEST("https://mbt.dsc.mybluehost.me/wp-json/wp/v2/", 2, false);

    private static Map<String, Site> sites = new HashMap<>();
    private static Site CURRENT_SITE = DatabaseService.loadCurrentSiteFromDatabase();  // Default to MAIN

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
        CURRENT_SITE = DatabaseService.loadCurrentSiteFromDatabase();
        return CURRENT_SITE;
    }

    public static void SwitchSite(Site newSite) {
        CURRENT_SITE = newSite;
        DatabaseService.updateCurrentSiteInDatabase(newSite.getSiteId());
        updatePropertiesFile();
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
        return new ArrayList<>(sites.values());  // Get all sites from the map
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