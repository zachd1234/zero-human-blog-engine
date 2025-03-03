package com.zho.services;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.amazonaws.regions.Regions;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
            System.out.println("\n🚀 Starting AWS Secrets Manager test...");
            System.out.println("Secret Name: " + GOOGLE_CREDENTIALS_SECRET_NAME);
            
            String credentials = getGoogleCredentials();
            if (credentials != null) {
                System.out.println("\n✅ Successfully retrieved credentials!");
                System.out.println("Credentials length: " + credentials.length());
                System.out.println("First 50 chars: " + credentials.substring(0, Math.min(50, credentials.length())));
                
                // Parse and validate JSON structure
                try {
                    JsonObject jsonCredentials = JsonParser.parseString(credentials).getAsJsonObject();
                    System.out.println("\n📋 Credentials Structure Check:");
                    System.out.println("- type: " + (jsonCredentials.has("type") ? "✓" : "✗"));
                    System.out.println("- project_id: " + (jsonCredentials.has("project_id") ? "✓" : "✗"));
                    System.out.println("- private_key_id: " + (jsonCredentials.has("private_key_id") ? "✓" : "✗"));
                    System.out.println("- private_key: " + (jsonCredentials.has("private_key") ? "✓" : "✗"));
                    System.out.println("- client_email: " + (jsonCredentials.has("client_email") ? "✓" : "✗"));
                    System.out.println("- client_id: " + (jsonCredentials.has("client_id") ? "✓" : "✗"));
                    
                    if (jsonCredentials.has("client_email")) {
                        System.out.println("\nService Account Email: " + jsonCredentials.get("client_email").getAsString());
                    }
                } catch (Exception e) {
                    System.err.println("\n❌ Error parsing JSON credentials: " + e.getMessage());
                }
            } else {
                System.err.println("\n❌ Failed to retrieve credentials");
            }
        } catch (Exception e) {
            System.err.println("\n❌ Error testing Secrets Manager:");
            System.err.println("Type: " + e.getClass().getName());
            System.err.println("Message: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 