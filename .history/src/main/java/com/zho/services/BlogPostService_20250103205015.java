package com.zho.services;

import com.zho.model.BlogPost;
import com.zho.model.UnsplashImage;
import com.zho.api.wordpress.WordPressPostClient;
import com.zho.api.UnsplashClient;
import com.zho.api.OpenAIClient;
import java.io.IOException;
import java.util.List;
import org.apache.hc.core5.http.ParseException;
import java.util.ArrayList;
import com.zho.model.BlogRequest;
import java.util.Arrays;
import com.zho.api.wordpress.WordPressMediaClient;
import com.zho.api.wordpress.WordPressClient;

public class BlogPostService {
    private final WordPressClient wpClient;
    private final OpenAIClient openAIClient;
    private final UnsplashClient unsplashClient;

    public BlogPostService() {
        this.wpClient = new WordPressClient();
        this.openAIClient = new OpenAIClient();
        this.unsplashClient = new UnsplashClient();
    }

    public void createAndPublishPosts(BlogRequest request, int postCount, int[] postIds) throws IOException, ParseException {
        // Step 1: Generate all blog posts
        List<BlogPost> posts = generateBlogPosts(request, postCount, postIds);
        
        // Step 2: Update WordPress with the posts
        updateWordPressPosts(posts);
    }

    private List<BlogPost> generateBlogPosts(BlogRequest request, int postCount, int[] postIds) throws IOException, ParseException {
        List<BlogPost> posts = new ArrayList<>();
        
        // Fetch actual categories from WordPress
        List<String> categories = wpClient.categories().getAllCategories();
        
        for (int i = 0; i < postCount; i++) {
            // Generate title
            String title = openAIClient.callOpenAI(String.format(
                "Generate a highly unique title for a blog post about %s", 
                request.getTopic()
            ));
            
            // Generate content based on title
            String content = openAIClient.callOpenAI(String.format(
                "Write a blog post about %s. Use double line breaks (\\n\\n) between paragraphs for proper formatting.", 
                title
            ));
            
            // Generate image search term and get image URL
            String searchTerm = openAIClient.callOpenAI(String.format(
                "Generate a short, specific search term (2-3 words) to find a relevant image for: %s",
                title
            ));
            UnsplashImage image = unsplashClient.searchImages(searchTerm).get(0);
            
            // Determine category from actual WordPress categories
            String prompt = String.format(
                "Given these categories:\n%s\n\nWhich ONE category best fits this blog post title: '%s'?\n" +
                "Reply with ONLY the exact category name, nothing else.",
                String.join("\n", categories),
                title
            );
            String category = openAIClient.callOpenAI(prompt).trim();
            
            // Create BlogPost object with all fields using provided postIds
            posts.add(new BlogPost(postIds[i], title.trim(), content.trim(), image, category));
        }
        return posts;
    }

    private void updateWordPressPosts(List<BlogPost> posts) throws IOException, ParseException {
        for (BlogPost post : posts) {
            wpClient.posts().updatePostTitle(post.getId(), post.getTitle());
            wpClient.posts().updatePostContent(post.getId(), post.getContent());
            wpClient.media().updatePostCoverImage(post.getId(), post.getCoverImage());  // This needs to be updated
        }
    }

    public static void main(String[] args) {
        try {
            BlogPostService service = new BlogPostService();            
            // Test the update process
            BlogRequest request = new BlogRequest("AI blog", "A blog about AI and machine learning");
            int[] postIds = {23, 21, 1}; // Test with 3 posts
            
            service.createAndPublishPosts(request, 3, postIds);
            
            
        } catch (Exception e) {
            System.err.println("Error during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 