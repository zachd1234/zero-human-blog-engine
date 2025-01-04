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
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpRequest;
import java.util.List;

public class WordPressBlockClient extends BaseWordPressClient {
    public enum BlockType {
        KADENCE_HEADING,
        SIMPLE_PARAGRAPH,
        GROUP_BLOCK
    }

    public WordPressBlockClient() {
        super();
    }

    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setAuthHeaderPublic(HttpRequest request) {
        setAuthHeader(request);
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
            
            String blockPattern = "<!-- wp:kadence/advancedheading \\{[^}]*\"uniqueID\":\"" + uniqueId + "\"[^}]*\\}[\\s\\S]*?<!-- /wp:kadence/advancedheading -->";
            
            JSONObject blockProps = new JSONObject().put("uniqueID", uniqueId);
            if (properties != null) {
                for (String key : properties.keySet()) {
                    blockProps.put(key, properties.get(key));
                }
            }
            
            String newBlock = "<!-- wp:kadence/advancedheading " + blockProps.toString() + " -->\n" +
                            "<" + (properties.optString("htmlTag", "h1")) + " class=\"kt-adv-heading" + uniqueId + 
                            " wp-block-kadence-advancedheading\" data-kb-block=\"kb-adv-heading" + uniqueId + 
                            "\">" + content + "</" + (properties.optString("htmlTag", "h1")) + ">\n" +
                            "<!-- /wp:kadence/advancedheading -->";
            
            String updatedContent = currentContent.replaceAll(blockPattern, newBlock);
            
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

    public void updateHeadingAndSubheading(String heading, String subheading) throws IOException, ParseException {
        String url = baseUrl + "pages/609?context=edit";
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
                    "\"align\":\"center\"," +
                    "\"color\":\"#ffffff\"," +
                    "\"htmlTag\":\"p\"" +
                    "} -->\n" +
                    "<p class=\"kt-adv-heading609_e56131-4a wp-block-kadence-advancedheading\" " +
                    "data-kb-block=\"kb-adv-heading609_e56131-4a\">" + subheading + "</p>\n" +
                    "<!-- /wp:kadence/advancedheading -->";
            
            // Replace both blocks
            System.out.println("\nCurrent content: " + currentContent);
            System.out.println("\nSubheading pattern: " + subheadingPattern);
            System.out.println("\nNew subheading: " + newSubheading);
            
            // First update heading
            String afterHeading = currentContent.replaceAll(headingPattern, newHeading);
            System.out.println("\nAfter heading update: " + afterHeading);
            
            // Then update subheading
            String updatedContent = afterHeading.replaceAll(subheadingPattern, newSubheading);
            System.out.println("\nAfter subheading update: " + updatedContent);
            
            // Check if subheading was actually replaced
            if (afterHeading.equals(updatedContent)) {
                System.out.println("\nWARNING: Subheading was not replaced!");
            }
            
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

    public String getPageContent(int pageId) throws IOException, ParseException {
        String url = baseUrl + "pages/" + pageId + "?context=edit";
        HttpGet getRequest = new HttpGet(URI.create(url));
        setAuthHeader(getRequest);
        
        try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONObject page = new JSONObject(responseBody);
            return page.getJSONObject("content").getString("raw");
        }
    }

    public void updatePageContent(int pageId, String content) throws IOException, ParseException {
        String url = baseUrl + "pages/" + pageId + "?context=edit";
        JSONObject updatePayload = new JSONObject()
            .put("content", new JSONObject().put("raw", content));
        
        HttpPost updateRequest = new HttpPost(URI.create(url));
        setAuthHeader(updateRequest);
        updateRequest.setEntity(new StringEntity(updatePayload.toString(), StandardCharsets.UTF_8));
        updateRequest.setHeader("Content-Type", "application/json");
        
        try (CloseableHttpResponse updateResponse = httpClient.execute(updateRequest)) {
            int statusCode = updateResponse.getCode();
            System.out.println("Update status: " + statusCode);
        }
    }

    public void updateTopicsSection(List<Topic> topics) throws IOException, ParseException {
        StringBuilder html = new StringBuilder();
        
        // Start main container
        html.append("<!-- wp:group {\"metadata\":{\"name\":\"Features\"},\"className\":\"nfd-container nfd-p-lg nfd-theme-darker nfd-bg-surface nfd-wb-features__features-4 is-style-nfd-theme-darker\",\"layout\":{\"type\":\"constrained\"}} -->\n");
        html.append("<div class=\"nfd-container nfd-p-lg nfd-theme-darker nfd-bg-surface nfd-wb-features__features-4 is-style-nfd-theme-darker wp-block-group nfd-bg-effect-dots\">\n");
        
        // Add heading
        html.append("<!-- wp:heading {\"textColor\":\"theme-palette9\"} -->\n");
        html.append("<h2 class=\"wp-block-heading has-theme-palette-9-color has-text-color\">Explore Topics</h2>\n");
        html.append("<!-- /wp:heading -->\n");
        
        // Start topics container
        html.append("<!-- wp:group {\"layout\":{\"type\":\"flex\",\"orientation\":\"vertical\"}} -->\n");
        html.append("<div class=\"wp-block-group\">\n");
        
        // Create rows of 3 topics each
        for (int i = 0; i < topics.size(); i += 3) {
            html.append("<!-- wp:columns {\"className\":\"nfd-text-base nfd-gap-md nfd-gap-y-xl\"} -->\n");
            html.append("<div class=\"nfd-text-base nfd-gap-md nfd-gap-y-xl wp-block-columns\">\n");
            
            // Add up to 3 topics in this row
            for (int j = i; j < Math.min(i + 3, topics.size()); j++) {
                Topic topic = topics.get(j);
                html.append(createTopicCard(topic, j));
            }
            
            html.append("</div>\n<!-- /wp:columns -->\n");
        }
        
        // Close containers
        html.append("</div>\n<!-- /wp:group -->\n");
        html.append("</div>\n<!-- /wp:group -->\n");
        
        // Update the page content
        updatePageContent(609, html.toString());  // Assuming 609 is your homepage ID
    }
    
    private String createTopicCard(Topic topic, int index) {
        StringBuilder card = new StringBuilder();
        
        // Start column
        card.append("<!-- wp:column -->\n<div class=\"wp-block-column\">");
        
        // Start card container
        String cardTheme = (index % 2 == 0) ? " nfd-theme-dark nfd-bg-surface is-style-nfd-theme-dark" : "";
        String borderStyle = (index % 2 == 0) ? "style=\"border-width:1px\"" : "";
        
        card.append("<!-- wp:group {\"metadata\":{\"name\":\"Card\"},\"className\":\"nfd-shadow-xs nfd-p-card-square nfd-rounded nfd-gap-md" + cardTheme + "\"," + borderStyle + ",\"layout\":{\"type\":\"flex\",\"orientation\":\"vertical\",\"justifyContent\":\"left\"}} -->\n");
        card.append("<div class=\"nfd-shadow-xs nfd-p-card-square nfd-rounded nfd-gap-md" + cardTheme + " wp-block-group\" " + borderStyle + ">");
        
        // Add title
        card.append("<!-- wp:heading {\"level\":3,\"className\":\"nfd-text-md nfd-text-contrast\"} -->\n");
        card.append("<h3 class=\"nfd-text-md nfd-text-contrast wp-block-heading\">" + topic.getTitle() + "</h3>\n");
        card.append("<!-- /wp:heading -->\n");
        
        // Add description
        card.append("<!-- wp:paragraph {\"className\":\"nfd-text-balance nfd-text-faded\"} -->\n");
        card.append("<p class=\"nfd-text-balance nfd-text-faded\">" + topic.getDescription() + "</p>\n");
        card.append("<!-- /wp:paragraph -->\n");
        
        // Add button
        card.append("<!-- wp:buttons -->\n<div class=\"wp-block-buttons\"><!-- wp:button -->\n");
        card.append("<div class=\"wp-block-button\"><a class=\"wp-block-button__link wp-element-button\" href=\"" + topic.getLink() + "\">Explore " + topic.getTitle().toLowerCase() + "</a></div>\n");
        card.append("<!-- /wp:button --></div>\n<!-- /wp:buttons -->\n");
        
        // Close containers
        card.append("</div>\n<!-- /wp:group -->\n");
        card.append("</div>\n<!-- /wp:column -->\n");
        
        return card.toString();
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