package com.zho.content;

import com.zho.model.*;
import java.util.*;
import okhttp3.*;
import org.json.*;
import java.io.IOException;
import javax.net.ssl.*;
import java.security.cert.X509Certificate;

public class AIContentGenerator {
    
    private static final String OPENAI_API_KEY = "***REMOVED***TFzXgxid0qd1tzKQG6ftWFVfMx3tUsq8jLvUSyu0GYcwuyy7LcKlHVse_j4O_R3eitib08OoA";
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    
    public AboutPage generateAboutPage(BlogNiche niche) {
        String prompt = String.format("Write an engaging and inspiring 'About Us' section for a blog about %s. %s. Highlight the blog’s purpose, its unique offerings. Conclude with a call to action that encourages readers to join the community and explore the topic.", 
            niche.getDisplayName(), niche.getDescription());

        String content = callOpenAI(prompt);
        return new AboutPage(2, content);
    }

    public List<BlogPost> generateBlogPosts(BlogNiche niche) {
        List<BlogPost> posts = new ArrayList<>();
        int[] postIds = {1, 21, 23}; // Your existing post IDs
        int count = 3; // Hardcoded count
        
        for (int i = 0; i < count; i++) {
            String title = callOpenAI(String.format("Generate a highly unique title for a blog post about %s. make it a unique subtopic of %s", 
                niche.getDisplayName(), niche.getDisplayName()));
            String content = callOpenAI(String.format("Write a blog post about %s", title));
            posts.add(new BlogPost(postIds[i], title, content));
        }
        return posts;
    }

    private String callOpenAI(String prompt) {
        OkHttpClient client = new OkHttpClient();
        JSONObject json = new JSONObject();
        json.put("model", "gpt-3.5-turbo");
        json.put("messages", new JSONArray()
            .put(new JSONObject()
                .put("role", "user")
                .put("content", prompt)));

        RequestBody body = RequestBody.create(
            json.toString(), MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
            .url(OPENAI_API_URL)
            .header("Authorization", "Bearer " + OPENAI_API_KEY)
            .post(body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            JSONObject responseBody = new JSONObject(response.body().string());
            return responseBody.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim();
        } catch (IOException e) {
            e.printStackTrace();
            return "Error generating content";
        }
    }

    public String generateBlogName(String topic) {
        String prompt = String.format(
            "Generate a short, catchy blog name for a blog about %s. " +
            "Requirements:\n" +
            "- Maximum 3 words\n" +
            "- No special characters\n" +
            "- Should be memorable and brandable\n" +
            "Return ONLY the name, nothing else.", 
            topic
        );
        
        return callOpenAI(prompt).trim();
    }

}