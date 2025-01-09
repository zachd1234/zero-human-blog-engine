package com.zho.services;

import com.zho.api.OpenAIClient;
import com.zho.api.wordpress.WordPressClient;
import com.zho.api.GetImgAIClient;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import org.json.JSONObject;
import org.json.JSONException;

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

    
    public PersonaService() {
        this.openAIClient = new OpenAIClient();
        this.wpClient = new WordPressClient();
        this.databaseService = new DatabaseService();
        this.getImgAIClient = new GetImgAIClient();
    }
    
    public void generateAndSetupPersona(String topic) throws IOException, ParseException, SQLException {
        // First generate the stereotypical description
        String stereotypicalDescription = generateStereotypicalDescription(topic);
        System.out.println("Generated description: " + stereotypicalDescription);
        
        // Generate the image using the description
        String imageUrl = getImgAIClient.generateImage(
            stereotypicalDescription,  // use the description as the prompt
            375,                      // width
            250,                      // height
            4,                        // steps
            null                      // random seed
        );
        System.out.println("Generated image URL: " + imageUrl);
        
        // Then generate the basic persona with this description
        generateBasicPersona(topic, stereotypicalDescription);
        
        // Then generate their writing tone
        String tonePrompt = String.format(
            "Based on the following fictional persona, create a detailed description of their writing tone that reflects " +
            "their personality, background, and expertise. The tone should align with their role as a blogger and feel " +
            "authentic to their character.\n\n" +
            "Persona:\n" +
            "• Name: %s\n" +
            "• Expertise: %s\n" +
            "• Biography: %s\n\n" +
            "Requirements for the Writing Tone:\n" +
            "1. Describe the overall tone (e.g., friendly, professional, adventurous, witty)\n" +
            "2. Specify how they convey their expertise (e.g., approachable for beginners, authoritative for professionals)\n" +
            "3. Explain their choice of language (e.g., formal, conversational, humorous, poetic)\n" +
            "4. Include how they incorporate their fun fact or unique personality traits into their writing\n" +
            "5. Highlight how they connect with their audience (e.g., through personal stories, actionable advice, or a touch of humor)\n\n" +
            "Format: Return ONLY a JSON object with these fields:\n" +
            "{\n" +
            "  \"toneDescription\": \"comprehensive description of their writing style incorporating all requirements\",\n" +
            "  \"examples\": \"3 short example phrases or sentences that showcase their unique voice\"\n" +
            "}",
            name,
            expertise,
            biography
        );
        
        String toneResponse = openAIClient.callGPT4(tonePrompt);
        JSONObject toneJson = new JSONObject(toneResponse);
        this.writingTone = toneJson.getString("toneDescription");
        
        // Update system prompt to include tone guidance
        this.systemPrompt = String.format(
            "You are %s, %s. " +
            "Your responses should reflect your expertise and experience in %s. " +
            "Draw from your background: %s\n\n" +
            "Writing Style:\n%s\n\n" +
            "Always maintain this tone and personality in your responses. Be consistent with this voice across all content.",
            this.name,
            this.expertise,
            topic,
            this.biography,
            this.writingTone
        );
        
        // Save to database
        databaseService.updatePersona(
            this.name,
            this.expertise,
            this.biography,
            this.writingTone,
            this.systemPrompt
        );
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
            String topic = "tennis coaching";
            
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