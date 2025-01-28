package com.zho.services;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.utils.StringUtils;

public class AwsSecretsManagerService {
    private static final String GOOGLE_CREDENTIALS_SECRET_NAME = "google-search-console-credentials";
    private static final Region REGION = Region.US_EAST_1;
    
    public static String getGoogleCredentials() {
        try {
            // Create a custom HTTP client
            ApacheHttpClient.Builder httpClientBuilder = ApacheHttpClient.builder();
            
            // Create the Secrets Manager client with custom HTTP client
            SecretsManagerClient client = SecretsManagerClient.builder()
                    .region(REGION)
                    .httpClientBuilder(httpClientBuilder)
                    .build();

            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(GOOGLE_CREDENTIALS_SECRET_NAME)
                    .build();

            GetSecretValueResponse response = client.getSecretValue(request);
            String secret = response.secretString();
            
            if (StringUtils.isBlank(secret)) {
                System.out.println("Retrieved secret is empty");
                return null;
            }
            
            return secret;
            
        } catch (Exception e) {
            System.out.println("Failed to retrieve secret from AWS: " + e.getMessage());
            e.printStackTrace();
            return null; // Return null to allow fallback to local file
        }
    }
} 