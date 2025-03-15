package com.zho.api;

import com.zho.config.ConfigManager;
import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;
import com.google.cloud.aiplatform.v1.EndpointName;
import com.google.cloud.aiplatform.v1.PredictionServiceClient;
import com.google.cloud.aiplatform.v1.PredictionServiceSettings;
import com.google.cloud.aiplatform.v1.PredictRequest;
import com.google.cloud.aiplatform.v1.PredictResponse;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.Struct;
import java.util.Map;
import java.util.HashMap;
import org.json.JSONArray;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.api.gax.core.FixedCredentialsProvider;
import java.io.FileInputStream;
import java.io.File;
import com.zho.api.wordpress.WordPressMediaClient;

public class GetImgAIClient {
    private final String apiKey;
    private final String apiUrl;
    private final OkHttpClient client;
    
    public GetImgAIClient() {
        this.apiKey = ConfigManager.getGetImgApiKey();
        this.apiUrl = ConfigManager.getGetImgApiUrl();
        this.client = new OkHttpClient();
    }
    
    public String generateImage(String prompt) throws IOException {
        return generateImage(prompt, 1024, 1024, 4, null);
    }

    public String generateImage(String prompt, int width, int height) throws IOException {
        return generateImage(prompt, width, height, 4, null);
    }
    
    public String generateImage(String prompt, Integer width, Integer height, 
                              Integer steps, Integer seed) throws IOException {
        JSONObject json = new JSONObject()
            .put("prompt", prompt)
            .put("width", width)
            .put("height", height)
            .put("steps", steps)
            .put("output_format", "jpeg")
            .put("response_format", "url");  // Using URL format for easier handling
            
        if (seed != null) {
            json.put("seed", seed);
        }
            
        RequestBody body = RequestBody.create(
            json.toString(), 
            MediaType.get("application/json; charset=utf-8")
        );
        
        Request request = new Request.Builder()
            .url(apiUrl + "/flux-schnell/text-to-image")
            .header("Authorization", "Bearer " + apiKey)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .post(body)
            .build();
            
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                throw new IOException("API call failed: " + response.code() + " - " + errorBody);
            }
            
            JSONObject responseJson = new JSONObject(response.body().string());
            return responseJson.getString("url");  // Returns the image URL
        }
    }
    
    /**
     * Generates an image using Google Vertex AI Imagen and returns the image URL
     * 
     * @param prompt The text prompt to generate the image from
     * @param width The width of the image (used to determine aspect ratio)
     * @param height The height of the image (used to determine aspect ratio)
     * @param numberOfImages Number of images to generate (usually 1)
     * @param negativePrompt Optional negative prompt (can be null)
     * @return The URL of the generated image
     * @throws IOException If there's an error communicating with the API
     */
    public String generateImageWithVertex(String prompt, int width, int height, int numberOfImages, String negativePrompt) throws IOException {
        // Set up the project and location
        String projectId = ConfigManager.getVertexProjectId();
        String location = ConfigManager.getVertexLocation();
        String modelId = ConfigManager.getVertexEndpointId(); // This is actually the model ID
        String credentialsPath = ConfigManager.getVertexCredentialsPath();
        
        // Create credentials from the file
        GoogleCredentials credentials = GoogleCredentials.fromStream(
            new FileInputStream(credentialsPath));
        
        // Create client settings with explicit credentials
        PredictionServiceSettings settings = PredictionServiceSettings.newBuilder()
            .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
            .build();
        
        // Determine aspect ratio based on width and height
        String aspectRatio = determineAspectRatio(width, height);
        
        try (PredictionServiceClient predictionServiceClient = PredictionServiceClient.create(settings)) {
            // Format the model name correctly
            String modelName = String.format("projects/%s/locations/%s/publishers/google/models/%s", 
                                            projectId, location, modelId);
            
            // Create the request payload
            Map<String, Object> instanceMap = new HashMap<>();
            instanceMap.put("prompt", prompt);
            
            Map<String, Object> parameterMap = new HashMap<>();
            parameterMap.put("aspectRatio", aspectRatio);
            parameterMap.put("sampleCount", numberOfImages);
            parameterMap.put("addWatermark", true);
            
            if (negativePrompt != null && !negativePrompt.isEmpty()) {
                parameterMap.put("negativePrompt", negativePrompt);
            }
            
            // Convert instance map to Struct
            Struct.Builder instanceBuilder = Struct.newBuilder();
            for (Map.Entry<String, Object> entry : instanceMap.entrySet()) {
                Value value;
                if (entry.getValue() instanceof String) {
                    value = Value.newBuilder().setStringValue((String) entry.getValue()).build();
                } else {
                    continue;
                }
                instanceBuilder.putFields(entry.getKey(), value);
            }
            
            // Convert parameter map to Struct
            Struct.Builder paramBuilder = Struct.newBuilder();
            for (Map.Entry<String, Object> entry : parameterMap.entrySet()) {
                Value value;
                if (entry.getValue() instanceof String) {
                    value = Value.newBuilder().setStringValue((String) entry.getValue()).build();
                } else if (entry.getValue() instanceof Integer) {
                    value = Value.newBuilder().setNumberValue((Integer) entry.getValue()).build();
                } else if (entry.getValue() instanceof Boolean) {
                    value = Value.newBuilder().setBoolValue((Boolean) entry.getValue()).build();
                } else {
                    continue;
                }
                paramBuilder.putFields(entry.getKey(), value);
            }
            
            // Create the prediction request
            PredictRequest predictRequest = PredictRequest.newBuilder()
                .setEndpoint(modelName)
                .addInstances(Value.newBuilder().setStructValue(instanceBuilder.build()).build())
                .setParameters(Value.newBuilder().setStructValue(paramBuilder.build()).build())
                .build();
            
            // Make the prediction request
            PredictResponse response = predictionServiceClient.predict(predictRequest);
            
            // Parse the response to get the image URL
            String responseJson = JsonFormat.printer().print(response);

            JSONObject jsonResponse = new JSONObject(responseJson);
            JSONArray predictions = jsonResponse.getJSONArray("predictions");
            JSONObject firstPrediction = predictions.getJSONObject(0);

            // Extract the base64 encoded image from the response
            String base64Image = firstPrediction.getString("bytesBase64Encoded");
            
            // Generate a unique filename
            String timestamp = String.valueOf(System.currentTimeMillis());
            String filename = "vertex-image-" + timestamp + ".jpg";
            
            // Upload to WordPress
            WordPressMediaClient wpClient = new WordPressMediaClient();
            String wordpressUrl = wpClient.uploadMediaFromBase64(
                base64Image,
                filename,
                "AI Generated: " + prompt.substring(0, Math.min(50, prompt.length())) + "..."
            );
            
            return wordpressUrl;
        }
    }

    /**
     * Determines the aspect ratio string based on width and height
     */
    private String determineAspectRatio(int width, int height) {
        if (width == height) {
            return "1:1";
        } else if (width > height) {
            // Calculate the ratio
            int gcd = gcd(width, height);
            return (width / gcd) + ":" + (height / gcd);
        } else {
            int gcd = gcd(height, width);
            return (width / gcd) + ":" + (height / gcd);
        }
    }

    /**
     * Calculate greatest common divisor for aspect ratio calculation
     */
    private int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    /**
     * Generates an image using Google Vertex AI with default settings
     * 
     * @param prompt The text prompt to generate the image from
     * @return The WordPress URL of the uploaded image
     * @throws IOException If there's an error communicating with the API
     */
    public String generateImageWithVertex(String prompt) throws IOException {
        // Default settings: 1024x1024 square image, single image, no negative prompt
        return generateImageWithVertex(prompt, 1024, 1024, 1, null);
    }

    /**
     * Generates an image using Google Vertex AI with specified dimensions
     * 
     * @param prompt The text prompt to generate the image from
     * @param width The width of the image
     * @param height The height of the image
     * @return The WordPress URL of the uploaded image
     * @throws IOException If there's an error communicating with the API
     */
    public String generateImageWithVertex(String prompt, int width, int height) throws IOException {
        // Use specified dimensions, single image, no negative prompt
        return generateImageWithVertex(prompt, width, height, 1, null);
    }

    /**
     * Test method for Vertex AI image generation
     */
    public static void main(String[] args) {
        try {
            String credentialsPath = ConfigManager.getVertexCredentialsPath();
            System.out.println("Using credentials from: " + credentialsPath);
            
            if (credentialsPath == null || credentialsPath.isEmpty()) {
                System.err.println("Google credentials path not found. Please set GOOGLE_APPLICATION_CREDENTIALS environment variable.");
                return;
            }
            
            System.setProperty("GOOGLE_APPLICATION_CREDENTIALS", credentialsPath);
    
            GetImgAIClient client = new GetImgAIClient();
            
            String prompt = "A professional business person in a modern office setting, " +
                          "wearing formal attire, high quality, realistic, natural lighting";
            
            System.out.println("Generating image with Vertex AI...");
            String imageUrl = client.generateImageWithVertex(
                prompt,      // prompt
                1024,        // width
                1024,        // height
                1,           // number of images
                null         // negative prompt
            );
            
            System.out.println("Image generated successfully!");
            System.out.println("URL: " + imageUrl);
            
        } catch (Exception e) {
            System.err.println("Error generating image: " + e.getMessage());
            e.printStackTrace();
        }
    }

}