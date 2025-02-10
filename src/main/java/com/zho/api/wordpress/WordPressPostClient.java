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
        Map<String, Object> postData = new HashMap<>();
        postData.put("title", blogPost.getTitle());
        postData.put("content", blogPost.getContent());
        postData.put("status", "publish");
        postData.put("slug", blogPost.getSlug());

        // Use yoast_meta for both meta description and SEO title
        JSONObject yoastMeta = new JSONObject();
        yoastMeta.put("yoast_wpseo_metadesc", blogPost.getMetaDescription());
        yoastMeta.put("yoast_wpseo_title", blogPost.getTitle()); // Using the same title, you might want to add a separate seoTitle field to BlogPost
        postData.put("yoast_meta", yoastMeta);

        // Add category if ID exists
        int categoryId = blogPost.getCategoryId();
        if (categoryId > 0) {
            postData.put("categories", List.of(categoryId));
        }

        String response = makeRequest("POST", "posts", postData);
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

    public void updatePageTitleAndMeta(int pageId, String metaDescription, String title) throws IOException, ParseException {
        String url = baseUrl + "pages/" + pageId;
        
        JSONObject updatePayload = new JSONObject()
            .put("title", title)
            .put("yoast_meta", new JSONObject()
                .put("yoast_wpseo_metadesc", metaDescription)
                .put("yoast_wpseo_title", title));
        
        HttpPost updateRequest = new HttpPost(URI.create(url));
        setAuthHeader(updateRequest);
        updateRequest.setEntity(new StringEntity(updatePayload.toString(), StandardCharsets.UTF_8));
        updateRequest.setHeader("Content-Type", "application/json");
        
        try (CloseableHttpResponse response = httpClient.execute(updateRequest)) {
            if (response.getCode() != 200) {
                throw new IOException("Failed to update page meta. Status: " + response.getCode());
            }
        }
    }

    public static void main(String[] args) {
        try {
            WordPressPostClient client = new WordPressPostClient();
            
            client.updatePageTitleAndMeta(609, "hi", "hi");
            System.out.println("Meta description and SEO title update attempted for post 1536");
            
        } catch (Exception e) {
            System.err.println("Error during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
}