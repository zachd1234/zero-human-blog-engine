package com.zho.api.wordpress;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URI;
import com.zho.model.Image;
import java.nio.charset.StandardCharsets;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import java.util.Arrays;

public class WordPressSettingsClient extends BaseWordPressClient {
    
    private static final int ADMIN_USER_ID = 1;

    public WordPressSettingsClient() {
        super();
    }

    public void updateUser(int userId, String firstName, String lastName, String jobTitle, String description) throws IOException {
        String displayName = firstName + " " + lastName;
        
        JSONObject userJson = new JSONObject()
            .put("name", displayName)  // Display name
            .put("first_name", firstName)
            .put("last_name", lastName)
            .put("nickname", displayName)  // Using full name as nickname
            .put("description", description)
            .put("meta", new JSONObject()
                .put("job_title", jobTitle));

        String url = baseUrl + "users/" + userId;
        
        HttpPost request = new HttpPost(URI.create(url));
        request.setEntity(new StringEntity(userJson.toString(), StandardCharsets.UTF_8));
        request.setHeader("Content-Type", "application/json");
        
        executeRequest(request);
    }

    //METHOD DOESN'T WORK
    private int uploadProfileImage(String imageUrl) throws IOException, ParseException {
        WordPressMediaClient mediaClient = new WordPressMediaClient();
        Image profileImage = new Image("", imageUrl, "Profile Image");
        return mediaClient.uploadImage(profileImage);
    }

    public void updateAdminUser(String firstName, String lastName, String jobTitle, String description) throws IOException {
        updateUser(ADMIN_USER_ID, firstName, lastName, jobTitle, description);
    }

    public JSONObject getUser(int userId) throws IOException {
        String url = baseUrl + "users/" + userId;
        HttpGet request = new HttpGet(URI.create(url));
        
        String response = executeRequest(request);
        return new JSONObject(response);
    }

    public void updateSiteSettings(JSONObject settings) throws IOException {
        String url = baseUrl + "settings";
        
        HttpPost request = new HttpPost(URI.create(url));
        request.setEntity(new StringEntity(settings.toString(), StandardCharsets.UTF_8));
        request.setHeader("Content-Type", "application/json");
        
        executeRequest(request);
    }

    public JSONObject getSiteSettings() throws IOException {
        String url = baseUrl + "settings";
        HttpGet request = new HttpGet(URI.create(url));
        
        String response = executeRequest(request);
        return new JSONObject(response);
    }

    public JSONObject getAdminUser() throws IOException {
        return getUser(ADMIN_USER_ID);
    }

    //METHOD DOESN'T WORK
    public void updateUserAvatar(int userId, String imageUrl) throws IOException, ParseException {
        String url = baseUrl.replace("/wp-json/wp/v2/", "") + "/wp-admin/profile.php";
        System.out.println("Debug - Full URL: " + url);
        
        // First upload the image to get media ID
        int mediaId = uploadProfileImage(imageUrl);
        System.out.println("Debug - Uploaded media ID: " + mediaId);
        
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        
        
        HttpEntity multipart = builder.build();

        HttpPost request = new HttpPost(URI.create(url));
        request.setEntity(multipart);
        request.setHeader("Content-Type", "multipart/form-data");
        request.setHeader("Accept", "*/*");
        request.setHeader("Cookie", "wordpress_logged_in_906f3769ca15487cfe07e7399646ef4d=zach%7C1739140786%7CgjIBgHQSTs1DgoWQJVRCm7JxunFem6EIhQzX5FrjxBf%7Cead6f14ebf3a38ec13f7cb2fa384c27f1ba1fbd2be28eae3d76f889fe7450811");
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            System.out.println("Debug - Response status: " + response.getCode());
            System.out.println("Debug - Response headers: " + Arrays.toString(response.getHeaders()));
            String responseBody = EntityUtils.toString(response.getEntity());
            System.out.println("Debug - Response body: " + responseBody);
            
            if (response.getCode() != 200 && response.getCode() != 302) {
                throw new IOException("API call failed with status " + response.getCode() + ": " + responseBody);
            }
        }
    }

    private byte[] downloadImage(String imageUrl) throws IOException {
        HttpGet request = new HttpGet(URI.create(imageUrl));
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            return EntityUtils.toByteArray(response.getEntity());
        }
    }

    public static void main(String[] args) {
        try {
            WordPressSettingsClient client = new WordPressSettingsClient();
            
            // Test updating user avatar
            client.updateAdminUser("Evan", "Marshall", "Chief Ruck Writer", "Evan T. Marshall is the Chief Ruck Writer at RuckQuest, where he leverages over a decade of experience in endurance training and outdoor fitness to provide expert insights on rucking. As a certified personal trainer and avid rucker, Evan combines his passion for physical challenge with in-depth product knowledge to equip readers with the tools and strategies they need to succeed in their rucking journeys.");            
        } catch (Exception e) {
            System.err.println("Error during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 

