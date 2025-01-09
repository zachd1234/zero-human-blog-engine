package com.zho.services;

import com.zho.api.OpenAIClient;
import com.zho.api.wordpress.WordPressClient;
import org.json.JSONObject;
import java.io.IOException;
import org.apache.hc.core5.http.ParseException;

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
        // First generate the basic persona
        generateBasicPersona(topic);
        
        // Then generate their writing tone
        String tonePrompt = String.format(
            "Based on %s's background as a %s, create a distinctive writing tone that fits their personality and expertise.\n\n" +
            "Consider:\n" +
            "1. How formal/informal are they?\n" +
            "2. What unique expressions or phrases do they use?\n" +
            "3. How do they connect with their audience?\n" +
            "4. What metaphors or examples are typical for them?\n" +
            "5. What's their energy level (enthusiastic, calm, analytical)?\n" +
            "6. Any signature communication quirks?\n\n" +
            "Format: Return ONLY a JSON object with these fields:\n" +
            "{\n" +
            "  \"toneDescription\": \"detailed description of their writing style\",\n" +
            "  \"examples\": \"3 short example phrases showing their voice\"\n" +
            "}\n\n" +
            "Example Response:\n" +
            "{\n" +
            "  \"toneDescription\": \"Energetic and encouraging with a dash of humor. Uses tennis metaphors frequently. Balances technical expertise with approachable explanations. Often starts sentences with 'Here's the thing...' or 'Picture this...' Makes complex concepts relatable through everyday analogies. Occasionally uses playful tennis puns.\",\n" +
            "  \"examples\": \"'Let's ace this technique together!', 'Here's the thing about backhands...', 'Think of your grip like holding a sandwich - not too tight, not too loose.'\"\n" +
            "}",
            name,
            expertise
        );
        
        String toneResponse = openAIClient.callOpenAI(tonePrompt);
        JSONObject toneJson = new JSONObject(toneResponse);
        this.writingTone = toneJson.getString("toneDescription");
        
        // Update system prompt to include tone guidance
        this.systemPrompt = String.format(
            "You are %s, %s. " +
            "Your responses should reflect your expertise and experience in %s. " +
            "Draw from your background: %s\n\n" +
            "Writing Style:\n%s\n\n" +
            "Always maintain this tone and personality in your responses.",
            this.name,
            this.expertise,
            topic,
            this.biography,
            this.writingTone
        );
    }
    
    private void generateBasicPersona(String topic) throws IOException {
        String personaPrompt = String.format(
            "I am creating a fictional character who is the perfect fit to run a blog on the topic of [%s]. " +
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
            "  \"biography\": \"engaging life story (first person)\"\n" +
            "}",
            topic
        );
        
        String response = openAIClient.callOpenAI(personaPrompt);
        JSONObject persona = new JSONObject(response);
        
        this.name = persona.getString("name");
        this.expertise = persona.getString("expertise");
        this.biography = persona.getString("biography");
    }
    
    // Getters for use in other services
    public String getSystemPrompt() {
        return systemPrompt;
    }
    
    public String getName() {
        return name;
    }
    
    public String getWritingTone() {
        return writingTone;
    }
} 