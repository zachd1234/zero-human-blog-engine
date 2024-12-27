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

import org.json.JSONArray;
import org.json.JSONObject;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;
import java.io.IOException;
import org.apache.hc.core5.http.ParseException;
import com.zho.content.LogoGenerator;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.http.HttpRequest;
import com.zho.content.AIContentGenerator;
import com.zho.content.BlogNiche;
import com.zho.images.UnsplashClient;
import com.zho.images.UnsplashImage;
import java.util.List;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import java.net.URLEncoder;
import java.util.ArrayList;
import org.apache.hc.client5.http.classic.methods.HttpPatch;


public class WordPressUpdater {
    private CloseableHttpClient httpClient;
    private AIContentGenerator generator;

    public WordPressUpdater() {
        this.httpClient = createHttpClient();
        this.generator = new AIContentGenerator();
    }

    public void updatePost(BlogPost post) throws IOException, ParseException {
        String url = WordPressConfig.BASE_URL + "posts/" + post.getId();

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost postRequest = createPostRequest(url, post.getTitle(), post.getContent());
            executeRequest(httpClient, postRequest, "Post");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Get all categories
        List<String> categories = getAllCategoryNames();
        
        // Ask GPT which category fits best
        String prompt = String.format(
            "Given these categories:\n%s\n\nWhich ONE category best fits this blog post title: '%s'?\n" +
            "Reply with ONLY the exact category name, nothing else.",
            String.join("\n", categories),
            post.getTitle()
        );
        
        String categoryName = generator.callOpenAI(prompt).trim();
        
        // Find category ID and update post
        String updateUrl = WordPressConfig.BASE_URL + "posts/" + post.getId();
        
        // Debug print
        System.out.println("Updating post with category ID: " + getCategoryId(categoryName));
        
        JSONObject updateData = new JSONObject();
        JSONArray categoryIds = new JSONArray();
        categoryIds.put(getCategoryId(categoryName));
        updateData.put("categories", categoryIds);
        
        HttpPatch updateRequest = new HttpPatch(URI.create(updateUrl));  // Changed to PATCH
        setAuthHeader(updateRequest);
        updateRequest.setEntity(new StringEntity(updateData.toString(), StandardCharsets.UTF_8));
        updateRequest.setHeader("Content-Type", "application/json");
        
        try (CloseableHttpResponse response = httpClient.execute(updateRequest)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            System.out.println("Category update response: " + responseBody);
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

    private void setAuthHeader(HttpRequest request) {
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
    
    public void updateMissionParagraph(String newText) throws IOException, ParseException {
        String url = WordPressConfig.BASE_URL + "pages/609?context=edit";
        HttpGet getRequest = new HttpGet(URI.create(url));
        setAuthHeader(getRequest);
        
        try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONObject page = new JSONObject(responseBody);
            
            // Get the current content with all blocks
            JSONObject content = page.getJSONObject("content");
            String currentContent = content.getString("raw");
            
            // Find our target block and replace just that one
            String targetBlockPattern = "<!-- wp:kadence/advancedheading \\{[^}]*\"uniqueID\":\"609_29304f-69\"[^}]*\\}[\\s\\S]*?<!-- /wp:kadence/advancedheading -->";
            
            String newBlock = "<!-- wp:kadence/advancedheading {" +
                    "\"uniqueID\":\"609_29304f-69\"," +
                    "\"markBorder\":\"\"," +
                    "\"markBorderStyles\":[{\"top\":[null,\"\",\"\"],\"right\":[null,\"\",\"\"],\"bottom\":[null,\"\",\"\"],\"left\":[null,\"\",\"\"],\"unit\":\"px\"}]," +
                    "\"tabletMarkBorderStyles\":[{\"top\":[null,\"\",\"\"],\"right\":[null,\"\",\"\"],\"bottom\":[null,\"\",\"\"],\"left\":[null,\"\",\"\"],\"unit\":\"px\"}]," +
                    "\"mobileMarkBorderStyles\":[{\"top\":[null,\"\",\"\"],\"right\":[null,\"\",\"\"],\"bottom\":[null,\"\",\"\"],\"left\":[null,\"\",\"\"],\"unit\":\"px\"}]," +
                    "\"htmlTag\":\"p\"" +
                    "} -->\n" +
                    "<p class=\"kt-adv-heading609_29304f-69 wp-block-kadence-advancedheading\" " +
                    "data-kb-block=\"kb-adv-heading609_29304f-69\">" + newText + "</p>\n" +
                    "<!-- /wp:kadence/advancedheading -->";
            
            String updatedContent = currentContent.replaceAll(targetBlockPattern, newBlock);
            
            // Create update payload with all content
            JSONObject updatePayload = new JSONObject();
            updatePayload.put("content", new JSONObject().put("raw", updatedContent));
            
            // Send update
            HttpPost updateRequest = new HttpPost(URI.create(url));
            setAuthHeader(updateRequest);
            updateRequest.setEntity(new StringEntity(updatePayload.toString(), StandardCharsets.UTF_8));
            updateRequest.setHeader("Content-Type", "application/json");
            
            try (CloseableHttpResponse updateResponse = httpClient.execute(updateRequest)) {
                int statusCode = updateResponse.getCode();
                String updateResponseBody = EntityUtils.toString(updateResponse.getEntity());
                System.out.println("Update status: " + statusCode);
                System.out.println("Response: " + updateResponseBody);
            }
        }
    }

    public void updateMissionParagraph(BlogNiche niche) throws IOException, ParseException {
        // Get AI-generated mission statement based on niche
        AIContentGenerator generator = new AIContentGenerator();
        String missionText = generator.generateMissionStatement(niche);
        
        // Use existing method to update the paragraph
        updateMissionParagraph(missionText);
    }

    public void updateHeadingAndSubheading(String heading, String subheading) throws IOException, ParseException {
        String url = WordPressConfig.BASE_URL + "pages/609?context=edit";
        HttpGet getRequest = new HttpGet(URI.create(url));
        setAuthHeader(getRequest);
        
        try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONObject page = new JSONObject(responseBody);
            
            JSONObject content = page.getJSONObject("content");
            String currentContent = content.getString("raw");
            
            // Update heading (Building strength...)
            String headingPattern = "<!-- wp:kadence/advancedheading \\{[^}]*\"uniqueID\":\"609_a8d80a-ca\"[^}]*\\}[\\s\\S]*?<!-- /wp:kadence/advancedheading -->";
            String newHeading = "<!-- wp:kadence/advancedheading {" +
                    "\"level\":1,\"uniqueID\":\"609_a8d80a-ca\"," +
                    "\"align\":\"center\",\"color\":\"#ffffff\"," +
                    "\"typography\":\"Jost\",\"googleFont\":true," +
                    "\"fontSubset\":\"latin\",\"fontVariant\":\"700\"," +
                    "\"fontWeight\":\"700\",\"textTransform\":\"none\"," +
                    "\"fontSize\":[60,null,40],\"fontHeight\":[68,null,48]," +
                    "\"fontHeightType\":\"px\"" +
                    "} -->\n" +
                    "<h1 class=\"kt-adv-heading609_a8d80a-ca wp-block-kadence-advancedheading\" " +
                    "data-kb-block=\"kb-adv-heading609_a8d80a-ca\">" + heading + "</h1>\n" +
                    "<!-- /wp:kadence/advancedheading -->";
            
            // Update subheading (expanding sentence)
            String subheadingPattern = "<!-- wp:kadence/advancedheading \\{[^}]*\"uniqueID\":\"609_e56131-4a\"[^}]*\\}[\\s\\S]*?<!-- /wp:kadence/advancedheading -->";
            String newSubheading = "<!-- wp:kadence/advancedheading {" +
                    "\"uniqueID\":\"609_e56131-4a\"," +
                    "\"align\":\"center\",\"color\":\"#020000\"," +
                    "\"htmlTag\":\"p\"" +
                    "} -->\n" +
                    "<p class=\"kt-adv-heading609_e56131-4a wp-block-kadence-advancedheading\" " +
                    "data-kb-block=\"kb-adv-heading609_e56131-4a\">" + subheading + "</p>\n" +
                    "<!-- /wp:kadence/advancedheading -->";
            
            // Replace both blocks
            String updatedContent = currentContent
                .replaceAll(headingPattern, newHeading)
                .replaceAll(subheadingPattern, newSubheading);
            
            // Create update payload
            JSONObject updatePayload = new JSONObject();
            updatePayload.put("content", new JSONObject().put("raw", updatedContent));
            
            // Send update
            HttpPost updateRequest = new HttpPost(URI.create(url));
            setAuthHeader(updateRequest);
            updateRequest.setEntity(new StringEntity(updatePayload.toString(), StandardCharsets.UTF_8));
            updateRequest.setHeader("Content-Type", "application/json");
            
            try (CloseableHttpResponse updateResponse = httpClient.execute(updateRequest)) {
                int statusCode = updateResponse.getCode();
                System.out.println("Update status: " + statusCode);
            }
        }
    }

    public void updateHeadingAndSubheading(BlogNiche niche) throws IOException, ParseException {
        AIContentGenerator generator = new AIContentGenerator();
        String heading = generator.generateHeading(niche);
        String subheading = generator.generateSubheading(niche);
        
        updateHeadingAndSubheading(heading, subheading);
    }

    public void updateValuePropSection(String valueProp, String explanation) throws IOException, ParseException {
        String url = WordPressConfig.BASE_URL + "pages/318?context=edit";
        HttpGet getRequest = new HttpGet(URI.create(url));
        setAuthHeader(getRequest);
        
        try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONObject page = new JSONObject(responseBody);
            
            JSONObject content = page.getJSONObject("content");
            String currentContent = content.getString("raw");
            
            // Update value prop heading
            String valuePropPattern = "<!-- wp:kadence/advancedheading \\{[^}]*\"uniqueID\":\"318_388068-ef\"[^}]*\\}[\\s\\S]*?<!-- /wp:kadence/advancedheading -->";
            String newValueProp = "<!-- wp:kadence/advancedheading {" +
                    "\"level\":1,\"uniqueID\":\"318_388068-ef\"," +
                    "\"lineType\":\"em\",\"margin\":[0,0,48,0]," +
                    "\"fontSize\":[\"xl\",\"\",38],\"fontHeight\":[1,null,null]," +
                    "\"fontHeightType\":\"em\"" +
                    "} -->\n" +
                    "<h1 class=\"kt-adv-heading318_388068-ef wp-block-kadence-advancedheading\" " +
                    "data-kb-block=\"kb-adv-heading318_388068-ef\">" + valueProp + "</h1>\n" +
                    "<!-- /wp:kadence/advancedheading -->";
            
            // Update explanation
            String explanationPattern = "<!-- wp:kadence/advancedheading \\{[^}]*\"uniqueID\":\"318_5d46e2-e6\"[^}]*\\}[\\s\\S]*?<!-- /wp:kadence/advancedheading -->";
            String newExplanation = "<!-- wp:kadence/advancedheading {" +
                    "\"uniqueID\":\"318_5d46e2-e6\"," +
                    "\"margin\":[-10,0,32,0],\"htmlTag\":\"p\"" +
                    "} -->\n" +
                    "<p class=\"kt-adv-heading318_5d46e2-e6 wp-block-kadence-advancedheading\" " +
                    "data-kb-block=\"kb-adv-heading318_5d46e2-e6\">" + explanation + "</p>\n" +
                    "<!-- /wp:kadence/advancedheading -->";
            
            String updatedContent = currentContent
                .replaceAll(valuePropPattern, newValueProp)
                .replaceAll(explanationPattern, newExplanation);
            
            JSONObject updatePayload = new JSONObject();
            updatePayload.put("content", new JSONObject().put("raw", updatedContent));
            
            HttpPost updateRequest = new HttpPost(URI.create(url));
            setAuthHeader(updateRequest);
            updateRequest.setEntity(new StringEntity(updatePayload.toString(), StandardCharsets.UTF_8));
            updateRequest.setHeader("Content-Type", "application/json");
            
            try (CloseableHttpResponse updateResponse = httpClient.execute(updateRequest)) {
                int statusCode = updateResponse.getCode();
                System.out.println("Update status: " + statusCode);
            }
        }
    }

    public void updateStoryAndMission(String story, String mission) throws IOException, ParseException {
        String url = WordPressConfig.BASE_URL + "pages/318?context=edit";
        HttpGet getRequest = new HttpGet(URI.create(url));
        setAuthHeader(getRequest);
        
        try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONObject page = new JSONObject(responseBody);
            
            JSONObject content = page.getJSONObject("content");
            String currentContent = content.getString("raw");
            
            // Update story paragraph
            String storyPattern = "<p class=\"\">story</p>";
            String newStory = "<p class=\"\">" + story + "</p>";
            
            // Update mission paragraph
            String missionPattern = "<p class=\"\">mission</p>";
            String newMission = "<p class=\"\">" + mission + "</p>";
            
            String updatedContent = currentContent
                .replace(storyPattern, newStory)
                .replace(missionPattern, newMission);
            
            JSONObject updatePayload = new JSONObject();
            updatePayload.put("content", new JSONObject().put("raw", updatedContent));
            
            HttpPost updateRequest = new HttpPost(URI.create(url));
            setAuthHeader(updateRequest);
            updateRequest.setEntity(new StringEntity(updatePayload.toString(), StandardCharsets.UTF_8));
            updateRequest.setHeader("Content-Type", "application/json");
            
            try (CloseableHttpResponse updateResponse = httpClient.execute(updateRequest)) {
                int statusCode = updateResponse.getCode();
                System.out.println("Update status: " + statusCode);
            }
        }
    }

    public void updateAllContent(BlogNiche niche) throws IOException, ParseException {
        AIContentGenerator generator = new AIContentGenerator();
        
        // Generate all content
        String valueProp = generator.generateValueProp(niche);
        String explanation = generator.generateSubheading(niche);  // reusing existing method
        String story = generator.generateStory(niche);
        String mission = generator.generateExpandedMission(niche);
        
        // Update all sections
        updateValuePropSection(valueProp, explanation);
        updateStoryAndMission(story, mission);
    }

    public void updateInfoBoxImage(String searchQuery) throws IOException, ParseException {
        // Get image from Unsplash
        UnsplashClient unsplashClient = new UnsplashClient();
        List<UnsplashImage> images = unsplashClient.searchImages(searchQuery);
        if (images.isEmpty()) {
            throw new IOException("No images found for query: " + searchQuery);
        }
        UnsplashImage selectedImage = images.get(0);  // Get first image
        
        // Update WordPress content
        String url = WordPressConfig.BASE_URL + "pages/318?context=edit";
        HttpGet getRequest = new HttpGet(URI.create(url));
        setAuthHeader(getRequest);
        
        try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONObject page = new JSONObject(responseBody);
            
            JSONObject content = page.getJSONObject("content");
            String currentContent = content.getString("raw");
            
            // Find and replace the infobox image block
            String infoboxPattern = "<!-- wp:kadence/infobox \\{[^}]*\"uniqueID\":\"318_1fa743-7a\"[^}]*\\}[\\s\\S]*?<!-- /wp:kadence/infobox -->";
            String newInfobox = "<!-- wp:kadence/infobox {" +
                    "\"uniqueID\":\"318_1fa743-7a\"," +
                    "\"containerBackground\":\"#f2f2f2\"," +
                    "\"containerBackgroundOpacity\":1," +
                    "\"containerHoverBackground\":\"#f2f2f2\"," +
                    "\"containerHoverBackgroundOpacity\":1," +
                    "\"containerPadding\":[0,0,0,0]," +
                    "\"mediaType\":\"image\"," +
                    "\"mediaImage\":[{" +
                    "\"url\":\"" + selectedImage.getUrl() + "\"," +
                    "\"alt\":\"" + selectedImage.getDescription() + "\"," +
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
                    "<div class=\"kt-info-box318_1fa743-7a wp-block-kadence-infobox\">" +
                    "<div class=\"kt-blocks-info-box-media-container\"><div class=\"kt-blocks-info-box-media\">" +
                    "<img src=\"" + selectedImage.getUrl() + "\" alt=\"" + selectedImage.getDescription() + "\"/>" +
                    "</div></div></div>\n" +
                    "<!-- /wp:kadence/infobox -->";
            
            String updatedContent = currentContent.replaceAll(infoboxPattern, newInfobox);
            
            // Create update payload
            JSONObject updatePayload = new JSONObject();
            updatePayload.put("content", new JSONObject().put("raw", updatedContent));
            
            // Send update
            HttpPost updateRequest = new HttpPost(URI.create(url));
            setAuthHeader(updateRequest);
            updateRequest.setEntity(new StringEntity(updatePayload.toString(), StandardCharsets.UTF_8));
            updateRequest.setHeader("Content-Type", "application/json");
            
            try (CloseableHttpResponse updateResponse = httpClient.execute(updateRequest)) {
                int statusCode = updateResponse.getCode();
                System.out.println("Update status: " + statusCode);
            }
        }
    }

    public void updateAllImages(BlogNiche niche) throws IOException, ParseException {
        AIContentGenerator generator = new AIContentGenerator();
        String searchTerm = generator.generateImageSearchTerm(niche);
        
        UnsplashClient unsplashClient = new UnsplashClient();
        List<UnsplashImage> images = unsplashClient.searchImages(searchTerm);
        if (images.size() < 6) {  // Now need 6 images (3 for pages + 3 for posts)
            throw new IOException("Not enough images found for query: " + searchTerm);
        }
        
        // Update page images
        updateInfoBoxImage(searchTerm);
        updateSimpleImage(609, images.get(1));
        updateBackgroundImage(609, "609_fc7adf-33", images.get(2));
        
        // Update post cover images
        int[] postIds = {23, 21, 1};
        for (int i = 0; i < postIds.length; i++) {
            updatePostCoverImage(postIds[i], images.get(i + 3));  // Use images 3, 4, and 5
        }
        
        System.out.println("All images updated successfully!");
    }
    
    private void updateSimpleImage(int pageId, UnsplashImage image) throws IOException, ParseException {
        String url = WordPressConfig.BASE_URL + "pages/" + pageId + "?context=edit";
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
            
            // Create update payload
            JSONObject updatePayload = new JSONObject();
            updatePayload.put("content", new JSONObject().put("raw", updatedContent));
            
            // Send update
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

    private void updateBackgroundImage(int pageId, String blockId, UnsplashImage image) throws IOException, ParseException {
        String url = WordPressConfig.BASE_URL + "pages/" + pageId + "?context=edit";
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
            
            // Create update payload
            JSONObject updatePayload = new JSONObject();
            updatePayload.put("content", new JSONObject().put("raw", updatedContent));
            
            // Send update
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

    private void updatePostCoverImage(int postId, UnsplashImage image) throws IOException, ParseException {
        String url = WordPressConfig.BASE_URL + "posts/" + postId + "?context=edit";
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
    
    private int uploadImage(UnsplashImage image) throws IOException, ParseException {
        String url = WordPressConfig.BASE_URL + "media";
        HttpPost uploadRequest = new HttpPost(URI.create(url));
        setAuthHeader(uploadRequest);
        
        // Download image data from Unsplash URL first
        byte[] imageData = downloadImage(image.getUrl());
        
        // Create multipart form data
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody(
            "file",
            imageData,
            ContentType.IMAGE_JPEG,
            "image.jpg"
        );
        
        HttpEntity multipart = builder.build();
        uploadRequest.setEntity(multipart);
        
        try (CloseableHttpResponse response = httpClient.execute(uploadRequest)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONObject mediaResponse = new JSONObject(responseBody);
            return mediaResponse.getInt("id");
        }
    }
    
    private byte[] downloadImage(String imageUrl) throws IOException {
        HttpGet request = new HttpGet(URI.create(imageUrl));
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            return EntityUtils.toByteArray(response.getEntity());
        }
    }

    public void updatePageSection(int pageId, String sectionId, String newContent) throws IOException, ParseException {
        String url = WordPressConfig.BASE_URL + "pages/" + pageId + "?context=edit";
        HttpGet getRequest = new HttpGet(URI.create(url));
        setAuthHeader(getRequest);
        
        try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONObject page = new JSONObject(responseBody);
            
            // Get the current content with all blocks
            JSONObject content = page.getJSONObject("content");
            String currentContent = content.getString("raw");
            
            // Debug print
            System.out.println("Current content before update: " + currentContent);
            
            // Find our target block and replace just that one
            String targetBlockPattern = "<!-- wp:group \\{\"layout\":\\{\"type\":\"flex\",\"orientation\":\"vertical\"\\}\\}[\\s\\S]*?<!-- /wp:group -->\\s*<!-- /wp:group -->";
            String updatedContent = currentContent.replaceFirst(targetBlockPattern, newContent);
            
            // Create update payload with all content
            JSONObject updatePayload = new JSONObject();
            updatePayload.put("content", new JSONObject().put("raw", updatedContent));
            
            // Send update
            HttpPost updateRequest = new HttpPost(URI.create(url));
            setAuthHeader(updateRequest);
            updateRequest.setEntity(new StringEntity(updatePayload.toString(), StandardCharsets.UTF_8));
            updateRequest.setHeader("Content-Type", "application/json");
            
            try (CloseableHttpResponse updateResponse = httpClient.execute(updateRequest)) {
                int statusCode = updateResponse.getCode();
                String updateResponseBody = EntityUtils.toString(updateResponse.getEntity());
                System.out.println("Update status: " + statusCode);
                System.out.println("Response: " + updateResponseBody);
            }
        }
    }

    public void createCategory(String title) throws IOException, ParseException {
        String url = WordPressConfig.BASE_URL + "categories";
        
        // Create JSON payload
        JSONObject categoryData = new JSONObject();
        categoryData.put("name", title);
        
        // Create POST request
        HttpPost request = new HttpPost(URI.create(url));
        setAuthHeader(request);
        request.setEntity(new StringEntity(categoryData.toString(), StandardCharsets.UTF_8));
        request.setHeader("Content-Type", "application/json");
        
        // Execute request
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getCode();
            String responseBody = EntityUtils.toString(response.getEntity());
            System.out.println("Category creation status: " + statusCode);
            System.out.println("Response: " + responseBody);
        }
    }

        public void clearCategories() throws IOException, ParseException {
        String url = WordPressConfig.BASE_URL + "categories";
        
        // First get all categories
        HttpGet getRequest = new HttpGet(URI.create(url));
        setAuthHeader(getRequest);
        
        try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONArray categories = new JSONArray(responseBody);
            
            // Delete each category (except uncategorized which is ID 1)
            for (int i = 0; i < categories.length(); i++) {
                JSONObject category = categories.getJSONObject(i);
                int id = category.getInt("id");
                if (id != 1) {  // Skip 'uncategorized'
                    deleteCategory(id);
                }
            }
        }
    }
    
    private void deleteCategory(int id) throws IOException {
        String url = WordPressConfig.BASE_URL + "categories/" + id + "?force=true";
        HttpDelete deleteRequest = new HttpDelete(URI.create(url));
        setAuthHeader(deleteRequest);
        
        try (CloseableHttpResponse response = httpClient.execute(deleteRequest)) {
            System.out.println("Deleted category ID: " + id);
        }
    }

    public List<String> getAllCategoryNames() throws IOException, ParseException {
        String url = WordPressConfig.BASE_URL + "categories";
        HttpGet getRequest = new HttpGet(URI.create(url));
        setAuthHeader(getRequest);
        
        List<String> categories = new ArrayList<>();
        try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONArray categoriesJson = new JSONArray(responseBody);
            
            for (int i = 0; i < categoriesJson.length(); i++) {
                JSONObject category = categoriesJson.getJSONObject(i);
                categories.add(category.getString("name"));
            }
        }
        return categories;
    }

    private int getCategoryId(String categoryName) throws IOException, ParseException {
        String url = WordPressConfig.BASE_URL + "categories?search=" + URLEncoder.encode(categoryName, "UTF-8");
        HttpGet getRequest = new HttpGet(URI.create(url));
        setAuthHeader(getRequest);
        
        try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONArray results = new JSONArray(responseBody);
            if (results.length() > 0) {
                return results.getJSONObject(0).getInt("id");
            }
        }
        return 1; // Default to uncategorized if not found
    }

    public static void main(String[] args) {
        try {
            WordPressUpdater updater = new WordPressUpdater();
            
            // Test updating the mission paragraph
            updater.updateInfoBoxImage("professional camera");
            System.out.println("Image updated successfully!");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}