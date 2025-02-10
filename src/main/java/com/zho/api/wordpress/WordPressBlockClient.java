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
import com.zho.model.Topic;
import com.zho.services.DatabaseService;

import java.util.List;
import org.json.JSONArray;
import java.net.URLEncoder;

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
            JSONObject content = page.getJSONObject("content");
            String currentContent = content.getString("raw");
            
            // Wrap the new content with the proper group metadata and heading
            String wrappedContent = 
                "<!-- wp:group {\"metadata\":{\"name\":\"Features\"},\"className\":\"nfd-container nfd-p-lg nfd-theme-darker nfd-bg-surface nfd-wb-features__features-4 is-style-nfd-theme-darker\",\"layout\":{\"type\":\"constrained\"},\"nfdGroupTheme\":\"\",\"nfdGroupEffect\":\"dots\"} -->\n" +
                "<div class=\"nfd-container nfd-p-lg nfd-theme-darker nfd-bg-surface nfd-wb-features__features-4 is-style-nfd-theme-darker wp-block-group nfd-bg-effect-dots\">" +
                "<!-- wp:heading {\"style\":{\"elements\":{\"link\":{\"color\":{\"text\":\"var:preset|color|theme-palette9\"}}}},\"textColor\":\"theme-palette9\"} -->\n" +
                "<h2 class=\"wp-block-heading has-theme-palette-9-color has-text-color has-link-color\">Explore Topics</h2>\n" +
                "<!-- /wp:heading -->\n" +
                "<!-- wp:group {\"layout\":{\"type\":\"flex\",\"orientation\":\"vertical\"}} -->\n" +
                "<div class=\"wp-block-group\">" +
                newContent +
                "</div>\n<!-- /wp:group --></div>\n<!-- /wp:group -->";

            String targetPattern = "<!-- wp:group \\{\"metadata\":\\{\"name\":\"Features\"\\}[^}]*\\}[\\s\\S]*?<!-- /wp:group -->(?=\\s*<!-- wp:group)";
            
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(targetPattern);
            java.util.regex.Matcher matcher = pattern.matcher(currentContent);
            
            if (matcher.find()) {
                String updatedContent = currentContent.substring(0, matcher.start()) + 
                                      wrappedContent + 
                                      currentContent.substring(matcher.end());
                
                JSONObject updatePayload = new JSONObject();
                updatePayload.put("content", new JSONObject().put("raw", updatedContent));
                
                HttpPost updateRequest = new HttpPost(URI.create(url));
                setAuthHeader(updateRequest);
                updateRequest.setEntity(new StringEntity(updatePayload.toString(), StandardCharsets.UTF_8));
                updateRequest.setHeader("Content-Type", "application/json");
                
                try (CloseableHttpResponse updateResponse = httpClient.execute(updateRequest)) {
                    System.out.println("Update status: " + updateResponse.getCode());
                }
            } else {
                System.out.println("Could not find Features section");
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
        StringBuilder content = new StringBuilder();
        
        // Start columns container
        content.append("<!-- wp:columns {\"className\":\"nfd-text-base nfd-gap-md nfd-gap-y-xl\"} -->\n")
               .append("<div class=\"nfd-text-base nfd-gap-md nfd-gap-y-xl wp-block-columns\">");

        // Add topics (3 per row)
        for (int i = 0; i < topics.size(); i++) {
            // Debug print
            System.out.println("Processing topic " + (i+1) + " of " + topics.size());
            
            if (i % 3 == 0 && i > 0) {
                System.out.println("Starting new row");
                content.append("</div><!-- /wp:columns -->\n")
                       .append("<!-- wp:columns {\"className\":\"nfd-text-base nfd-gap-md nfd-gap-y-xl\"} -->\n")
                       .append("<div class=\"nfd-text-base nfd-gap-md nfd-gap-y-xl wp-block-columns\">");
            }
            content.append(createTopicCard(topics.get(i), (i % 2 == 1)));
        }

        // Close columns container
        content.append("</div><!-- /wp:columns -->");

        // Update WordPress
        updatePageSection(609, "topics-section", content.toString());
    }

    private String createTopicCard(Topic topic, boolean isDark) {
        String themeClass = isDark ? " nfd-theme-dark nfd-bg-surface is-style-nfd-theme-dark" : "";
        String borderStyle = isDark ? " style=\"border-width:1px\"" : "";

        return String.format(
            "<!-- wp:column --><div class=\"wp-block-column\">" +
            "<!-- wp:group {\"className\":\"nfd-shadow-xs nfd-p-card-square nfd-rounded nfd-gap-md%s\"%s} -->" +
            "<div class=\"nfd-shadow-xs nfd-p-card-square nfd-rounded nfd-gap-md%s wp-block-group\"%s>" +
            "<!-- wp:heading {\"level\":3,\"className\":\"nfd-text-md nfd-text-contrast\"} -->" +
            "<h3 class=\"nfd-text-md nfd-text-contrast wp-block-heading\">%s</h3>" +
            "<!-- /wp:heading -->" +
            "<!-- wp:paragraph {\"className\":\"nfd-text-balance nfd-text-faded\"} -->" +
            "<p class=\"nfd-text-balance nfd-text-faded\">%s</p>" +
            "<!-- /wp:paragraph -->" +
            "<!-- wp:buttons --><div class=\"wp-block-buttons\">" +
            "<!-- wp:button --><div class=\"wp-block-button\">" +
            "<a class=\"wp-block-button__link wp-element-button\" href=\"%s\">Explore %s</a>" +
            "</div><!-- /wp:button --></div><!-- /wp:buttons -->" +
            "</div><!-- /wp:group --></div><!-- /wp:column -->",
            themeClass, borderStyle, themeClass, borderStyle,
            topic.getTitle(), topic.getDescription(), topic.getLink(), topic.getTitle().toLowerCase()
        );
    }

    public void updateParagraphText(int pageId, String uniqueId, String text) throws IOException, ParseException {
        String currentContent = getPageContent(pageId);
        
        // Pattern to extract the existing className
        String classPattern = "<!-- wp:paragraph \\{[^}]*\"className\":\"([^\"]+)\"[^}]*\"uniqueID\":\"" + uniqueId + "\"[^}]*\\}";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(classPattern);
        java.util.regex.Matcher matcher = pattern.matcher(currentContent);
        
        // Get existing className or use empty if none exists
        String className = matcher.find() ? matcher.group(1) : "";
        
        // Create the replacement block with original className if it existed
        String classAttribute = className.isEmpty() ? "" : "\"className\":\"" + className + "\",";
        String newBlock = String.format(
            "<!-- wp:paragraph {%s\"uniqueID\":\"%s\"} -->\n" +
            "<p%s>%s</p>\n" +
            "<!-- /wp:paragraph -->",
            classAttribute,
            uniqueId,
            className.isEmpty() ? "" : String.format(" class=\"%s\"", className),
            text
        );
        
        // Replace the old block including the uniqueID
        String blockPattern = String.format(
            "<!-- wp:paragraph \\{[^}]*\"uniqueID\":\"%s\"[^}]*\\}[\\s\\S]*?<!-- /wp:paragraph -->",
            uniqueId
        );
        String updatedContent = currentContent.replaceFirst(blockPattern, newBlock);
        
        updatePageContent(pageId, updatedContent);
    }
    
    public void updateHeadingText(int pageId, String uniqueId, String text) throws IOException, ParseException {
        String currentContent = getPageContent(pageId);
        
        // Pattern to extract the existing attributes
        String classPattern = "<!-- wp:heading \\{[^}]*\"textAlign\":\"([^\"]+)\"[^}]*\"uniqueID\":\"" + uniqueId + "\"[^}]*\\}";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(classPattern);
        java.util.regex.Matcher matcher = pattern.matcher(currentContent);
        
        // Get existing textAlign or default to center
        String textAlign = matcher.find() ? matcher.group(1) : "center";
        
        // Create the replacement block preserving attributes
        String newBlock = String.format(
            "<!-- wp:heading {\"textAlign\":\"%s\",\"uniqueID\":\"%s\"} -->\n" +
            "<h2 class=\"wp-block-heading has-text-align-%s\">%s</h2>\n" +
            "<!-- /wp:heading -->",
            textAlign,
            uniqueId,
            textAlign,
            text
        );
        
        // Replace the old block including the uniqueID
        String blockPattern = String.format(
            "<!-- wp:heading \\{[^}]*\"uniqueID\":\"%s\"[^}]*\\}[\\s\\S]*?<!-- /wp:heading -->",
            uniqueId
        );
        String updatedContent = currentContent.replaceFirst(blockPattern, newBlock);
        
        updatePageContent(pageId, updatedContent);
    }

    public void updateAdvancedHeadingText(int pageId, String uniqueId, String text) throws IOException, ParseException {
        String currentContent = getPageContent(pageId);
        
        // Pattern to extract the existing attributes
        String attrPattern = "<!-- wp:kadence/advancedheading \\{([^}]*)\"uniqueID\":\"" + uniqueId + "\"([^}]*)\\}";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(attrPattern);
        java.util.regex.Matcher matcher = pattern.matcher(currentContent);
        
        // Create the replacement block preserving all attributes
        String newBlock = String.format(
            "<!-- wp:kadence/advancedheading {%s\"uniqueID\":\"%s\"%s} -->\n" +
            "<h1 class=\"kt-adv-heading%s wp-block-kadence-advancedheading\" data-kb-block=\"kb-adv-heading%s\">%s</h1>\n" +
            "<!-- /wp:kadence/advancedheading -->",
            matcher.find() ? matcher.group(1) : "",  // Preserve prefix attributes
            uniqueId,
            matcher.group(2),                        // Preserve suffix attributes
            uniqueId,
            uniqueId,
            text
        );
        
        // Replace the old block
        String blockPattern = String.format(
            "<!-- wp:kadence/advancedheading \\{[^}]*\"uniqueID\":\"%s\"[^}]*\\}[\\s\\S]*?<!-- /wp:kadence/advancedheading -->",
            uniqueId
        );
        String updatedContent = currentContent.replaceFirst(blockPattern, newBlock);
        
        updatePageContent(pageId, updatedContent);
    }

    public void updateAccordionFAQ(int faqId, String content) throws IOException {
        String url = baseUrl + "sp_accordion_faqs/" + faqId;
        System.out.println("DEBUG: Attempting to update FAQ at URL: " + url);
        
        // First verify the FAQ exists
        HttpGet getRequest = new HttpGet(URI.create(url));
        setAuthHeader(getRequest);
        try (CloseableHttpResponse getResponse = httpClient.execute(getRequest)) {
            if (getResponse.getCode() == 404) {
                throw new IOException("FAQ with ID " + faqId + " not found. Please verify the ID is correct.");
            }
        }
        
        // Continue with update if FAQ exists
        JSONObject updatePayload = new JSONObject()
            .put("content", new JSONObject()
                .put("raw", content));
        
        HttpPost updateRequest = new HttpPost(URI.create(url));
        setAuthHeader(updateRequest);
        updateRequest.setEntity(new StringEntity(updatePayload.toString(), StandardCharsets.UTF_8));
        updateRequest.setHeader("Content-Type", "application/json");

        try (CloseableHttpResponse response = httpClient.execute(updateRequest)) {
            int statusCode = response.getCode();
            if (statusCode == 200) {
                System.out.println("Successfully updated FAQ accordion");
            } else {
                throw new IOException("Failed to update FAQ accordion. Status code: " + statusCode);
            }
        }
    }
    
    public Integer getFirstAccordionFAQId() throws IOException, ParseException {
        String url = baseUrl + "sp_accordion_faqs";
        
        HttpGet getRequest = new HttpGet(URI.create(url));
        setAuthHeader(getRequest);
        
        try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONArray faqs = new JSONArray(responseBody);
            
            if (faqs.length() > 0) {
                return faqs.getJSONObject(0).getInt("id");
            }
            
            throw new IOException("No FAQ found");
        }
    }

    public JSONObject getPageByUrl(String pageUrl) throws IOException, ParseException {
        // Remove domain from URL if present
        String slug = pageUrl.replaceFirst("https?://[^/]+/", "")
                            .replaceAll("/$", ""); // Remove trailing slash
        
        String url = baseUrl + "pages?slug=" + URLEncoder.encode(slug, StandardCharsets.UTF_8);
        
        HttpGet getRequest = new HttpGet(URI.create(url));
        setAuthHeader(getRequest);
        
        try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONArray pages = new JSONArray(responseBody);
            
            if (pages.length() > 0) {
                return pages.getJSONObject(0);
            }
            
            throw new IOException("Page not found: " + pageUrl);
        }
    }

    public String getSiteSlogan() throws IOException, ParseException {
        String url = baseUrl.replaceFirst("wp/v2/", ""); // Remove wp/v2/ to access root endpoint
        
        HttpGet getRequest = new HttpGet(URI.create(url));
        setAuthHeader(getRequest);
        
        try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONObject siteInfo = new JSONObject(responseBody);
            return siteInfo.getString("description");  // WordPress stores the tagline as "description"
        }
    }

    public int getAuthorBlockId() throws IOException, ParseException {
        String url = baseUrl + "blocks?search=author";  // Remove wp/v2/ as it's already in baseUrl
        
        HttpGet getRequest = new HttpGet(URI.create(url));
        setAuthHeader(getRequest);
        
        try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            System.out.println("Response body: " + responseBody); // Debug line
            
            JSONArray blocks = new JSONArray(responseBody);
            
            for (int i = 0; i < blocks.length(); i++) {
                JSONObject block = blocks.getJSONObject(i);
                String title = block.getJSONObject("title").getString("raw").toLowerCase();
                if (title.contains("author")) {
                    return block.getInt("id");
                }
            }
            
            throw new IOException("Author block not found. Please ensure a reusable block with 'author' in the title exists.");
        }
    }

    public void updateAuthorBlock(String authorName, String jobTitle, String authorBio, String imageUrl) throws IOException, ParseException {
        int blockId = getAuthorBlockId();
        String url = baseUrl + "blocks/" + blockId;
        
        String siteTitle = getSiteTitle();
        String siteUrl = baseUrl.replaceAll("wp-json/wp/v2/$", "");
        
        // Generate the author URL using the first name in lowercase
        String authorUrl = siteUrl + "author/" + authorName.split(" ")[0].toLowerCase();
        
        String content = String.format(
            "<!-- wp:kadence/rowlayout {\"uniqueID\":\"1458_35c24b-2c\",\"columns\":1,\"colLayout\":\"equal\",\"kbVersion\":2} -->\n" +
            "<!-- wp:kadence/column {\"uniqueID\":\"1458_21c883-b1\",\"kbVersion\":2} -->\n" +
            "<div class=\"wp-block-kadence-column kadence-column1458_21c883-b1\"><div class=\"kt-inside-inner-col\">" +
            "<!-- wp:kadence/rowlayout {\"uniqueID\":\"1458_fdce15-66\",\"colLayout\":\"equal\",\"kbVersion\":2} -->\n" +
            "<!-- wp:kadence/column {\"uniqueID\":\"1458_d49288-3e\",\"kbVersion\":2} -->\n" +
            "<div class=\"wp-block-kadence-column kadence-column1458_d49288-3e\"><div class=\"kt-inside-inner-col\">" +
            "<!-- wp:image {\"sizeSlug\":\"large\"} -->\n" +
            "<figure class=\"wp-block-image size-large\"><img src=\"%s\" alt=\"\"/></figure>\n" +
            "<!-- /wp:image --></div></div>\n" +
            "<!-- /wp:kadence/column -->\n\n" +
            "<!-- wp:kadence/column {\"uniqueID\":\"1458_050b65-3a\",\"kbVersion\":2} -->\n" +
            "<div class=\"wp-block-kadence-column kadence-column1458_050b65-3a\"><div class=\"kt-inside-inner-col\">" +
            "<!-- wp:heading {\"level\":4} -->\n" +
            "<h4 class=\"wp-block-heading\">Written By:</h4>\n" +
            "<!-- /wp:heading -->\n\n" +
            "<!-- wp:heading {\"level\":3} -->\n" +
            "<h3 class=\"wp-block-heading\">%s</h3>\n" +
            "<!-- /wp:heading -->\n\n" +
            "<!-- wp:paragraph -->\n" +
            "<p class=\"\">%s</p>\n" +
            "<!-- /wp:paragraph -->\n\n" +
            "<!-- wp:paragraph -->\n" +
            "<p class=\"\">%s</p>\n" +
            "<!-- /wp:paragraph -->\n\n" +
            "<!-- wp:kadence/advancedbtn {\"hAlign\":\"left\",\"uniqueID\":\"1458_ceca8d-9a\"} -->\n" +
            "<div class=\"wp-block-kadence-advancedbtn kb-buttons-wrap kb-btns1458_ceca8d-9a\">" +
            "<!-- wp:kadence/singlebtn {\"uniqueID\":\"1458_24904e-a5\",\"text\":\"More About the Author\",\"link\":\"%s\"} /--></div>\n" +
            "<!-- /wp:kadence/advancedbtn --></div></div>\n" +
            "<!-- /wp:kadence/column -->\n" +
            "<!-- /wp:kadence/rowlayout --></div></div>\n" +
            "<!-- /wp:kadence/column -->\n" +
            "<!-- /wp:kadence/rowlayout -->",
            imageUrl, authorName, jobTitle, authorBio, authorUrl
        );

        JSONObject updatePayload = new JSONObject()
            .put("content", new JSONObject().put("raw", content));
        
        HttpPost updateRequest = new HttpPost(URI.create(url));
        setAuthHeader(updateRequest);
        updateRequest.setEntity(new StringEntity(updatePayload.toString(), StandardCharsets.UTF_8));
        updateRequest.setHeader("Content-Type", "application/json");
        
        try (CloseableHttpResponse response = httpClient.execute(updateRequest)) {
            if (response.getCode() != 200) {
                throw new IOException("Failed to update author block. Status: " + response.getCode());
            }
        }
    }

    public String getSiteTitle() throws IOException, ParseException {
        String url = baseUrl.replaceFirst("wp/v2/", ""); // Remove wp/v2/ to access root endpoint
        
        HttpGet getRequest = new HttpGet(URI.create(url));
        setAuthHeader(getRequest);
        
        try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONObject siteInfo = new JSONObject(responseBody);
            return siteInfo.getString("name");
        }
    }


    public static void main(String[] args) {
        try {
            WordPressBlockClient client = new WordPressBlockClient();
            System.out.println(client.getSiteSlogan());
        } catch (Exception e) {
            System.err.println("Error during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 