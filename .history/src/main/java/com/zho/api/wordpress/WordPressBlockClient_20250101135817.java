package com.zho.api.wordpress;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.ParseException;
import org.json.JSONObject;

public class WordPressBlockClient extends BaseWordPressClient {
    public enum BlockType {
        KADENCE_HEADING,
        SIMPLE_PARAGRAPH,
        GROUP_BLOCK
    }

    public WordPressBlockClient() {
        super();
    }

    public void updateBlock(int pageId, BlockType type, String uniqueId, String content, JSONObject properties) 
            throws IOException, ParseException {
        switch (type) {
            case KADENCE_HEADING:
                updateKadenceHeading(pageId, uniqueId, content, properties);
                break;
            case SIMPLE_PARAGRAPH:
                updateSimpleParagraph(pageId, content, properties != null ? properties.optString("className") : null);
                break;
            case GROUP_BLOCK:
                updateGroupBlock(pageId, content);
                break;
            default:
                throw new IllegalArgumentException("Unsupported block type: " + type);
        }
    }

    private void updateKadenceHeading(int pageId, String uniqueId, String content, JSONObject properties) 
            throws IOException, ParseException {
        String url = baseUrl + "pages/" + pageId + "?context=edit";
        HttpGet getRequest = new HttpGet(URI.create(url));
        setAuthHeader(getRequest);
        
        try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONObject page = new JSONObject(responseBody);
            
            JSONObject contentObj = page.getJSONObject("content");
            String currentContent = contentObj.getString("raw");
            
            // More specific pattern that matches the heading within the column structure
            String blockPattern = "<h[1-6] class=\"kt-adv-heading" + uniqueId + "[^>]*>.*?</h[1-6]>|" +
                                "<p class=\"kt-adv-heading" + uniqueId + "[^>]*>.*?</p>";
            
            // Preserve all properties from the original block
            if (currentContent.contains("\"uniqueID\":\"" + uniqueId + "\"")) {
                String fullBlockPattern = "<!-- wp:kadence/advancedheading \\{[^}]*\"uniqueID\":\"" + uniqueId + "\"[^}]*\\}[\\s\\S]*?<!-- /wp:kadence/advancedheading -->";
                java.util.regex.Matcher matcher = Pattern.compile(fullBlockPattern).matcher(currentContent);
                if (matcher.find()) {
                    String existingBlock = matcher.group(0);
                    String propsJson = existingBlock.substring(
                        existingBlock.indexOf("{"),
                        existingBlock.indexOf("}") + 1
                    );
                    JSONObject existingProps = new JSONObject(propsJson);
                    // Merge existing properties with new ones
                    for (String key : JSONObject.getNames(existingProps)) {
                        if (!properties.has(key)) {
                            properties.put(key, existingProps.get(key));
                        }
                    }
                }
            }
            
            String newBlock = "<!-- wp:kadence/advancedheading " + properties.toString() + " -->\n" +
                            "<" + (properties.optString("htmlTag", "h1")) + " class=\"kt-adv-heading" + uniqueId + 
                            " wp-block-kadence-advancedheading\" data-kb-block=\"kb-adv-heading" + uniqueId + 
                            "\">" + content + "</" + (properties.optString("htmlTag", "h1")) + ">\n" +
                            "<!-- /wp:kadence/advancedheading -->";
            
            String updatedContent = currentContent.replaceAll(blockPattern, 
                "<" + (properties.optString("htmlTag", "h1")) + " class=\"kt-adv-heading" + uniqueId + 
                " wp-block-kadence-advancedheading\" data-kb-block=\"kb-adv-heading" + uniqueId + 
                "\">" + content + "</" + (properties.optString("htmlTag", "h1")) + ">");
            
            JSONObject updatePayload = new JSONObject();
            updatePayload.put("content", new JSONObject().put("raw", updatedContent));
            
            HttpPost updateRequest = new HttpPost(URI.create(url));
            setAuthHeader(updateRequest);
            updateRequest.setEntity(new StringEntity(updatePayload.toString(), StandardCharsets.UTF_8));
            updateRequest.setHeader("Content-Type", "application/json");
            
            try (CloseableHttpResponse updateResponse = httpClient.execute(updateRequest)) {
                int statusCode = updateResponse.getCode();
                String updateResponseBody = EntityUtils.toString(updateResponse.getEntity());
                System.out.println("Update status: " + statusCode);
                if (statusCode >= 300) {
                    System.out.println("Error response: " + updateResponseBody);
                }
            }
        }
    }
    
    private void updateSimpleParagraph(int pageId, String content, String className) 
            throws IOException, ParseException {
        String url = baseUrl + "pages/" + pageId + "?context=edit";
        HttpGet getRequest = new HttpGet(URI.create(url));
        
        try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONObject page = new JSONObject(responseBody);
            
            JSONObject contentObj = page.getJSONObject("content");
            String currentContent = contentObj.getString("raw");
            
            String pattern = "<p class=\"" + (className == null ? "" : className) + "\">.*?</p>";
            String newContent = "<p class=\"" + (className == null ? "" : className) + "\">" + content + "</p>";
            
            String updatedContent = currentContent.replaceAll(pattern, newContent);
            
            JSONObject updatePayload = new JSONObject();
            updatePayload.put("content", new JSONObject().put("raw", updatedContent));
            
            HttpPost updateRequest = new HttpPost(URI.create(url));
            updateRequest.setEntity(new StringEntity(updatePayload.toString(), StandardCharsets.UTF_8));
            updateRequest.setHeader("Content-Type", "application/json");
            
            try (CloseableHttpResponse updateResponse = httpClient.execute(updateRequest)) {
                int statusCode = updateResponse.getCode();
                String updateResponseBody = EntityUtils.toString(updateResponse.getEntity());
                System.out.println("Update status: " + statusCode);
                //System.out.println("Response: " + updateResponseBody);
            }
        }
    }
    
    private void updateGroupBlock(int pageId, String content) throws IOException, ParseException {
        String url = baseUrl + "pages/" + pageId + "?context=edit";
        HttpGet getRequest = new HttpGet(URI.create(url));
        setAuthHeader(getRequest);
        
        try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONObject page = new JSONObject(responseBody);
            
            String currentContent = page.getJSONObject("content").getString("raw");
            
            System.out.println("Current content before update: " + currentContent);
            
            String pattern = Pattern.quote("<!-- wp:group {\"layout\":{\"type\":\"flex\",\"orientation\":\"vertical\",\"justifyContent\":\"left\"}} -->") + 
                           ".*?" + 
                           Pattern.quote("<!-- /wp:group -->");
            
            String updatedContent = currentContent.replaceFirst(pattern, content);
            
            JSONObject updatePayload = new JSONObject();
            updatePayload.put("content", new JSONObject().put("raw", updatedContent));
            
            HttpPost updateRequest = new HttpPost(URI.create(url));
            setAuthHeader(updateRequest);
            updateRequest.setEntity(new StringEntity(updatePayload.toString(), StandardCharsets.UTF_8));
            updateRequest.setHeader("Content-Type", "application/json");
            
            try (CloseableHttpResponse updateResponse = httpClient.execute(updateRequest)) {
                int statusCode = updateResponse.getCode();
                String updateResponseBody = EntityUtils.toString(updateResponse.getEntity());
                System.out.println("Update status: " + statusCode);
                //System.out.println("Response: " + updateResponseBody);
            }
        }
    }


    public void updatePageSection(int pageId, String sectionId, String newContent) throws IOException, ParseException {
        String url = baseUrl + "pages/" + pageId + "?context=edit";
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

    public static void main(String[] args) {
        try {
            WordPressBlockClient client = new WordPressBlockClient();
            
            // Test 1: Update mission paragraph (uniqueID: 609_29304f-69)
            JSONObject missionProps = new JSONObject()
                .put("uniqueID", "609_29304f-69")
                .put("markBorder", "")
                .put("htmlTag", "p");
            client.updateBlock(609, BlockType.KADENCE_HEADING, "609_29304f-69", 
                "Test mission statement update", missionProps);
            
            // Test 2: Update main heading (uniqueID: 609_a8d80a-ca)
            JSONObject headingProps = new JSONObject()
                .put("uniqueID", "609_a8d80a-ca")
                .put("level", 1)
                .put("align", "center")
                .put("color", "#ffffff")
                .put("typography", "Jost")
                .put("googleFont", true)
                .put("fontSubset", "latin")
                .put("fontVariant", "700")
                .put("fontWeight", "700")
                .put("textTransform", "none")
                .put("fontSize", new int[]{60, 0, 40})
                .put("fontHeight", new int[]{68, 0, 48})
                .put("fontHeightType", "px");
            client.updateBlock(609, BlockType.KADENCE_HEADING, "609_a8d80a-ca", 
                "Test Main Heading Update", headingProps);
            
            // Test 3: Update topics section using TopicConfig format
            String[] testTopics = {"Topic 1", "Topic 2", "Topic 3"};
            StringBuilder topicsContent = new StringBuilder();
            topicsContent.append("<!-- wp:group {\"layout\":{\"type\":\"flex\",\"orientation\":\"vertical\"}} -->\n");
            topicsContent.append("<div class=\"wp-block-group\">\n");
            
            for (String topic : testTopics) {
                topicsContent.append("<!-- wp:paragraph -->\n");
                topicsContent.append("<p>").append(topic).append("</p>\n");
                topicsContent.append("<!-- /wp:paragraph -->\n");
            }
            
            topicsContent.append("</div>\n");
            topicsContent.append("<!-- /wp:group -->");
            
            client.updateBlock(609, BlockType.GROUP_BLOCK, null, topicsContent.toString(), null);
            
            System.out.println("All test updates completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Error during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 