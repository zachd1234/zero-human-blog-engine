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
} 