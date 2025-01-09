package com.zho.services;

import com.zho.api.OpenAIClient;
import com.zho.api.wordpress.WordPressClient;
import org.json.JSONObject;
import java.io.IOException;
import org.apache.hc.core5.http.ParseException;
import org.json.JSONException;

public class PersonaService {
    private final OpenAIClient openAIClient;
    private final WordPressClient wpClient;
    private String name;
    private String biography;
    private String expertise;
    private String writingTone;
    private String systemPrompt;
    
    public PersonaService(OpenAIClient openAIClient, WordPressClient wpClient) {
        this.openAIClient = openAIClient;
        this.wpClient = wpClient;
    }
    
    public void generateAndSetupPersona(String topic) throws IOException, ParseException {
        // First generate the stereotypical description
        String stereotypicalDescription = generateStereotypicalDescription(topic);
        
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
            "I am casting an actor to play the role of someone who writes a blog about %s.\n\n" +
            "Describe the most stereotypical person that would run this type of blog. Include:\n" +
            "1. Age range and gender\n" +
            "2. Physical appearance (face, build, style)\n" +
            "3. How they present themselves professionally\n" +
            "4. Any distinctive features that make them instantly recognizable as an expert in %s\n\n" +
            "Format: Return a single, detailed sentence that paints a clear visual picture.\n" +
            "Example: \"A fit woman in her mid-30s with a sleek ponytail and athletic wear, sporting a tennis bracelet " +
            "and a light tan from countless hours on outdoor courts.\"",
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
    
    public static void main(String[] args) {
        try {
            // Create dependencies
            OpenAIClient openAIClient = new OpenAIClient();
            
            // Create PersonaService instance
            PersonaService personaService = new PersonaService(openAIClient, null);
            
            // Test with a sample topic
            String testTopic = "discounted cash flow commerical real estate ";
            personaService.generateAndSetupPersona(testTopic);
            
            // Print results
            System.out.println("\n=== Generated Persona ===");
            System.out.println("Name: " + personaService.getName());
            System.out.println("\nExpertise: " + personaService.getExpertise());
            System.out.println("\nBiography: " + personaService.getBiography());
            System.out.println("\nWriting Tone: " + personaService.getWritingTone());
            System.out.println("\nSystem Prompt: " + personaService.getSystemPrompt());
            
        } catch (Exception e) {
            System.err.println("Error testing PersonaService: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 