package com.zho.services;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;

public class AwsSecretsManagerService {
    private static final String GOOGLE_CREDENTIALS_SECRET_NAME = "google-search-console-credentials";
    
    public static String getGoogleCredentials() {
        try {
            AWSSecretsManager client = AWSSecretsManagerClientBuilder.defaultClient();
            GetSecretValueRequest request = new GetSecretValueRequest()
                .withSecretId(GOOGLE_CREDENTIALS_SECRET_NAME);
            GetSecretValueResult result = client.getSecretValue(request);
            return result.getSecretString();
        } catch (Exception e) {
            System.out.println("Failed to retrieve secret from AWS: " + e.getMessage());
            return null;
        }
    }
} 