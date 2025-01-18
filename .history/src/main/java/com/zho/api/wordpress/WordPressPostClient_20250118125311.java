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
import java.util.ArrayList;
import java.util.HashMap;
import com.zho.model.BlogPost;
import org.apache.hc.core5.http.ClassicHttpRequest;
import com.zho.config.ConfigManager;
import java.util.List;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import okhttp3.Response;
import okhttp3.Request;
import org.apache.hc.client5.http.classic.methods.HttpDelete;

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

    public class PostResponse {
        private final String url;
        private final int id;
        
        public PostResponse(String url, int id) {
            this.url = url;
            this.id = id;
        }
        
        public String getUrl() { return url; }
        public int getId() { return id; }
    }

    public PostResponse publishPost(BlogPost blogPost) throws IOException {
        // Create post request body
        Map<String, Object> postData = new HashMap<>();
        postData.put("title", blogPost.getTitle());
        postData.put("content", blogPost.getContent());
        postData.put("status", "publish");
        postData.put("slug", blogPost.getSlug());


        JSONObject meta = new JSONObject();
        meta.put("_yoast_wpseo_metadesc", blogPost.getMetaDescription());
        postData.put("meta", meta);


        // Add category if ID exists
        int categoryId = blogPost.getCategoryId();
        if (categoryId > 0) {
            postData.put("categories", List.of(categoryId));
        }

        // Make API request
        String response = makeRequest("POST", "posts", postData);
        
        // Extract URL and ID from response
        JSONObject jsonResponse = new JSONObject(response);
        return new PostResponse(
            jsonResponse.getString("link"),
            jsonResponse.getInt("id")
        );
    }

    public List<Integer> getAllPostIds() throws IOException, ParseException {
        String url = baseUrl + "posts?per_page=100&fields=id";
        HttpGet request = new HttpGet(URI.create(url));
        
        String response = executeRequest(request);
        List<Integer> postIds = new ArrayList<>();
        
        JSONArray posts = new JSONArray(response);
        for (int i = 0; i < posts.length(); i++) {
            postIds.add(posts.getJSONObject(i).getInt("id"));
        }
        
        return postIds;
    }

    public void deletePost(int postId) throws IOException {
        String url = baseUrl + "posts/" + postId + "?force=true";
        HttpDelete request = new HttpDelete(URI.create(url));
        executeRequest(request);
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
            
            PostResponse postResponse = client.publishPost(testPost);
            System.out.println("Successfully published post at: " + postResponse.getUrl());
            
        } catch (Exception e) {
            System.err.println("Error during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
}