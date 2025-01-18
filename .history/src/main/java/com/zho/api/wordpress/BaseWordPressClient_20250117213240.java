package com.zho.api.wordpress;

import java.io.IOException;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpRequest;
import com.zho.config.ConfigManager;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.ParseException;

public abstract class BaseWordPressClient {
    protected final String baseUrl;
    protected final String username;
    protected final String password;
    protected final CloseableHttpClient httpClient;

    protected BaseWordPressClient() {
        this.baseUrl = ConfigManager.getWpBaseUrl();
        System.out.println("Using WordPress URL: " + this.baseUrl);
        this.username = ConfigManager.getWpUsername();
        this.password = ConfigManager.getWpPassword();
        this.httpClient = HttpClients.createDefault();
    }

    protected void setAuthHeader(HttpRequest request) {
        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        request.setHeader("Authorization", "Basic " + encodedAuth);
    }

    protected String executeRequest(HttpUriRequest request) throws IOException {
        setAuthHeader(request);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getCode();
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            
            if (statusCode < 200 || statusCode >= 300) {
                throw new IOException("API call failed with status " + statusCode + ": " + responseBody);
            }
            
            return responseBody;
        } catch (ParseException e) {
            throw new IOException("Failed to parse response", e);
        }
    }
} 