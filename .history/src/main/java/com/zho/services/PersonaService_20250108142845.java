package com.zho.services;

import com.zho.api.OpenAIClient;
import com.zho.api.wordpress.WordPressClient;
import com.zho.api.GetImgAIClient;
import com.zho.api.wordpress.WordPressMediaClient;
import com.zho.model.UnsplashImage;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import org.json.JSONObject;
import org.json.JSONException;
import org.apache.hc.core5.http.ParseException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.net.URL;

public class PersonaService {
    private final OpenAIClient openAIClient;
    private final WordPressClient wpClient;
    private final DatabaseService databaseService;
    private final GetImgAIClient getImgAIClient;
    private String name;
    private String biography;
    private String expertise;
    private String writingTone;
    private String systemPrompt;
    private String imageUrl;
    private final WordPressMediaClient mediaClient;

    
    public PersonaService() {
        this.openAIClient = new OpenAIClient();
        this.wpClient = new WordPressClient();
        this.databaseService = new DatabaseService();
        this.getImgAIClient = new GetImgAIClient();
        this.mediaClient = new WordPressMediaClient();
    }
    
    public void generateAndSetupPersona(String topic) throws IOException, ParseException {
        try {
            // First generate the stereotypical description
            String stereotypicalDescription = generateStereotypicalDescription(topic);
            System.out.println("Generated description: " + stereotypicalDescription);
            
            // Generate the image and store the URL
            this.imageUrl = getImgAIClient.generateImage(
                stereotypicalDescription,
                1024,
                1024,
                4,
                null
            );
            
            // Download and save the image locally first
            String localImagePath = "/Users/zachderhake/Desktop/persona_" + topic.replaceAll("\\s+", "_") + ".jpg";
            downloadImage(this.imageUrl, localImagePath);
            System.out.println("Image downloaded to: " + localImagePath);
            
            // Upload to WordPress Media Library
            String wpMediaUrl = mediaClient.uploadMediaFromFile(localImagePath, "Persona Image - " + topic);
            System.out.println("Uploaded to WordPress Media Library: " + wpMediaUrl);
            
            // Create UnsplashImage object with WordPress Media URL
            UnsplashImage personaImage = new UnsplashImage(
                "",               // Empty ID for AI-generated images
                wpMediaUrl,      // Use the WordPress Media URL
                topic           // Description
            );
            
            // Update the WordPress page with the new image
            mediaClient.updateColumnImage(
                318,                // page ID
                "318_74d069-a3",   // column ID
                personaImage
            );
            
            try {
                System.out.println("About to generate basic persona...");
                generateBasicPersona(topic, stereotypicalDescription);
                
                System.out.println("Basic persona generated, attempting database update...");
                System.out.println("Name: " + this.name);
                System.out.println("Expertise: " + this.expertise);
                System.out.println("Biography: " + this.biography);
                
                // Add database update here
                databaseService.updatePersona(
                    this.name,
                    this.expertise,
                    this.biography,
                    this.writingTone,
                    this.systemPrompt,
                    wpMediaUrl    // Store the WordPress Media URL
                );
                System.out.println("Database update completed successfully");
                
            } catch (JSONException e) {
                System.err.println("Error generating basic persona: " + e.getMessage());
            } catch (SQLException e) {
                System.err.println("Error saving persona to database: " + e.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("Error in persona generation: " + e.getMessage());
            throw e;
        }
    }
    
    private void generateBasicPersona(String topic, String stereotypicalDescription) throws IOException {
        String personaPrompt = String.format(
            "Create a persona for a %s expert. " +
            "The persona should be someone who is highly knowledgeable and experienced in %s. " +
            "Here's a stereotypical description to help guide the persona creation: %s " +
            "Format the response as a JSON object with these fields: " +
            "name (full name), " +
            "expertise (specific area of expertise), " +
            "biography (2-3 paragraphs about their background and experience)",
            topic, topic, stereotypicalDescription
        );
        
        String response = openAIClient.callGPT4(personaPrompt);
                
        try {
            JSONObject persona = new JSONObject(response);
            
            this.name = persona.getString("name");
            this.expertise = persona.getString("expertise");
            this.biography = persona.getString("biography");
            
            this.writingTone = generateWritingTone(topic);
            this.systemPrompt = generateSystemPrompt(topic);
            
        } catch (JSONException e) {
            System.err.println("Failed to parse JSON response: " + e.getMessage());
            throw e;
        }
    }
    
    private String generateStereotypicalDescription(String topic) throws IOException {
        String stereotypePrompt = String.format(
            "For a blog about %s, create a detailed, natural description of the most stereotypical person who could " +
            "have expertise in this field and start a blog about . Write it as a hyper-realistic image generation prompt.\n\n" +
            "Focus on these elements, but blend them naturally into a flowing description:\n" +
            "- Physical appearance (age, build, complexion)\n" +
            "- Personal style and clothing\n" +
            "- Distinctive accessories or props\n" +
            "- Environmental context and setting\n" +
            "- Lighting and atmosphere\n" +
            "- Small, realistic details that add authenticity\n\n" +
            "Include specific, tangible details that create a vivid mental image.\n\n" +
            "Example style: \"Create a hyper-realistic image of a passionate man in his early 40s, with a lean, athletic build and sun-kissed skin " +
            "that speaks of time spent outdoors. His sporty-casual style is highlighted by a well-fitted polo shirt...\" etc.\n\n" +
            "Now, create a similar description for a %s expert (make sure under 1024 characters):",
            topic,
            topic
        );
        
        return openAIClient.callGPT4(stereotypePrompt);
    }
    
    private void downloadImage(String imageUrl, String destinationPath) throws IOException {
        URL url = new URL(imageUrl);
        try (InputStream in = url.openStream();
             OutputStream out = new FileOutputStream(destinationPath)) {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }
    }
    
    // Getters for use in other services
    public String getSystemPrompt() {
        return systemPrompt;
    }
    
    public String getName() {
        return name;
    }
    
    public String getExpertise() {
        return expertise;
    }

    public String getBiography() {
        return biography;
    }
    
    public String getWritingTone() {
        return writingTone;
    }

    public String getImageUrl() {
        return imageUrl;
    }
    
    public static void main(String[] args) {
        try {
            PersonaService personaService = new PersonaService();
            String topic = "hiking in Europe";
            
            System.out.println("Generating persona for: " + topic);
            personaService.generateAndSetupPersona(topic);
            
            // Download the image
            OkHttpClient client = new OkHttpClient();
            String imageUrl = personaService.getImageUrl();
            
            System.out.println("Downloading image from: " + imageUrl);
            
            Request request = new Request.Builder().url(imageUrl).build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) throw new IOException("Failed to download image");
                
                // Get desktop path
                String desktopPath = System.getProperty("user.home") + "/Desktop";
                String fileName = "persona_" + topic.replaceAll("\\s+", "_") + ".jpg";
                String fullPath = desktopPath + "/" + fileName;
                
                // Save the image
                try (FileOutputStream fos = new FileOutputStream(fullPath);
                     InputStream in = response.body().byteStream()) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }
                
                System.out.println("Image downloaded successfully to: " + fullPath);
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String generateWritingTone(String topic) throws IOException {
        String prompt = String.format(
            "Based on this person's background and appearance:\n" +
            "Background: %s\n" +
            "Appearance: %s\n\n" +
            "Describe their natural writing tone in 2-3 words" +
            "The tone should authentically reflect their personality.",
            this.biography,
            this.imageUrl,
            topic
        );
        return openAIClient.callGPT4(prompt);
    }

    private String generateSystemPrompt(String topic) throws IOException {
        String prompt = String.format(
            "You are writing as this expert:\n" +
            "Name: %s\n" +
            "Expertise: %s\n" +
            "Background: %s\n" +
            "Writing Tone: %s\n\n" +
            "Create a system prompt that will help an AI write blog posts about %s in this person's authentic voice. " +
            "The prompt should capture their unique perspective, expertise level, and communication style.",
            this.name,
            this.expertise,
            this.biography,
            this.writingTone,
            topic
        );
        return openAIClient.callGPT4(prompt);
    }
} 