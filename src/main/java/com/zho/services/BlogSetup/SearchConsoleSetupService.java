package com.zho.services.BlogSetup;

import com.zho.api.GoogleSearchConsoleClient;
import com.zho.model.Site;
import java.io.IOException;

public class SearchConsoleSetupService {
    private final GoogleSearchConsoleClient searchConsoleClient;
    private static final String CREDENTIALS_PATH = "/Users/zachderhake/Downloads/autokeywords-445618-457181be0577.json";

    public SearchConsoleSetupService() throws IOException {
        this.searchConsoleClient = new GoogleSearchConsoleClient();
    }

    public void indexSite() {
        Site currentSite = Site.getCurrentSite();
        
        if (!currentSite.isActive()) {
            System.out.println("Site is not active. Skipping indexing.");
            return;
        }

        String rootDomain = getRootDomain(currentSite.getUrl());
        try {
            System.out.println("Indexing root domain: " + rootDomain);
            searchConsoleClient.submitUrl(rootDomain);
            System.out.println("Successfully indexed: " + rootDomain);
        } catch (IOException e) {
            System.err.println("Failed to index site: " + e.getMessage());
        }
    }

    private String getRootDomain(String url) {
        // Convert WordPress API URL to root domain
        // Example: "https://ruckquest.com/wp-json/wp/v2/" -> "https://ruckquest.com"
        return url.split("/wp-json")[0];
    }

    // Test method
    public static void main(String[] args) {
        try {
            SearchConsoleSetupService service = new SearchConsoleSetupService();
            service.indexSite();
        } catch (IOException e) {
            System.err.println("Error initializing service: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
