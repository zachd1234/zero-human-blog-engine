package com.zho.api.wordpress;

import org.json.JSONObject;
import org.json.JSONArray;
import java.io.IOException;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.core5.http.io.entity.StringEntity;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import com.zho.model.UnsplashImage;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;
import java.util.Map;
import java.util.HashMap;
import com.zho.model.BlogPost;
import org.apache.hc.core5.http.ClassicHttpRequest;

public class WordPressPostClient extends BaseWordPressClient {
    public WordPressPostClient() {
        super();
    }

    public void updatePost(int postId, JSONObject updateData) throws IOException, ParseException {
        String url = baseUrl + "posts/" + postId;
        
        HttpPatch request = new HttpPatch(URI.create(url));
        request.setEntity(new StringEntity(updateData.toString(), StandardCharsets.UTF_8));
        request.setHeader("Content-Type", "application/json");
        
        executeRequest(request);
    }
    
    public void updatePostContent(int postId, String content) throws IOException, ParseException {
        // Split content by line breaks and wrap each paragraph
        String[] paragraphs = content.split("\n\n");
        StringBuilder formattedContent = new StringBuilder();
        
        for (String paragraph : paragraphs) {
            if (!paragraph.trim().isEmpty()) {
                formattedContent.append(String.format(
                    "<!-- wp:paragraph -->\n<p>%s</p>\n<!-- /wp:paragraph -->\n",
                    paragraph.trim()
                ));
            }
        }
        
        JSONObject updateData = new JSONObject();
        updateData.put("content", formattedContent.toString());
        updatePost(postId, updateData);
    }
    
    public void updatePostCategory(int postId, int categoryId) throws IOException, ParseException {
        JSONObject updateData = new JSONObject();
        JSONArray categories = new JSONArray();
        categories.put(categoryId);
        updateData.put("categories", categories);
        updatePost(postId, updateData);
    }
    
    public void updatePostTitle(int postId, String title) throws IOException, ParseException {
        JSONObject updateData = new JSONObject();
        updateData.put("title", title);
        updatePost(postId, updateData);
    }

    public String publishPost(BlogPost blogPost) throws IOException {
        // Create post request body
        Map<String, Object> postData = new HashMap<>();
        postData.put("title", blogPost.getTitle());
        postData.put("content", blogPost.getContent());
        postData.put("status", "publish");

        // Make API request
        String response = makeRequest("POST", "/wp/v2/posts", postData);
        
        // Extract and return post URL from response
        JSONObject jsonResponse = new JSONObject(response);
        return jsonResponse.getString("link");
    }

    private String makeRequest(String method, String endpoint, Map<String, Object> data) throws IOException {
        String url = baseUrl + endpoint;
        JSONObject jsonData = new JSONObject(data);
        
        HttpPost request = new HttpPost(url);
        request.setEntity(new StringEntity(jsonData.toString(), StandardCharsets.UTF_8));
        request.setHeader("Content-Type", "application/json");
        
        return executeRequest(request);
    }

    public static void main(String[] args) {
        try {
            WordPressPostClient client = new WordPressPostClient();
            int[] postIds = {23, 21, 1};  // Your standard post IDs
            
            // Test updating post titles
            System.out.println("Testing post title updates...");
            for (int postId : postIds) {
                client.updatePostTitle(postId, "hi Post " + postId);
            }
            
            // Test updating post content
            System.out.println("\nhi...");
            String testContent = "poop";
            for (int postId : postIds) {
                client.updatePostContent(postId, testContent);
            }
            
            // Test updating post categories
            System.out.println("\nTesting post category updates...");
            // Assuming category ID 1 is "Uncategorized"
            for (int postId : postIds) {
                client.updatePostCategory(postId, 1);
            }
            
            // Test updating multiple properties at once
            System.out.println("\nTesting multiple property updates...");
            JSONObject multiUpdate = new JSONObject()
                .put("title", "Combined Update Test")
                .put("content", testContent)
                .put("categories", new JSONArray().put(1));
            
            client.updatePost(postIds[0], multiUpdate);
            
            System.out.println("\nAll post updates completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Error during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
}