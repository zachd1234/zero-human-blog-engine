package com.zho.services;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

public class AwsSecretsManagerService {
    private static final String GOOGLE_CREDENTIALS_SECRET_NAME = "google-search-console-credentials";
    private static final Region REGION = Region.US_EAST_1;
    
    public static String getGoogleCredentials() {
        try (SecretsManagerClient client = SecretsManagerClient.builder()
                .region(REGION)
                .build()) {

            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(GOOGLE_CREDENTIALS_SECRET_NAME)
                    .build();

            GetSecretValueResponse response = client.getSecretValue(request);
            return response.secretString();
            
        } catch (Exception e) {
            System.out.println("Failed to retrieve secret from AWS: " + e.getMessage());
            return null; // Return null to allow fallback to local file
        }
    }
} 