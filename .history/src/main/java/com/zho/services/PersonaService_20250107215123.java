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
import java.sql.SQLException;

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
            
            // The local path where GetImgAI saves the image
            String localImagePath = "/Users/zachderhake/Desktop/persona_" + topic.replaceAll("\\s+", "_") + ".jpg";
            
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
                generateBasicPersona(topic, stereotypicalDescription);
            } catch (JSONException e) {
                System.err.println("Error generating basic persona: " + e.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("Error in persona generation: " + e.getMessage());
            throw e;
        }
    }
    
    private void generateBasicPersona(String topic, String stereotypicalDescription) throws IOException {
        String personaPrompt = String.format(
            "I am creating a fictional character who is the perfect fit to run a blog on the topic of [%s]. " +
            "Consider this stereotypical description of such a person: \"%s\"\n\n" +
            "Please generate a detailed JSON response describing this character with the following fields:\n\n" +
            "1. name: Provide a natural, culturally appropriate full name for someone with a background related to the topic. " +
            "The name should feel realistic and specific to their identity or expertise.\n\n" +
            "2. expertise: Write a concise, one-line summary in the first person describing their area of expertise, " +
            "written in a confident, relatable tone.\n\n" +
            "3. biography: Craft an engaging and authentic 4-5 sentence life story in the first person. Include:\n" +
            "• The number of years they've been involved in the topic\n" +
            "• Specific locations or settings tied to their background\n" +
            "• Realistic challenges they've faced and overcome\n" +
            "• Concrete achievements or milestones\n" +
            "• A tone that reflects their personality and passion for the topic\n" +
            "• An optional fun fact about them (doesn't have to be related to the topic)\n\n" +
            "The character should feel human, relatable, and perfectly aligned with the blog's theme.\n\n" +
            "Format: Return ONLY a JSON object with these exact fields:\n" +
            "{\n" +
            "  \"name\": \"full name\",\n" +
            "  \"expertise\": \"one-line expertise summary (first person)\",\n" +
            "  \"biography\": \"engaging life story (4-5 sentances) (first person)\"\n" +
            "}",
            topic,
            stereotypicalDescription
        );
        
        String response = openAIClient.callGPT4(personaPrompt);
                
        try {
            JSONObject persona = new JSONObject(response);
            
            this.name = persona.getString("name");
            this.expertise = persona.getString("expertise");
            this.biography = persona.getString("biography");
        } catch (JSONException e) {
            System.err.println("Failed to parse JSON response: " + e.getMessage());
            throw e;
        }
    }
    
    private String generateStereotypicalDescription(String topic) throws IOException {
        String stereotypePrompt = String.format(
            "For a blog about %s, create a detailed, natural description of the most stereotypical person who would be " +
            "an expert in this field. Write it as a hyper-realistic image generation prompt.\n\n" +
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
    }} 