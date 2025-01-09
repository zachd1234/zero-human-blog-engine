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
    private String systemPrompt;
    
    public PersonaService(OpenAIClient openAIClient, WordPressClient wpClient) {
        this.openAIClient = openAIClient;
        this.wpClient = wpClient;
    }
    
    public void generateAndSetupPersona(String topic) throws IOException, ParseException {
        // Generate the persona
        String personaPrompt = String.format(
            "Create a fictional but credible expert persona for a blog about %s.\n" +
            "Consider:\n" +
            "1. What background would give them deep expertise?\n" +
            "2. What experiences make them uniquely qualified?\n" +
            "3. What achievements give them authority?\n" +
            "4. What personal journey led them here?\n\n" +
            "Format: Return ONLY a JSON object with these fields:\n" +
            "{\n" +
            "  \"name\": \"full name\",\n" +
            "  \"expertise\": \"one-line expertise summary\",\n" +
            "  \"biography\": \"engaging 4-5 sentence life story\"\n" +
            "}", 
            topic
        );
        
        String response = openAIClient.callOpenAI(personaPrompt);
        JSONObject persona = new JSONObject(response);
        
        // Set the fields
        this.name = persona.getString("name");
        this.expertise = persona.getString("expertise");
        this.biography = persona.getString("biography");
        
        // Create system prompt
        this.systemPrompt = String.format(
            "You are %s, %s. " +
            "Your responses should reflect your expertise and experience in %s. " +
            "Draw from your background: %s",
            this.name,
            this.expertise,
            topic,
            this.biography
        );
        
        // Update WordPress About page
    }

    
    // Getters for use in other services
    public String getSystemPrompt() {
        return systemPrompt;
    }
    
    public String getName() {
        return name;
    }
} 