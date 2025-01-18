package com.zho.api.wordpress;

import org.json.JSONObject;
import org.json.JSONArray;
import java.io.IOException;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.core5.http.io.entity.StringEntity;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import com.zho.model.Image;
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
import com.zho.config.ConfigManager;
import java.util.List;

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
        
        // Add featured media if image exists
        if (blogPost.getCoverImage() != null) {
            String imageUrl = blogPost.getCoverImage().getUrl();
            postData.put("featured_media_url", imageUrl);
        }

        // Add category if not uncategorized
        String category = blogPost.getCategory();
        if (category != null && !category.equals("uncategorized")) {
            // Get category ID and add as array
            Integer categoryId = wpCategoryClient.getCategoryId(category);
            if (categoryId != null) {
                postData.put("categories", List.of(categoryId));
            }
        }

        // Make API request
        String response = makeRequest("POST", "posts", postData);
        
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
            
            // Debug config using correct method name
            System.out.println("WordPress URL from config: " + ConfigManager.getWpBaseUrl());
            
            BlogPost testPost = new BlogPost(-1,
                "Test Post Title",
                "This is a test post content.\n\nIt has multiple paragraphs.\n\nTesting the publishing functionality.", 
                null, 
                null);
            
            String postUrl = client.publishPost(testPost);
            System.out.println("Successfully published post at: " + postUrl);
            
        } catch (Exception e) {
            System.err.println("Error during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
}