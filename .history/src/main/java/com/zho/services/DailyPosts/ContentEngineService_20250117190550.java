package com.zho.services.DailyPosts;

import com.zho.services.DailyPosts.KeywordResearchService;
import com.zho.services.DatabaseService;
import com.zho.model.BlogRequest;
import com.zho.model.KeywordAnalysis;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Arrays;

public class ContentEngineService {
    private final KeywordResearchService keywordResearchService;
    private final AutoContentWorkflowService autoContentWorkflowService;
    private final DatabaseService databaseService;
    private ScheduledExecutorService scheduler;
    
    public ContentEngineService() {
        this.keywordResearchService = new KeywordResearchService();
        this.autoContentWorkflowService = new AutoContentWorkflowService();
        this.databaseService = new DatabaseService();
    }
    
    public void startContentEngine(BlogRequest request) {
        try {
            // 1. Perform keyword research
            List<KeywordAnalysis> keywords = keywordResearchService.getLongTailKeywords(request, 1000);
            
            // 2. Save to database
            //DB serivce. clear keywords. 
            //databaseService.saveKeywords(keywords);
            
            // 3. Start the automated content workflow
            startScheduledContentCreation();
            
            System.out.println("Content Engine started successfully");
            System.out.println("Found " + keywords.size() + " keywords");
            
        } catch (Exception e) {
            System.err.println("Error starting content engine: " + e.getMessage());
            throw new RuntimeException("Failed to start content engine", e);
        }
    }
    
    private void startScheduledContentCreation() {
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(
            () -> autoContentWorkflowService.processNextKeyword(),
            0, // initial delay
            24, // period
            TimeUnit.HOURS
        );
    }
    
    public void shutdown() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }
    }
    
    public static void main(String[] args) {
        try {
            // Create test keywords
            List<KeywordAnalysis> testKeywords = Arrays.asList(
                new KeywordAnalysis(
                    "phase 1 esa report",
                    1200,
                    20,
                    2.50,
                    9.5,
                    "High intent, low competition keyword"
                ),
                new KeywordAnalysis(
                    "environmental site assessment cost",
                    890,
                    15,
                    3.20,
                    8.8,
                    "Good search volume, moderate competition"
                ),
                new KeywordAnalysis(
                    "how to do phase 1 esa",
                    450,
                    10,
                    1.75,
                    9.2,
                    "Informational intent, very low competition"
                )
            );
            
            // Create database service and save keywords
            DatabaseService dbService = new DatabaseService();
            System.out.println("Saving test keywords to database...");
            dbService.saveKeywords(testKeywords);
            System.out.println("Successfully saved " + testKeywords.size() + " keywords");
            
            // Print the saved keywords
            testKeywords.forEach(kw -> 
                System.out.printf("Saved: %s (Score: %.1f, Searches: %d)\n",
                    kw.getKeyword(),
                    kw.getScore(),
                    kw.getMonthlySearches())
            );
            
        } catch (Exception e) {
            System.err.println("Error in main: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
