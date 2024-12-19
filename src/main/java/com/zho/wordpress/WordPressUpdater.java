package com.zho.wordpress;

import com.zho.model.*;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import com.zho.model.AboutPage;
import com.zho.model.BlogPost;
import org.json.JSONObject;


public class WordPressUpdater {
    public void updatePost(BlogPost post) {
        String url = WordPressConfig.BASE_URL + "posts/" + post.getId();

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost postRequest = createPostRequest(url, post.getTitle(), post.getContent());
            executeRequest(httpClient, postRequest, "Post");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateAboutPage(AboutPage page) {
        String url = WordPressConfig.BASE_URL + "pages/" + page.getId();

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost postRequest = createPostRequest(url, null, page.getContent());
            executeRequest(httpClient, postRequest, "Page");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HttpPost createPostRequest(String url, String title, String body) {
        HttpPost postRequest = new HttpPost(URI.create(url));
        String auth = WordPressConfig.USERNAME + ":" + WordPressConfig.APPLICATION_PASSWORD;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        postRequest.setHeader("Authorization", "Basic " + encodedAuth);
        postRequest.setHeader("Content-Type", "application/json");

        JSONObject jsonPayload = new JSONObject();
        if (title != null) {
            jsonPayload.put("title", title);
        }
        jsonPayload.put("content", body);
        
        postRequest.setEntity(new StringEntity(jsonPayload.toString(), StandardCharsets.UTF_8));
        return postRequest;
    }

    private void executeRequest(CloseableHttpClient httpClient, HttpPost postRequest, String type) {
        try (CloseableHttpResponse response = httpClient.execute(postRequest)) {
            int statusCode = response.getCode();
            HttpEntity responseEntity = response.getEntity();
            String responseString = responseEntity != null ? EntityUtils.toString(responseEntity) : "";

            if (statusCode >= 200 && statusCode < 300) {
                System.out.println(type + " updated successfully: " + responseString);
            } else {
                System.err.println("Failed to update " + type + ". Status Code: " + statusCode);
                System.err.println("Response: " + responseString);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}