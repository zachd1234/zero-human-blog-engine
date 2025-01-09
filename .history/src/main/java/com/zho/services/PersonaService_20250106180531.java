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
        // Move the original persona generation code here
        String personaPrompt = String.format(
            "Create a fictional but highly credible expert persona for a blog about %s.\n\n" +
            "Important: Make this persona feel authentic and relatable - the kind of person who would naturally start this type of blog. " +
            "Include specific details about their journey, challenges overcome, and real-world experiences. " +
            "Think about what makes someone trustworthy and engaging in this field.\n\n" +
            "Consider:\n" +
            "1. Personal Journey: What specific experiences shaped their expertise? (Include actual events, challenges, or turning points)\n" +
            "2. Professional Background: What concrete credentials and real-world experience do they have? (Include specific years, roles, or institutions)\n" +
            "3. Teaching/Sharing History: How have they helped others in this field? (Include specific examples of impact)\n" +
            "4. Relatable Struggles: What challenges did they overcome that readers might identify with?\n" +
            "5. Current Focus: Why are they passionate about helping others in this specific area?\n\n" +
            "Format: Return ONLY a JSON object with these fields:\n" +
            "{\n" +
            "  \"name\": \"full name (should feel natural for their background)\",\n" +
            "  \"expertise\": \"one-line expertise summary in first person (e.g., 'I'm a former Division I tennis player turned coach with 15 years of experience helping beginners master the fundamentals')\",\n" +
            "  \"biography\": \"engaging 4-5 sentence life story in first person. Include specific details like years of experience, real locations, actual challenges overcome, and concrete achievements. Make it personal and authentic.\"\n" +
            "}\n\n" +
            "Example Response Structure (but for %s):\n" +
            "{\n" +
            "  \"name\": \"Sarah Chen-Martinez\",\n" +
            "  \"expertise\": \"I'm a certified pastry chef with 12 years of experience, specializing in teaching complex techniques to home bakers\",\n" +
            "  \"biography\": \"After burning my first batch of cookies at age 8, I never imagined I'd graduate top of my class from Le Cordon Bleu Paris in 2010. I spent five years working as head pastry chef at The Ritz-Carlton San Francisco, where I discovered my true passion: teaching our weekend baking workshops to enthusiastic beginners. In 2015, I opened my own baking school in Portland, Oregon, where I've helped over 2,000 home bakers master everything from basic bread to complex French pastries. What drives me is seeing that 'aha' moment when a student finally nails a technique they've been struggling with - it reminds me of my own journey from kitchen disasters to professional success.\"\n" +
            "}", 
            topic,
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