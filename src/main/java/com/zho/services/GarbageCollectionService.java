package com.zho.services;

import com.zho.api.wordpress.WordPressPostClient;
import com.zho.api.wordpress.WordPressMediaClient;
import java.util.List;
import java.io.IOException;

public class GarbageCollectionService {
    private final WordPressPostClient wordPressPostClient;
    private final WordPressMediaClient wordPressMediaClient;

    public GarbageCollectionService() {
        this.wordPressPostClient = new WordPressPostClient();
        this.wordPressMediaClient = new WordPressMediaClient();
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

    public void deleteAllImages() {
        try {
            System.out.println("\n=== Starting Image Cleanup ===");
            
            List<Integer> imageIds = wordPressMediaClient.getAllMediaIds();
            System.out.println("Found " + imageIds.size() + " images to delete");
            
            int deleted = 0;
            for (Integer imageId : imageIds) {
                try {
                    wordPressMediaClient.deleteMedia(imageId);
                    deleted++;
                    System.out.println("Deleted image " + imageId + " (" + deleted + "/" + imageIds.size() + ")");
                } catch (Exception e) {
                    System.err.println("Failed to delete image " + imageId + ": " + e.getMessage());
                }
            }
            
            System.out.println("Successfully deleted " + deleted + " images");
            
        } catch (Exception e) {
            System.err.println("Error during image cleanup: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            System.out.println("Starting Garbage Collection Service Test");
            
            
            // Create and run garbage collection service
            GarbageCollectionService garbageCollector = new GarbageCollectionService();

            System.out.println("Running delete all images...");
            garbageCollector.deleteAllImages();
            
            System.out.println("Test completed successfully");
            
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);  // Exit with error code
        }
    }
} 