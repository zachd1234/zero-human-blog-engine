package com.zho.api.wordpress;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
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
import java.util.Base64;

import com.zho.model.Image;
import com.zho.api.UnsplashClient;
import java.util.List;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import org.json.JSONArray;

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

        try (CloseableHttpResponse uploadResponse = httpClient.execute(uploadRequest)) {
            String responseBody = EntityUtils.toString(uploadResponse.getEntity());
            JSONObject json = new JSONObject(responseBody);
            int mediaId = json.getInt("id");
            updateSiteSetting("site_logo", String.valueOf(mediaId));
        }
    }

    public void updateFavicon(BufferedImage icon, String siteName) throws IOException, ParseException {
        // Generate metadata using AI vision
        String prompt = "Analyze this favicon/icon image and provide a brief description.\n\n" +
                       "Return ONLY a JSON object with this field:\n" +
                       "- description: Brief description of the favicon (under 100 chars)";
        
        // Convert BufferedImage to Base64 for AI vision
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(icon, "png", baos);
        byte[] imageBytes = baos.toByteArray();
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String dataUrl = "data:image/png;base64," + base64Image;
        
        String response = new OpenAIClient().callVisionModel(dataUrl, prompt);
        JSONObject aiResponse = new JSONObject(response.replaceAll("```json\\s*|```", "").trim());
        String description = aiResponse.optString("description", "Website favicon");
        
        // Format title
        String title = siteName.toLowerCase().replaceAll("[^a-z0-9\\s]", "").replaceAll("\\s+", "-") + "-favicon";
        
        // Generate unique filename with timestamp
        String timestamp = String.valueOf(System.currentTimeMillis());
        String filename = title + "-" + timestamp + ".png";
        
        String url = baseUrl + "media";
        
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody("file", imageBytes, ContentType.IMAGE_PNG, filename);
        builder.addTextBody("title", title);
        builder.addTextBody("alt_text", description);
        HttpEntity multipart = builder.build();

        HttpPost uploadRequest = new HttpPost(URI.create(url));
        uploadRequest.setEntity(multipart);
        setAuthHeader(uploadRequest);

        try (CloseableHttpResponse uploadResponse = httpClient.execute(uploadRequest)) {
            String responseBody = EntityUtils.toString(uploadResponse.getEntity());
            JSONObject json = new JSONObject(responseBody);
            int mediaId = json.getInt("id");
            updateSiteSetting("site_icon", String.valueOf(mediaId));
        }
    }

    private void updateSiteSetting(String setting, String value) throws IOException, ParseException {
        String url = baseUrl + "settings";
        System.out.println("Updating site setting: " + setting + " to value: " + value);
        
        JSONObject jsonPayload = new JSONObject();
        jsonPayload.put(setting, value);

        HttpPost request = new HttpPost(URI.create(url));
        setAuthHeader(request);
        request.setEntity(new StringEntity(jsonPayload.toString(), StandardCharsets.UTF_8));
        request.setHeader("Content-Type", "application/json");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getCode();
            String responseBody = EntityUtils.toString(response.getEntity());
            System.out.println("Update setting response code: " + statusCode);
            System.out.println("Update setting response: " + responseBody);
            
            if (statusCode >= 300) {
                throw new IOException("Failed to update " + setting + ". Status: " + statusCode + ", Response: " + responseBody);
            }
        }
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

    public int uploadImage(Image image, String altText, String title) throws IOException, ParseException {
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
        builder.addTextBody("alt_text", altText);
        builder.addTextBody("title", title);
        
        HttpEntity multipart = builder.build();
        uploadRequest.setEntity(multipart);
        
        try (CloseableHttpResponse uploadResponse = httpClient.execute(uploadRequest)) {
            String responseBody = EntityUtils.toString(uploadResponse.getEntity());
            System.out.println("WordPress Media Response: " + responseBody);
            JSONObject mediaResponse = new JSONObject(responseBody);
            int mediaId = mediaResponse.getInt("id");
            System.out.println("Uploaded image ID: " + mediaId);
            return mediaId;
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
        
        try (CloseableHttpResponse uploadResponse = httpClient.execute(uploadRequest)) {
            String responseBody = EntityUtils.toString(uploadResponse.getEntity());
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
        // First, upload the image and get metadata
        JSONObject metadata = generateImageMetadata(image.getUrl());
        System.out.println("Generated metadata: " + metadata.toString());
        
        int mediaId = uploadImage(image, 
            metadata.getString("alt_text"),
            metadata.getString("title")
        );
        System.out.println("Uploaded media ID: " + mediaId);
        
        // Get the WordPress media URL from the upload
        String url = baseUrl + "media/" + mediaId;
        System.out.println("Getting media URL from: " + url);
        HttpGet getRequest = new HttpGet(URI.create(url));
        setAuthHeader(getRequest);
        String uploadedImageUrl;
        try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONObject mediaInfo = new JSONObject(responseBody);
            uploadedImageUrl = mediaInfo.getString("source_url");
            System.out.println("Got uploaded image URL: " + uploadedImageUrl);
        }
        
        // Update the page content
        url = baseUrl + "pages/" + pageId + "?context=edit";
        System.out.println("Getting page content from: " + url);
        getRequest = new HttpGet(URI.create(url));
        setAuthHeader(getRequest);
        
        try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONObject page = new JSONObject(responseBody);
            
            JSONObject content = page.getJSONObject("content");
            String currentContent = content.getString("raw");
            
            // Find and replace the image block
            String imagePattern = "<!-- wp:image \\{[^}]*\\}[\\s\\S]*?<!-- /wp:image -->";
            String newImage = String.format(
                "<!-- wp:image {\"id\":%d,\"sizeSlug\":\"large\",\"linkDestination\":\"none\"} -->\n" +
                "<figure class=\"wp-block-image size-large\">" +
                "<img src=\"%s\" alt=\"%s\" class=\"wp-image-%d\"/></figure>\n" +
                "<!-- /wp:image -->",
                mediaId,
                uploadedImageUrl,
                metadata.getString("alt_text"),
                mediaId
            );
            
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
                if (statusCode >= 300) {
                    String errorResponse = EntityUtils.toString(updateResponse.getEntity());
                    System.err.println("Error response: " + errorResponse);
                }
            }
        }
    }

    public void updateBackgroundImage(int pageId, String blockId, Image image) throws IOException, ParseException {
        // First, upload the image and get metadata
        JSONObject metadata = generateImageMetadata(image.getUrl());
        System.out.println("Generated metadata: " + metadata.toString());
        
        int mediaId = uploadImage(image, 
            metadata.getString("alt_text"),
            metadata.getString("title")
        );
        System.out.println("Uploaded media ID: " + mediaId);
        
        // Get the WordPress media URL from the upload
        String url = baseUrl + "media/" + mediaId;
        System.out.println("Getting media URL from: " + url);
        HttpGet getRequest = new HttpGet(URI.create(url));
        setAuthHeader(getRequest);

        String uploadedImageUrl = getMediaUrl(mediaId);
        // Update the page content
        url = baseUrl + "pages/" + pageId + "?context=edit";
        System.out.println("Getting page content from: " + url);
        getRequest = new HttpGet(URI.create(url));
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
                    "\"bgImg\":\"" + uploadedImageUrl + "\"," +
                    "\"bgImgID\":" + mediaId + "," +
                    "\"bgImgAlt\":\"" + metadata.getString("alt_text") + "\"," +
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
                if (statusCode >= 300) {
                    String errorResponse = EntityUtils.toString(updateResponse.getEntity());
                    System.err.println("Error response: " + errorResponse);
                }
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
                metadata.getString("title")
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
        // First, upload the image and get metadata
        JSONObject metadata = generateImageMetadata(image.getUrl());
        System.out.println("Generated metadata: " + metadata.toString());
        
        int mediaId = uploadImage(image, 
            metadata.getString("alt_text"),
            metadata.getString("title")
        );
        System.out.println("Uploaded media ID: " + mediaId);
        
        // Get the WordPress media URL from the upload
        String uploadedImageUrl = getMediaUrl(mediaId);
        System.out.println("Got uploaded image URL: " + uploadedImageUrl);
        
        // Update the page content
        String url = baseUrl + "pages/" + pageId + "?context=edit";
        System.out.println("Getting page content from: " + url);
        HttpGet getRequest = new HttpGet(URI.create(url));
        setAuthHeader(getRequest);
        
        try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONObject page = new JSONObject(responseBody);
            
            JSONObject content = page.getJSONObject("content");
            String currentContent = content.getString("raw");
            
            System.out.println("Looking for Kadence column with uniqueID: " + infoboxId);
            
            // Find the Kadence column with this exact structure:
            // <!-- wp:kadence/column {"borderWidth":["","","",""],"uniqueID":"318_eaccd9-42","kbVersion":2} -->
            String columnPattern = "<!-- wp:kadence/column [^>]*\"uniqueID\":\"" + infoboxId + "\"[\\s\\S]*?/wp:kadence/column -->";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(columnPattern);
            java.util.regex.Matcher matcher = pattern.matcher(currentContent);
            
            if (matcher.find()) {
                String columnContent = matcher.group(0);
                System.out.println("Found Kadence column with uniqueID: " + infoboxId);
                
                // Find the image block within this column with this exact structure:
                // <!-- wp:image {"id":2570,"width":"550px","height":"auto","aspectRatio":"1","sizeSlug":"full","linkDestination":"none"} -->
                String imageBlockPattern = "<!-- wp:image [\\s\\S]*?/wp:image -->";
                pattern = java.util.regex.Pattern.compile(imageBlockPattern);
                matcher = pattern.matcher(columnContent);
                
                if (matcher.find()) {
                    String imageBlock = matcher.group(0);
                    System.out.println("Found image block within column");
                    
                    // Create updated image block with the exact same attributes as in the example HTML
                    String updatedImageBlock = String.format(
                        "<!-- wp:image {\"id\":%d,\"width\":\"550px\",\"height\":\"auto\",\"aspectRatio\":\"1\",\"sizeSlug\":\"full\",\"linkDestination\":\"none\"} -->\n" +
                        "<figure class=\"wp-block-image size-full is-resized\">" +
                        "<img src=\"%s\" alt=\"%s\" class=\"wp-image-%d\" style=\"aspect-ratio:1;width:550px;height:auto\"/></figure>\n" +
                        "<!-- /wp:image -->",
                        mediaId,
                        uploadedImageUrl,
                        metadata.getString("alt_text"),
                        mediaId
                    );
                    
                    // Replace the image block within the column content
                    String updatedColumnContent = columnContent.replace(imageBlock, updatedImageBlock);
                    
                    // Now replace the entire column in the page content
                    String updatedContent = currentContent.replace(columnContent, updatedColumnContent);
                    
                    JSONObject updatePayload = new JSONObject();
                    updatePayload.put("content", new JSONObject().put("raw", updatedContent));
                    
                    HttpPost updateRequest = new HttpPost(URI.create(url));
                    setAuthHeader(updateRequest);
                    updateRequest.setEntity(new StringEntity(updatePayload.toString(), StandardCharsets.UTF_8));
                    updateRequest.setHeader("Content-Type", "application/json");
                    
                    try (CloseableHttpResponse updateResponse = httpClient.execute(updateRequest)) {
                        int statusCode = updateResponse.getCode();
                        System.out.println("Update status for page " + pageId + " infobox image: " + statusCode);
                        if (statusCode >= 300) {
                            String errorResponse = EntityUtils.toString(updateResponse.getEntity());
                            System.err.println("Error response: " + errorResponse);
                            throw new IOException("Failed to update infobox image: " + errorResponse);
                        } else {
                            System.out.println("Successfully updated infobox image");
                        }
                    }
                    return;
                } else {
                    System.out.println("Could not find image block within column " + infoboxId);
                    
                    // Try to find any img tag within the column
                    pattern = java.util.regex.Pattern.compile("<img[^>]*>");
                    matcher = pattern.matcher(columnContent);
                    
                    if (matcher.find()) {
                        String imgTag = matcher.group(0);
                        System.out.println("Found img tag directly in the column. Attempting direct update.");
                        
                        // Create a complete new image block
                        String newImageBlock = String.format(
                            "<!-- wp:image {\"id\":%d,\"width\":\"550px\",\"height\":\"auto\",\"aspectRatio\":\"1\",\"sizeSlug\":\"full\",\"linkDestination\":\"none\"} -->\n" +
                            "<figure class=\"wp-block-image size-full is-resized\">" +
                            "<img src=\"%s\" alt=\"%s\" class=\"wp-image-%d\" style=\"aspect-ratio:1;width:550px;height:auto\"/></figure>\n" +
                            "<!-- /wp:image -->",
                            mediaId,
                            uploadedImageUrl,
                            metadata.getString("alt_text"),
                            mediaId
                        );
                        
                        // Check if there's a surrounding figure tag
                        pattern = java.util.regex.Pattern.compile("<figure[^>]*>[\\s\\S]*?</figure>");
                        matcher = pattern.matcher(columnContent);
                        
                        String updatedColumnContent;
                        if (matcher.find()) {
                            // Replace the entire figure tag
                            String figureTag = matcher.group(0);
                            updatedColumnContent = columnContent.replace(figureTag, 
                                "<figure class=\"wp-block-image size-full is-resized\">" +
                                "<img src=\"" + uploadedImageUrl + "\" alt=\"" + metadata.getString("alt_text") + 
                                "\" class=\"wp-image-" + mediaId + "\" style=\"aspect-ratio:1;width:550px;height:auto\"/></figure>");
                        } else {
                            // Just replace the img tag with the whole image block
                            updatedColumnContent = columnContent.replace(imgTag, newImageBlock);
                        }
                        
                        // Update the content
                        String updatedContent = currentContent.replace(columnContent, updatedColumnContent);
                        
                        JSONObject updatePayload = new JSONObject();
                        updatePayload.put("content", new JSONObject().put("raw", updatedContent));
                        
                        HttpPost updateRequest = new HttpPost(URI.create(url));
                        setAuthHeader(updateRequest);
                        updateRequest.setEntity(new StringEntity(updatePayload.toString(), StandardCharsets.UTF_8));
                        updateRequest.setHeader("Content-Type", "application/json");
                        
                        try (CloseableHttpResponse updateResponse = httpClient.execute(updateRequest)) {
                            int statusCode = updateResponse.getCode();
                            System.out.println("Update status for page " + pageId + " infobox image (alternative approach): " + statusCode);
                            if (statusCode >= 300) {
                                String errorResponse = EntityUtils.toString(updateResponse.getEntity());
                                System.err.println("Error response: " + errorResponse);
                            }
                        }
                        return;
                    }
                }
            } else {
                System.out.println("Could not find Kadence column with uniqueID: " + infoboxId);
                
                // Debug: Print a portion of the content for diagnostic
                if (currentContent.contains(infoboxId)) {
                    int idIndex = currentContent.indexOf(infoboxId);
                    int startIndex = Math.max(0, idIndex - 100);
                    int endIndex = Math.min(currentContent.length(), idIndex + 200);
                    System.out.println("Context around ID: " + currentContent.substring(startIndex, endIndex));
                    
                    // Try a more lenient pattern
                    String lenientPattern = "[\\s\\S]*?" + infoboxId + "[\\s\\S]*?";
                    pattern = java.util.regex.Pattern.compile(lenientPattern);
                    matcher = pattern.matcher(currentContent);
                    
                    if (matcher.find()) {
                        String matchedContent = matcher.group(0);
                        System.out.println("Found content with ID using lenient match. Length: " + matchedContent.length());
                        
                        // Look for image tag in this content
                        pattern = java.util.regex.Pattern.compile("<img[^>]*>");
                        matcher = pattern.matcher(matchedContent.substring(Math.max(0, matchedContent.length() - 500)));
                        
                        if (matcher.find()) {
                            String imgTag = matcher.group(0);
                            System.out.println("Found img tag in lenient match context: " + imgTag);
                            
                            // Create new img tag
                            String newImgTag = String.format(
                                "<img src=\"%s\" alt=\"%s\" class=\"wp-image-%d\" style=\"aspect-ratio:1;width:550px;height:auto\"",
                                uploadedImageUrl,
                                metadata.getString("alt_text"),
                                mediaId
                            );
                            
                            if (imgTag.endsWith("/>")) {
                                newImgTag += "/>";
                            } else {
                                newImgTag += ">";
                            }
                            
                            // Replace just the img tag
                            String updatedContent = currentContent.replace(imgTag, newImgTag);
                            
                            JSONObject updatePayload = new JSONObject();
                            updatePayload.put("content", new JSONObject().put("raw", updatedContent));
                            
                            HttpPost updateRequest = new HttpPost(URI.create(url));
                            setAuthHeader(updateRequest);
                            updateRequest.setEntity(new StringEntity(updatePayload.toString(), StandardCharsets.UTF_8));
                            updateRequest.setHeader("Content-Type", "application/json");
                            
                            try (CloseableHttpResponse updateResponse = httpClient.execute(updateRequest)) {
                                int statusCode = updateResponse.getCode();
                                System.out.println("Last-resort update status: " + statusCode);
                            }
                            return;
                        }
                    }
                }
            }
            
            System.err.println("Failed to update infobox image: Could not identify the correct HTML structure.");
        } catch (Exception e) {
            System.err.println("Error updating infobox: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw to maintain original behavior
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
        // First, upload the image and get metadata
        JSONObject metadata = generateImageMetadata(image.getUrl());
        System.out.println("Generated metadata: " + metadata.toString());
        
        int mediaId = uploadImage(image, 
            metadata.getString("alt_text"),
            metadata.getString("title")
        );
        System.out.println("Uploaded media ID: " + mediaId);
        
        // Get the WordPress media URL from the upload
        String url = baseUrl + "media/" + mediaId;
        System.out.println("Getting media URL from: " + url);
        HttpGet getRequest = new HttpGet(URI.create(url));
        setAuthHeader(getRequest);
        
        String uploadedImageUrl = getMediaUrl(mediaId);
        
        // Update the page content
        url = baseUrl + "pages/" + pageId + "?context=edit";
        System.out.println("Getting page content from: " + url);
        getRequest = new HttpGet(URI.create(url));
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
                    String newImage = String.format(
                        "<!-- wp:image {\"id\":%d,\"width\":\"550px\",\"height\":\"auto\",\"aspectRatio\":\"1\",\"sizeSlug\":\"large\"} -->\n" +
                        "<figure class=\"wp-block-image size-large is-resized\">" +
                        "<img src=\"%s\" " +
                        "alt=\"%s\" " +
                        "class=\"wp-image-%d\" " +
                        "style=\"aspect-ratio:1;width:550px;height:auto\"/>" +
                        "</figure>\n" +
                        "<!-- /wp:image -->",
                        mediaId,
                        uploadedImageUrl,
                        metadata.getString("alt_text"),
                        mediaId
                    );
                    
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
                    if (statusCode >= 300) {
                        String errorResponse = EntityUtils.toString(updateResponse.getEntity());
                        System.err.println("Error response: " + errorResponse);
                    }
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

    public void updateSiteLogoFromUrl(String imageUrl, String siteName) throws IOException, ParseException {
        // Generate metadata using AI vision
        String prompt = "Analyze this logo image and provide a brief description.\n\n" +
                       "Return ONLY a JSON object with this field:\n" +
                       "- description: Brief description of the logo (under 100 chars)";
        
        String response = new OpenAIClient().callVisionModel(imageUrl, prompt);
        JSONObject aiResponse = new JSONObject(response.replaceAll("```json\\s*|```", "").trim());
        String description = aiResponse.optString("description", "Company logo");
        
        // Format title and alt text
        String title = siteName.toLowerCase().replaceAll("[^a-z0-9\\s]", "").replaceAll("\\s+", "-") + "-logo";        
       
        // Download and upload the image
        byte[] imageBytes = downloadImage(imageUrl);
        String timestamp = String.valueOf(System.currentTimeMillis());
        String filename = title + "-" + timestamp + ".png";
        
        String url = baseUrl + "media";
        
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody("file", imageBytes, ContentType.IMAGE_PNG, filename);
        builder.addTextBody("title", title);
        builder.addTextBody("alt_text", description);
        HttpEntity multipart = builder.build();

        HttpPost uploadRequest = new HttpPost(URI.create(url));
        uploadRequest.setEntity(multipart);
        setAuthHeader(uploadRequest);

        try (CloseableHttpResponse uploadResponse = httpClient.execute(uploadRequest)) {
            String responseBody = EntityUtils.toString(uploadResponse.getEntity());
            JSONObject json = new JSONObject(responseBody);
            int mediaId = json.getInt("id");
            updateSiteSetting("site_logo", String.valueOf(mediaId));
        }
    }

    public void updateFaviconFromUrl(String imageUrl, String siteName) throws IOException, ParseException {
        // Generate metadata using AI vision
        String prompt = "Analyze this favicon/icon image and provide a brief description.\n\n" +
                       "Return ONLY a JSON object with this field:\n" +
                       "- description: Brief description of the favicon (under 100 chars)";
        
        String response = new OpenAIClient().callVisionModel(imageUrl, prompt);
        JSONObject aiResponse = new JSONObject(response.replaceAll("```json\\s*|```", "").trim());
        String description = aiResponse.optString("description", "Website favicon");
        
        // Format title and alt text
        String title = siteName.toLowerCase().replaceAll("[^a-z0-9\\s]", "").replaceAll("\\s+", "-") + "-favicon";
        
        // Download and upload the image
        byte[] imageBytes = downloadImage(imageUrl);
        String timestamp = String.valueOf(System.currentTimeMillis());
        String filename = title + "-" + timestamp + ".png";
        
        String url = baseUrl + "media";
        
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody("file", imageBytes, ContentType.IMAGE_PNG, filename);
        builder.addTextBody("title", title);
        builder.addTextBody("alt_text", description);
        HttpEntity multipart = builder.build();

        HttpPost uploadRequest = new HttpPost(URI.create(url));
        uploadRequest.setEntity(multipart);
        setAuthHeader(uploadRequest);

        try (CloseableHttpResponse uploadResponse = httpClient.execute(uploadRequest)) {
            String responseBody = EntityUtils.toString(uploadResponse.getEntity());
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
                       "- title: Image title with words separated by dashes (under 60 chars)";

        String response = new OpenAIClient().callVisionModel(imageUrl, prompt);
        System.out.println("DEBUG: Raw API response: " + response);

        try {
            response = response.replaceAll("```json\\s*", "")
                             .replaceAll("```", "");

            JSONObject metadata = new JSONObject(response.trim());
            
            // Convert title to dash-separated format
            String title = metadata.optString("title", "Image")
                                 .toLowerCase()
                                 .replaceAll("[^a-z0-9\\s-]", "")
                                 .replaceAll("\\s+", "-");

            return new JSONObject()
                .put("alt_text", metadata.optString("alt_text", "Image"))
                .put("title", title);
                
        } catch (Exception e) {
            System.err.println("Failed to parse JSON response: " + response);
            return new JSONObject()
                .put("alt_text", "Image")
                .put("title", "Image");
        }
    }

    public int uploadAuthorBlock(int pageId, String blockId, Image image, String firstName, String lastName) throws IOException, ParseException {
        String prompt = "Analyze this image and provide a brief description of the person.\n\n" +
                       "Return ONLY a JSON object with this field:\n" +
                       "- description: Brief description of the person (under 100 chars)";

        String response = new OpenAIClient().callVisionModel(image.getUrl(), prompt);
        JSONObject aiResponse = new JSONObject(response.replaceAll("```json\\s*|```", "").trim());
        
        String description = aiResponse.optString("description", "Professional headshot");
        String title = (firstName + "-" + lastName).toLowerCase().replaceAll("[^a-z0-9-]", "");
        String altText = String.format("%s %s: %s", firstName, lastName, description);

        return uploadImage(image, altText, title);
    }

    public String getMediaUrl(int mediaId) throws IOException, ParseException {
        String url = baseUrl + "media/" + mediaId;
        HttpGet getRequest = new HttpGet(URI.create(url));
        setAuthHeader(getRequest);
        
        try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONObject mediaInfo = new JSONObject(responseBody);
            return mediaInfo.getString("source_url");
        }
    }

    public List<Integer> getAllMediaIds() throws IOException, ParseException {
        List<Integer> allIds = new ArrayList<>();
        int page = 1;
        int perPage = 100;
        boolean hasMore = true;

        while (hasMore) {
            String endpoint = baseUrl + "media?per_page=" + perPage + "&page=" + page;
            List<Integer> pageIds = getIdsFromEndpoint(endpoint);
            if (pageIds.isEmpty()) {
                hasMore = false;
            } else {
                allIds.addAll(pageIds);
                page++;
            }
        }
        return allIds;
    }

    private List<Integer> getIdsFromEndpoint(String endpoint) throws IOException, ParseException {
        List<Integer> ids = new ArrayList<>();
        HttpGet request = new HttpGet(URI.create(endpoint));
        setAuthHeader(request);
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            System.out.println("DEBUG: Raw response: " + responseBody);
            
            // Check if response is an error
            if (response.getCode() >= 400) {
                return ids; // Return empty list to signal end of pagination
            }
            
            JSONArray items = new JSONArray(responseBody);
            for (int i = 0; i < items.length(); i++) {
                ids.add(items.getJSONObject(i).getInt("id"));
            }
        }
        return ids;
    }

    public void deleteMedia(Integer mediaId) throws IOException {
        String endpoint = baseUrl + "media/" + mediaId + "?force=true";
        HttpDelete request = new HttpDelete(URI.create(endpoint));
        setAuthHeader(request);
        httpClient.execute(request).close();
    }

    /**
     * Upload media to WordPress directly from base64-encoded image data
     * @param base64ImageData The base64-encoded image data
     * @param filename The filename to use
     * @param title Optional title for the media
     * @return The URL of the uploaded media
     * @throws IOException If there's an error uploading the media
     */
    public String uploadMediaFromBase64(String base64ImageData, String filename, String title) throws IOException {
        System.out.println("DEBUG: Starting media upload from base64 data");
        System.out.println("DEBUG: Current site URL: " + baseUrl);
        
        String url = baseUrl + "media";
        System.out.println("DEBUG: Full upload URL: " + url);
        
        // Create multipart request
        HttpPost uploadRequest = new HttpPost(URI.create(url));
        setAuthHeader(uploadRequest);
        
        // Decode the base64 data
        byte[] imageBytes = Base64.getDecoder().decode(base64ImageData);
        System.out.println("DEBUG: Decoded image size: " + imageBytes.length + " bytes");
        
        // Create multipart entity
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody(
            "file",
            imageBytes,
            ContentType.IMAGE_JPEG,
            filename
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

    public static void main(String[] args) {
        WordPressMediaClient client = new WordPressMediaClient();
        WordPressBlockClient blockClient = new WordPressBlockClient();
        int pageId = 318;
        
        System.out.println("===== TESTING ABOUT PAGE UPDATE METHODS =====");
        
        // Test value prop section updates
        System.out.println("\n1. Testing Value Prop Section Updates:");
        try {
            blockClient.updateAdvancedHeadingText(
                pageId,
                "318_heading_value-prop",
                "oin proposition New"
            );
            System.out.println("✓ Value prop heading updated successfully");
        } catch (Exception e) {
            System.err.println("✗ Error updating value prop heading: " + e.getMessage());
        }
        
        try {
            blockClient.updateParagraphText(
                pageId,
                "318_para_explanation",
                "Test explanation paragraph for the value proposition section."
            );
            System.out.println("✓ Explanation paragraph updated successfully");
        } catch (Exception e) {
            System.err.println("✗ Error updating explanation paragraph: " + e.getMessage());
        }
        
        // Test mission section updates
        System.out.println("\n2. Testing Mission Section Updates:");
        try {
            blockClient.updateHeadingText(
                pageId,
                "318_heading_mission",
                "Test Mission Heading"
            );
            System.out.println("✓ Mission heading updated successfully");
        } catch (Exception e) {
            System.err.println("✗ Error updating mission heading: " + e.getMessage());
        }
        
        try {
            blockClient.updateParagraphText(
                pageId,
                "318_para_mission",
                "Test mission paragraph text. This is where the mission statement would go."
            );
            System.out.println("✓ Mission paragraph updated successfully");
        } catch (Exception e) {
            System.err.println("✗ Error updating mission paragraph: " + e.getMessage());
        }
        
        // Test biography text update
        System.out.println("\n3. Testing Biography Text Update:");
        try {
            blockClient.updateParagraphText(
                pageId,
                "318_para_bio",
                "Test biography text. This is where the personal story would go."
            );
            System.out.println("✓ Biography paragraph updated successfully");
        } catch (Exception e) {
            System.err.println("✗ Error updating biography paragraph: " + e.getMessage());
        }
        
        // Test image updates
        System.out.println("\n4. Testing Image Updates:");
        try {
            // Create a test image object
            Image testImage = new Image("https://ruckquest.com/wp-content/uploads/2025/03/vertex-image-1742014092486.png");
            
            // Test infobox image update
            client.updateInfoboxImage(pageId, "318_1fa743-7a", testImage);
            System.out.println("✓ Infobox image updated successfully");
            
            // Test column image update
            client.UpdateColumnImage(pageId, "318_8978a8-3e", "https://ruckquest.com/wp-content/uploads/2025/03/vertex-image-1742014092486.png");
            System.out.println("✓ Column image updated successfully");
        } catch (Exception e) {
            System.err.println("✗ Error updating images: " + e.getMessage());
        }
        
        System.out.println("\n===== ABOUT PAGE UPDATE TESTS COMPLETE =====");
    }
} 