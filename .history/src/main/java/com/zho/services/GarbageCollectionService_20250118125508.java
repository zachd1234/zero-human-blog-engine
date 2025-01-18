package com.zho.services;

import com.zho.api.wordpress.WordPressPostClient;
import java.util.List;
import java.io.IOException;

public class GarbageCollectionService {
    private final WordPressPostClient wordPressPostClient;

    public GarbageCollectionService(WordPressPostClient wordPressPostClient) {
        this.wordPressPostClient = wordPressPostClient;
    }

    public void deleteAllPosts() {
        try {
            System.out.println("\n=== Starting Post Cleanup ===");
            
            // Method 1: Get all post IDs and delete one by one
            List<Integer> postIds = wordPressPostClient.getAllPostIds();
            System.out.println("Found " + postIds.size() + " posts to delete");
            
            int deleted = 0;
            for (Integer postId : postIds) {
                try {
                    wordPressPostClient.deletePost(postId);
                    deleted++;
                    System.out.println("Deleted post " + postId + " (" + deleted + "/" + postIds.size() + ")");
                } catch (Exception e) {
                    System.err.println("Failed to delete post " + postId + ": " + e.getMessage());
                }
            }
            
            System.out.println("Successfully deleted " + deleted + " posts");
            
        } catch (Exception e) {
            System.err.println("Error during post cleanup: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            System.out.println("Starting Garbage Collection Service Test");
            
            // Create WordPress client (using default constructor)
            WordPressPostClient wpClient = new WordPressPostClient();
            
            // Create and run garbage collection service
            GarbageCollectionService garbageCollector = new GarbageCollectionService(wpClient);
            
            System.out.println("Running delete all posts...");
            garbageCollector.deleteAllPosts();
            
            System.out.println("Test completed successfully");
            
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);  // Exit with error code
        }
    }
} 