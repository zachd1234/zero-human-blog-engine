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
import com.zho.api.OpenAIClient;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.io.InputStream;
import java.io.File;
import java.nio.file.Files;

import com.zho.model.Image;
import com.zho.api.UnsplashClient;
import java.util.List;
import java.io.File;
import java.nio.file.Files;

public class WordPressMediaClient extends BaseWordPressClient {

    public WordPressMediaClient() {
        super();
    }

    public void updateSiteLogo(BufferedImage logo) throws IOException, ParseException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(logo, "png", baos);
        byte[] imageBytes = baos.toByteArray();

        // Generate unique filename with timestamp
        String timestamp = String.valueOf(System.currentTimeMillis());
        String filename = "site-logo-" + timestamp + ".png";

        String url = baseUrl + "media";
        
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody("file", imageBytes, ContentType.IMAGE_PNG, filename);
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

    public void updateFavicon(BufferedImage icon) throws IOException, ParseException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(icon, "png", baos);
        byte[] imageBytes = baos.toByteArray();

        // Generate unique filename with timestamp
        String timestamp = String.valueOf(System.currentTimeMillis());
        String filename = "favicon-" + timestamp + ".png";

        String url = baseUrl + "media";
        
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody("file", imageBytes, ContentType.IMAGE_PNG, filename);
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

    public int uploadImage(Image image) throws IOException, ParseException {
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

    public int uploadImage(Image image, String altText, String title, String description) throws IOException, ParseException {
        System.out.println("Original Unsplash URL: " + image.getUrl());
        
        String url = baseUrl + "media";
        HttpPost uploadRequest = new HttpPost(URI.create(url));
        setAuthHeader(uploadRequest);
        
        // Download image data
        byte[] imageData = downloadImage(image.getUrl());
        System.out.println("Downloaded image size: " + imageData.length + " bytes");
        
        // Create multipart form data with metadata
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody(
            "file",
            imageData,
            ContentType.IMAGE_JPEG,
            "image-" + System.currentTimeMillis() + ".jpg"
        );
        builder.addTextBody("alt_text", altText);
        builder.addTextBody("title", title);
        builder.addTextBody("description", description);
        
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

    public void updateSimpleImage(int pageId, Image image) throws IOException, ParseException {
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

    public void updateBackgroundImage(int pageId, String blockId, Image image) throws IOException, ParseException {
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
                    "\"currentOverlayTab\":\"gradient\"," +
                    "\"maxWidth\":900," +
                    "\"bgImg\":\"" + image.getUrl() + "\"," +
                    "\"tabletPadding\":[60,30,60,30]," +
                    "\"overlayGradient\":\"linear-gradient(180deg,var(--global-palette9) 43%,var(--global-palette3) 81%)\"," +
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

    public void updatePostCoverImage(int postId, Image image) throws IOException, ParseException {
        String url = baseUrl + "posts/" + postId + "?context=edit";
        HttpGet getRequest = new HttpGet(URI.create(url));
        setAuthHeader(getRequest);
        
        try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONObject post = new JSONObject(responseBody);
            
            // Generate metadata for the image
            JSONObject metadata = generateImageMetadata(image.getUrl());
            System.out.println("Generated metadata: " + metadata.toString(2));
            
            // Upload image with metadata
            int mediaId = uploadImage(image, 
                metadata.getString("alt_text"),
                metadata.getString("title"),
                metadata.getString("description")
            );
            
            // Create update payload with featured media
            JSONObject updatePayload = new JSONObject();
            updatePayload.put("featured_media", mediaId);
            
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

    public void updateInfoboxImage(int pageId, String infoboxId, Image image) throws IOException, ParseException {
        String url = baseUrl + "pages/" + pageId + "?context=edit";
        HttpGet getRequest = new HttpGet(URI.create(url));
        setAuthHeader(getRequest);
        
        try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONObject page = new JSONObject(responseBody);
            
            JSONObject content = page.getJSONObject("content");
            String currentContent = content.getString("raw");
            
            // Find and replace the infobox image block
            String infoboxPattern = "<!-- wp:kadence/infobox \\{[^}]*\"uniqueID\":\"" + infoboxId + "\"[^}]*\\}[\\s\\S]*?<!-- /wp:kadence/infobox -->";
            String newInfobox = "<!-- wp:kadence/infobox {" +
                    "\"uniqueID\":\"" + infoboxId + "\"," +
                    "\"containerBackground\":\"#f2f2f2\"," +
                    "\"containerBackgroundOpacity\":1," +
                    "\"containerHoverBackground\":\"#f2f2f2\"," +
                    "\"containerHoverBackgroundOpacity\":1," +
                    "\"containerPadding\":[0,0,0,0]," +
                    "\"mediaType\":\"image\"," +
                    "\"mediaImage\":[{" +
                    "\"url\":\"" + image.getUrl() + "\"," +
                    "\"alt\":\"" + image.getDescription() + "\"," +
                    "\"maxWidth\":493," +
                    "\"hoverAnimation\":\"none\"" +
                    "}]," +
                    "\"mediaStyle\":[{" +
                    "\"background\":\"transparent\"," +
                    "\"hoverBackground\":\"transparent\"," +
                    "\"border\":\"#444444\"," +
                    "\"hoverBorder\":\"#444444\"," +
                    "\"borderRadius\":0," +
                    "\"borderWidth\":[0,0,0,0]," +
                    "\"padding\":[0,0,0,0]," +
                    "\"margin\":[0,0,0,0]" +
                    "}]} -->\n" +
                    "<div class=\"kt-info-box" + infoboxId + " wp-block-kadence-infobox\">" +
                    "<div class=\"kt-blocks-info-box-media-container\"><div class=\"kt-blocks-info-box-media\">" +
                    "<img src=\"" + image.getUrl() + "\" alt=\"" + image.getDescription() + "\"/>" +
                    "</div></div></div>\n" +
                    "<!-- /wp:kadence/infobox -->";
            
            String updatedContent = currentContent.replaceAll(infoboxPattern, newInfobox);
            
            JSONObject updatePayload = new JSONObject();
            updatePayload.put("content", new JSONObject().put("raw", updatedContent));
            
            HttpPost updateRequest = new HttpPost(URI.create(url));
            setAuthHeader(updateRequest);
            updateRequest.setEntity(new StringEntity(updatePayload.toString(), StandardCharsets.UTF_8));
            updateRequest.setHeader("Content-Type", "application/json");
            
            try (CloseableHttpResponse updateResponse = httpClient.execute(updateRequest)) {
                int statusCode = updateResponse.getCode();
                System.out.println("Update status for page " + pageId + " infobox: " + statusCode);
            }
        }
    }

    public void UpdateColumnImage(int pageId, String columnId, String imageUrl) throws IOException, ParseException {

        Image image = new Image(
            "",          // Empty ID since it's not from Unsplash
            imageUrl,    // The actual image URL
            ""          // Empty description
        );
        updateColumnImage(pageId, columnId, image);
    }

    public void updateColumnImage(int pageId, String columnId, Image image) throws IOException, ParseException {
        String url = baseUrl + "pages/" + pageId + "?context=edit";
        HttpGet getRequest = new HttpGet(URI.create(url));
        setAuthHeader(getRequest);
        
        try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONObject page = new JSONObject(responseBody);
            
            JSONObject content = page.getJSONObject("content");
            String currentContent = content.getString("raw");
            
            // Find the specific column content
            String columnPattern = "<!-- wp:kadence/column [^>]*\"uniqueID\":\"" + columnId + "\"[\\s\\S]*?/wp:kadence/column -->";
            
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(columnPattern);
            java.util.regex.Matcher matcher = pattern.matcher(currentContent);
            
            if (matcher.find()) {
                String originalColumnContent = matcher.group(0);
                String columnContent = originalColumnContent;
                
                // Find the image block within this specific column
                String imagePattern = "<!-- wp:image [\\s\\S]*?/wp:image -->";
                pattern = java.util.regex.Pattern.compile(imagePattern);
                matcher = pattern.matcher(columnContent);
                
                if (matcher.find()) {
                    // Create new image block with proper WordPress format
                    String newImage = "<!-- wp:image {\"width\":\"550px\",\"height\":\"auto\",\"aspectRatio\":\"1\",\"sizeSlug\":\"large\"} -->\n" +
                            "<figure class=\"wp-block-image size-large is-resized\">" +
                            "<img src=\"" + image.getUrl() + "\" " +
                            "alt=\"" + image.getDescription() + "\" " +
                            "class=\"wp-image-1107\" " +  // Add WordPress image class
                            "style=\"aspect-ratio:1;width:550px;height:auto\"/>" +
                            "</figure>\n" +
                            "<!-- /wp:image -->";
                    
                    columnContent = columnContent.substring(0, matcher.start()) + 
                                  newImage + 
                                  columnContent.substring(matcher.end());
                }
                
                // Replace the entire column content in the page
                String updatedContent = currentContent.replace(originalColumnContent, columnContent);
                
                JSONObject updatePayload = new JSONObject();
                updatePayload.put("content", new JSONObject().put("raw", updatedContent));
                
                HttpPost updateRequest = new HttpPost(URI.create(url));
                setAuthHeader(updateRequest);
                updateRequest.setEntity(new StringEntity(updatePayload.toString(), StandardCharsets.UTF_8));
                updateRequest.setHeader("Content-Type", "application/json");
                
                try (CloseableHttpResponse updateResponse = httpClient.execute(updateRequest)) {
                    int statusCode = updateResponse.getCode();
                    System.out.println("Update status for page " + pageId + " column image: " + statusCode);
                }
            }
        }
    }

    public String uploadMediaFromFile(String localFilePath, String title) throws IOException {
        // Debug prints at start
        System.out.println("DEBUG: Starting media upload");
        System.out.println("DEBUG: Current site URL: " + baseUrl);
        System.out.println("DEBUG: File path: " + localFilePath);
        
        String url = baseUrl + "media";
        System.out.println("DEBUG: Full upload URL: " + url);
        
        // Create multipart request
        HttpPost uploadRequest = new HttpPost(URI.create(url));
        setAuthHeader(uploadRequest);
        
        // Read the file into a byte array
        File file = new File(localFilePath);
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        System.out.println("DEBUG: File size: " + fileBytes.length + " bytes");
        
        // Create multipart entity
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody(
            "file",
            fileBytes,
            ContentType.IMAGE_JPEG,
            file.getName()
        );
        
        // Add title if provided
        if (title != null && !title.isEmpty()) {
            builder.addTextBody("title", title);
            System.out.println("DEBUG: Added title: " + title);
        }
        
        HttpEntity multipart = builder.build();
        uploadRequest.setEntity(multipart);
        
        try (CloseableHttpResponse response = httpClient.execute(uploadRequest)) {
            System.out.println("DEBUG: Response status code: " + response.getCode());
            String responseBody = EntityUtils.toString(response.getEntity());
            System.out.println("DEBUG: Raw response: " + responseBody);
            
            JSONObject mediaResponse = new JSONObject(responseBody);
            String mediaUrl = mediaResponse.getString("source_url");
            System.out.println("Media uploaded successfully. URL: " + mediaUrl);
            
            return mediaUrl;
        } catch (Exception e) {
            System.err.println("Error uploading media: " + e.getMessage());
            System.err.println("DEBUG: Full error:");
            e.printStackTrace();
            throw new IOException("Failed to upload media", e);
        }
    }

    public void updateSiteName(String siteName) throws IOException, ParseException {
        updateSiteSetting("title", siteName);
        System.out.println("Successfully updated site name to: " + siteName);
    }

    public void updateSiteTagline(String tagline) throws IOException, ParseException {
        updateSiteSetting("description", tagline);
        System.out.println("Successfully updated site tagline to: " + tagline);
    }

    public String uploadImageFromUrl(String imageUrl) throws IOException {
        // Download the image from the URL
        byte[] imageData = downloadImageFromUrl(imageUrl);
        
        // Create a temporary file to hold the image data with a unique name
        String timestamp = String.valueOf(System.currentTimeMillis());
        File tempFile = File.createTempFile("uploaded-image-" + timestamp, ".jpg");
        Files.write(tempFile.toPath(), imageData);
        
        // Use the existing upload method to upload the image
        String mediaUrl = uploadMediaFromFile(tempFile.getAbsolutePath(), "Uploaded from URL");
        
        // Optionally, delete the temporary file after upload
        tempFile.delete();
        
        return mediaUrl;
    }

    private byte[] downloadImageFromUrl(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        try (InputStream in = url.openStream()) {
            return in.readAllBytes();
        }
    }

    public void updateSiteLogoFromUrl(String imageUrl) throws IOException, ParseException {
        // Download the image from URL
        byte[] imageBytes = downloadImage(imageUrl);
        
        // Generate unique filename with timestamp
        String timestamp = String.valueOf(System.currentTimeMillis());
        String filename = "site-logo-" + timestamp + ".png";
        
        String url = baseUrl + "media";
        
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody("file", imageBytes, ContentType.IMAGE_PNG, filename);
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

    public void updateFaviconFromUrl(String imageUrl) throws IOException, ParseException {
        // Download the image from URL
        byte[] imageBytes = downloadImage(imageUrl);
        
        // Generate unique filename with timestamp
        String timestamp = String.valueOf(System.currentTimeMillis());
        String filename = "favicon-" + timestamp + ".png";
        
        String url = baseUrl + "media";
        
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody("file", imageBytes, ContentType.IMAGE_PNG, filename);
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

    /**
     * Generates metadata for WordPress media uploads using image analysis
     * @param imageUrl - URL of the image to analyze
     * @return JSONObject containing alt_text, title, description, and caption
     */
    private JSONObject generateImageMetadata(String imageUrl) throws IOException {
        String prompt = "Analyze this image and provide SEO-optimized metadata for WordPress.\n\n" +
                       "Return ONLY a JSON object with these fields:\n" +
                       "- alt_text: Descriptive text for accessibility (under 125 chars)\n" +
                       "- title: SEO-friendly title (under 60 chars)\n" +
                       "- description: 2-3 detailed sentences about the image\n" +
                       "- caption: Engaging text with possible call-to-action (under 155 chars)";

        String response = new OpenAIClient().callVisionModel(imageUrl, prompt);
        System.out.println("DEBUG: Raw API response: " + response);

        try {
            response = response.replaceAll("```json\\s*", "")
                             .replaceAll("```", "")
                             .replaceAll("(?m)^\\s*\"(\\w+)\":\\s*\"[^\"]*\"(?=\\s*,?\\s*$)\\s*\\n", "");

            JSONObject metadata = new JSONObject(response.trim());
            String captionText = metadata.optString("caption", "");
            
            return new JSONObject()
                .put("alt_text", metadata.optString("alt_text", "Image"))
                .put("title", metadata.optString("title", "Image"))
                .put("description", metadata.optString("description", "An image"))
                .put("caption", new JSONObject()
                    .put("raw", captionText)
                    .put("rendered", "<p>" + captionText + "</p>\n"));
                
        } catch (Exception e) {
            System.err.println("Failed to parse JSON response: " + response);
            return new JSONObject()
                .put("alt_text", "Image")
                .put("title", "Image")
                .put("description", "An image")
                .put("caption", new JSONObject()
                    .put("raw", "")
                    .put("rendered", ""));
        }
    }

    // Test method
    public static void main(String[] args) {
        try {
            WordPressMediaClient client = new WordPressMediaClient();
            

            client.updatePostCoverImage(1592, new Image("https://ruckquest.com/wp-content/uploads/2025/01/uploaded-image-17378355178476562370395749309153.jpg"));
        } catch (Exception e) {
            System.err.println("Error during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 