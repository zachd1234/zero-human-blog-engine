package com.zho.services;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.amazonaws.regions.Regions;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;

public class AwsSecretsManagerService {
    private static final String GOOGLE_CREDENTIALS_SECRET_NAME = "google-search-console-credentials";
    
    public static String getGoogleCredentials() {
        try {
            EndpointConfiguration endpointConfig = new EndpointConfiguration(
                "secretsmanager.us-east-1.amazonaws.com", 
                "us-east-1"
            );
            
            AWSSecretsManager client = AWSSecretsManagerClientBuilder.standard()
                .withEndpointConfiguration(endpointConfig)
                .build();
                
            GetSecretValueRequest request = new GetSecretValueRequest()
                .withSecretId(GOOGLE_CREDENTIALS_SECRET_NAME);
            GetSecretValueResult result = client.getSecretValue(request);
            return result.getSecretString();
        } catch (Exception e) {
            System.out.println("Failed to retrieve secret from AWS: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        try {
            System.out.println("Starting AWS Secrets Manager test...");
            System.out.println("Attempting to retrieve secret: " + GOOGLE_CREDENTIALS_SECRET_NAME);
            
            String credentials = getGoogleCredentials();
            if (credentials != null) {
                System.out.println("Successfully retrieved credentials from Secrets Manager");
                System.out.println("First 50 characters of credentials: " + credentials.substring(0, Math.min(50, credentials.length())));
            } else {
                System.out.println("Failed to retrieve credentials");
            }
        } catch (Exception e) {
            System.err.println("Error testing Secrets Manager: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 