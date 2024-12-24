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
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;
import java.io.IOException;
import org.apache.hc.core5.http.ParseException;
import com.zho.content.LogoGenerator;


public class WordPressUpdater {
    private CloseableHttpClient httpClient;

    public WordPressUpdater() {
        this.httpClient = createHttpClient();
    }

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

    public void updateSiteLogo(BufferedImage logo) throws IOException, ParseException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(logo, "png", baos);
        byte[] imageBytes = baos.toByteArray();

        String url = WordPressConfig.BASE_URL + "media";
        
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody("file", imageBytes, ContentType.IMAGE_PNG, "site-logo.png");
        HttpEntity multipart = builder.build();

        HttpPost uploadRequest = new HttpPost(URI.create(url));
        uploadRequest.setEntity(multipart);
        setAuthHeader(uploadRequest);

        try (CloseableHttpResponse response = httpClient.execute(uploadRequest)) {
            try {
                String responseBody = EntityUtils.toString(response.getEntity());
                JSONObject json = new JSONObject(responseBody);
                int mediaId = json.getInt("id");
                updateSiteSetting("site_logo", String.valueOf(mediaId));
            } catch (ParseException e) {
                throw new IOException("Failed to parse response", e);
            }
        }
    }

    public void updateSiteIcon(BufferedImage icon) throws IOException, ParseException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(icon, "png", baos);
        byte[] imageBytes = baos.toByteArray();

        String url = WordPressConfig.BASE_URL + "media";
        
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody("file", imageBytes, ContentType.IMAGE_PNG, "site-icon.png");
        HttpEntity multipart = builder.build();

        HttpPost uploadRequest = new HttpPost(URI.create(url));
        uploadRequest.setEntity(multipart);
        setAuthHeader(uploadRequest);

        try (CloseableHttpResponse response = httpClient.execute(uploadRequest)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONObject json = new JSONObject(responseBody);
            int mediaId = json.getInt("id");
            
            updateSiteSetting("site_icon", String.valueOf(mediaId));
        }
    }

    private void updateSiteSetting(String setting, String value) throws IOException {
        String url = WordPressConfig.BASE_URL + "settings";
        
        JSONObject jsonPayload = new JSONObject();
        jsonPayload.put(setting, value);

        HttpPost request = new HttpPost(URI.create(url));
        setAuthHeader(request);
        request.setEntity(new StringEntity(jsonPayload.toString(), StandardCharsets.UTF_8));
        request.setHeader("Content-Type", "application/json");

        executeRequest(httpClient, request, "Setting");
    }

    private void setAuthHeader(HttpPost request) {
        String auth = WordPressConfig.USERNAME + ":" + WordPressConfig.APPLICATION_PASSWORD;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        request.setHeader("Authorization", "Basic " + encodedAuth);
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

    private CloseableHttpClient createHttpClient() {
        return HttpClients.createDefault();
    }

    public static void main(String[] args) {
        try {
            // Create test logo generator
            LogoGenerator logoGenerator = new LogoGenerator();
            
            // Generate a test logo
            BufferedImage logo = logoGenerator.generateLogo("coffee blog");
            
            // Create updater and update the logo
            WordPressUpdater updater = new WordPressUpdater();
            updater.updateSiteLogo(logo);
            
            System.out.println("Logo updated successfully!");
            
        } catch (Exception e) {
            System.err.println("Error updating logo: " + e.getMessage());
            e.printStackTrace();
        }
    }
}