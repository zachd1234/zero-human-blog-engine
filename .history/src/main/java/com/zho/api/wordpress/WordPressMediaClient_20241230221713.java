package com.zho.api.wordpress;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.json.JSONObject;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;

import com.zho.model.UnsplashImage;
import com.zho.api.UnsplashClient;
import java.util.List;

public class WordPressMediaClient extends BaseWordPressClient {

    public WordPressMediaClient() {
        super();
    }

    public void updateSiteLogo(BufferedImage logo) throws IOException, ParseException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(logo, "png", baos);
        byte[] imageBytes = baos.toByteArray();

        String url = baseUrl + "media";
        
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody("file", imageBytes, ContentType.IMAGE_PNG, "site-logo.png");
        HttpEntity multipart = builder.build();

        HttpPost uploadRequest = new HttpPost(URI.create(url));
        uploadRequest.setEntity(multipart);
        setAuthHeader(uploadRequest);

        try (CloseableHttpResponse response = httpClient.execute(uploadRequest)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONObject json = new JSONObject(responseBody);
            int mediaId = json.getInt("id");
            updateSiteSetting("site_logo", String.valueOf(mediaId));
        }
    }

    public void updateSiteIcon(BufferedImage icon) throws IOException, ParseException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(icon, "png", baos);
        byte[] imageBytes = baos.toByteArray();

        String url = baseUrl + "media";
        
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

    private void updateSiteSetting(String setting, String value) throws IOException, ParseException {
        String url = baseUrl + "settings";
        
        JSONObject jsonPayload = new JSONObject();
        jsonPayload.put(setting, value);

        HttpPost request = new HttpPost(URI.create(url));
        setAuthHeader(request);
        request.setEntity(new StringEntity(jsonPayload.toString(), StandardCharsets.UTF_8));
        request.setHeader("Content-Type", "application/json");

        executeRequest(request, "Setting");
    }

    private void executeRequest(HttpPost request, String type) throws IOException, ParseException {
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getCode();
            HttpEntity responseEntity = response.getEntity();
            String responseString = responseEntity != null ? EntityUtils.toString(responseEntity) : "";

            if (statusCode >= 200 && statusCode < 300) {
                System.out.println(type + " updated successfully: " + responseString);
            } else {
                System.err.println("Failed to update " + type + ". Status Code: " + statusCode);
                System.err.println("Response: " + responseString);
            }
        }
    }

    public int uploadImage(UnsplashImage image) throws IOException, ParseException {
        System.out.println("Original Unsplash URL: " + image.getUrl());
        
        String url = baseUrl + "media";
        HttpPost uploadRequest = new HttpPost(URI.create(url));
        setAuthHeader(uploadRequest);
        
        // Download image data from Unsplash URL first
        byte[] imageData = downloadImage(image.getUrl());
        System.out.println("Downloaded image size: " + imageData.length + " bytes");
        
        // Create multipart form data
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody(
            "file",
            imageData,
            ContentType.IMAGE_JPEG,
            "image-" + System.currentTimeMillis() + ".jpg"
        );
        
        HttpEntity multipart = builder.build();
        uploadRequest.setEntity(multipart);
        
        try (CloseableHttpResponse response = httpClient.execute(uploadRequest)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            System.out.println("WordPress Media Response: " + responseBody);
            JSONObject mediaResponse = new JSONObject(responseBody);
            int mediaId = mediaResponse.getInt("id");
            System.out.println("Uploaded image ID: " + mediaId);
            return mediaId;
        }
    }
    
    private byte[] downloadImage(String imageUrl) throws IOException {
        HttpGet request = new HttpGet(URI.create(imageUrl));
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            return EntityUtils.toByteArray(response.getEntity());
        }
    }

    public void updateSimpleImage(int pageId, UnsplashImage image) throws IOException, ParseException {
        String url = baseUrl + "pages/" + pageId + "?context=edit";
        HttpGet getRequest = new HttpGet(URI.create(url));
        setAuthHeader(getRequest);
        
        try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONObject page = new JSONObject(responseBody);
            
            JSONObject content = page.getJSONObject("content");
            String currentContent = content.getString("raw");
            
            // Find and replace the image block
            String imagePattern = "<!-- wp:image \\{[^}]*\\}[\\s\\S]*?<!-- /wp:image -->";
            String newImage = "<!-- wp:image {\"sizeSlug\":\"large\",\"linkDestination\":\"none\"} -->\n" +
                    "<figure class=\"wp-block-image size-large\">" +
                    "<img src=\"" + image.getUrl() + "\" " +
                    "alt=\"" + image.getDescription() + "\"/>" +
                    "</figure>\n" +
                    "<!-- /wp:image -->";
            
            String updatedContent = currentContent.replaceAll(imagePattern, newImage);
            
            JSONObject updatePayload = new JSONObject();
            updatePayload.put("content", new JSONObject().put("raw", updatedContent));
            
            HttpPost updateRequest = new HttpPost(URI.create(url));
            setAuthHeader(updateRequest);
            updateRequest.setEntity(new StringEntity(updatePayload.toString(), StandardCharsets.UTF_8));
            updateRequest.setHeader("Content-Type", "application/json");
            
            try (CloseableHttpResponse updateResponse = httpClient.execute(updateRequest)) {
                int statusCode = updateResponse.getCode();
                System.out.println("Update status for page " + pageId + ": " + statusCode);
            }
        }
    }

    public void updateBackgroundImage(int pageId, String blockId, UnsplashImage image) throws IOException, ParseException {
        String url = baseUrl + "pages/" + pageId + "?context=edit";
        HttpGet getRequest = new HttpGet(URI.create(url));
        setAuthHeader(getRequest);
        
        try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONObject page = new JSONObject(responseBody);
            
            JSONObject content = page.getJSONObject("content");
            String currentContent = content.getString("raw");
            
            // Find and replace the background image
            String rowPattern = "<!-- wp:kadence/rowlayout \\{[^}]*\"uniqueID\":\"" + blockId + "\"[^}]*\\}[\\s\\S]*?-->";
            String newRow = "<!-- wp:kadence/rowlayout {" +
                    "\"uniqueID\":\"" + blockId + "\"," +
                    "\"columns\":1,\"colLayout\":\"equal\"," +
                    "\"maxWidth\":900," +
                    "\"bgImg\":\"" + image.getUrl() + "\"," +
                    "\"tabletPadding\":[60,30,60,30]," +
                    "\"padding\":[100,20,101,20]," +
                    "\"mobilePadding\":[40,20,40,20]," +
                    "\"kbVersion\":2} -->";
            
            String updatedContent = currentContent.replaceFirst(rowPattern, newRow);
            
            JSONObject updatePayload = new JSONObject();
            updatePayload.put("content", new JSONObject().put("raw", updatedContent));
            
            HttpPost updateRequest = new HttpPost(URI.create(url));
            setAuthHeader(updateRequest);
            updateRequest.setEntity(new StringEntity(updatePayload.toString(), StandardCharsets.UTF_8));
            updateRequest.setHeader("Content-Type", "application/json");
            
            try (CloseableHttpResponse updateResponse = httpClient.execute(updateRequest)) {
                int statusCode = updateResponse.getCode();
                System.out.println("Update status for page " + pageId + " background: " + statusCode);
            }
        }
    }

    public void updatePostCoverImage(int postId, UnsplashImage image) throws IOException, ParseException {
        String url = baseUrl + "posts/" + postId + "?context=edit";
        HttpGet getRequest = new HttpGet(URI.create(url));
        setAuthHeader(getRequest);
        
        try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONObject post = new JSONObject(responseBody);
            
            // Create update payload with featured media
            JSONObject updatePayload = new JSONObject();
            updatePayload.put("featured_media", uploadImage(image));
            
            // Send update
            HttpPost updateRequest = new HttpPost(URI.create(url));
            setAuthHeader(updateRequest);
            updateRequest.setEntity(new StringEntity(updatePayload.toString(), StandardCharsets.UTF_8));
            updateRequest.setHeader("Content-Type", "application/json");
            
            try (CloseableHttpResponse updateResponse = httpClient.execute(updateRequest)) {
                int statusCode = updateResponse.getCode();
                System.out.println("Update status for post " + postId + " cover: " + statusCode);
            }
        }
    }

    // Test method
    public static void main(String[] args) {
        try {
            WordPressMediaClient client = new WordPressMediaClient();
            
            // Test updating site logo and icon
            BufferedImage testImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
            client.updateSiteLogo(testImage);
            client.updateSiteIcon(testImage);
            
            // Get random image from Unsplash
            UnsplashClient unsplashClient = new UnsplashClient();
            List<UnsplashImage> images = unsplashClient.searchImages("AI");
            if (images.isEmpty()) {
                throw new IOException("No images found");
            }
            UnsplashImage testUnsplashImage = images.get(0);
            
            // Test updating simple image
            client.updateSimpleImage(609, testUnsplashImage);
            
            // Test updating background image
            client.updateBackgroundImage(609, "609_fc7adf-33", testUnsplashImage);
            
            // Test updating post cover images
            int[] postIds = {23, 21, 1};  // Your post IDs
            for (int postId : postIds) {
                client.updatePostCoverImage(postId, testUnsplashImage);
            }
            
            System.out.println("All media updates completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Error during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 