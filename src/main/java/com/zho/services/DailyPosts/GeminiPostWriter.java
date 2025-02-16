package com.zho.services.DailyPosts;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.service.SystemMessage;
import com.zho.config.ConfigManager;
import com.zho.api.GetImgAIClient;
import java.io.IOException;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.output.Response;

public class GeminiPostWriter {
    
    private final ChatLanguageModel model = GoogleAiGeminiChatModel.builder()
        .apiKey(ConfigManager.getGeminiKey())
        .modelName("gemini-1.5-flash")
        .temperature(0.7)
        .build();

    @SystemMessage({
        "You are a professional blog post editor. Your task is to enhance blog posts with relevant images.",
        "Follow these steps EXACTLY:",
        "STEP 1: READ the blog post carefully",
        "STEP 2: IDENTIFY 2-3 exact lines where an image would enhance the content",
        "STEP 3: For each identified line:",
            "- COPY the exact line from the blog post",
            "- THINK of a relevant image description",
            "- CALL generateImage with your description",
            "- SAVE the returned URL",
        "STEP 4: FORMAT your response as this exact JSON:",
        "{",
        "  'imagePlacements': [",
        "    {",
        "      'insertBefore': '<exact line from blog>',",
        "      'imageUrl': '<URL from generateImage>'",
        "    }",
        "  ]",
        "}"
    })
    interface PostAgent {
        String enhancePostWithImages(String blogPost);
    }

    private final PostAgent agent = AiServices.builder(PostAgent.class)
        .chatLanguageModel(model)
        .tools(this)
        .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
        .build();

    private final GetImgAIClient imgClient = new GetImgAIClient();

    @Tool("Generates an image based on the provided prompt and returns the URL")
    public String generateImage(String prompt) {
        try {
            System.out.println("Generating image for: " + prompt);
            String url = imgClient.generateImage(prompt);
            System.out.println("Generated URL: " + url);
            return url;
        } catch (IOException e) {
            System.err.println("Error generating image: " + e.getMessage());
            return "Error generating image: " + e.getMessage();
        }
    }

    public String enhancePost(String blogPost) {
        String imagePlacements = agent.enhancePostWithImages(blogPost);
        System.out.println("Image Placements: " + imagePlacements);
        return blogPost;
    }

    public static void main(String[] args) {
        GeminiPostWriter postWriter = new GeminiPostWriter();
        String samplePost = 
            "🎾 The Art and Science of Tennis: A Perfect Blend of Skill and Strategy\n\n" +
            "Tennis is more than just a sport—it's a test of endurance, precision, and mental strength. " +
            "Whether played on grass, clay, or hard courts, the game demands a unique combination of " +
            "agility, technique, and strategic thinking.\n\n" +
            "🎾 The Fundamentals\n\n" +
            "At its core, tennis is a battle of consistency and power. Players must master essential " +
            "shots such as the forehand, backhand, serve, and volley. Each shot requires precise timing, " +
            "footwork, and tactical placement to outmaneuver an opponent.\n\n";
        
        String enhancedPost = postWriter.enhancePost(samplePost);
        System.out.println("Enhanced Post: " + enhancedPost);
    }
} 