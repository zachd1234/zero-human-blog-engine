package com.zho.services.DailyPosts;
import dev.langchain4j.model.openai.OpenAiChatModel; // Correct import
import dev.langchain4j.service.AiServices;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.service.SystemMessage;
import com.zho.config.ConfigManager;
import com.zho.api.GetImgAIClient;
import java.io.IOException;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.output.Response;

public class PostWriterV2 {
    
    // Initialize the OpenAiChatModel
    private final OpenAiChatModel model = OpenAiChatModel.builder()
        .apiKey(ConfigManager.getOpenAiKey())
        .modelName("gpt-4") // Specify the model name
        .build();

    @SystemMessage({
        "You are a professional blog post editor. Your task is to enhance blog posts with relevant images.",
        "When you receive a blog post:",
        "1. Identify 2-3 key points where images would enhance the narrative",
        "2. For each identified point:",
            "- Specify the exact text before which the image should be inserted",
            "- Generate a relevant image using the generateImage tool",
        "3. Return a JSON array of image placements, each containing:",
            "- 'insertBefore': the exact text where image should be inserted",
            "- 'imageUrl': the URL from generateImage",
        "EXAMPLE OUTPUT FORMAT:",
        "{",
        "  'imagePlacements': [",
        "    {",
        "      'insertBefore': '🎾 The Art and Science of Tennis: A Perfect Blend of Skill and Strategy',",
        "      'imageUrl': 'URL_FROM_GENERATE_IMAGE'",
        "    }",
        "  ]",
        "}",
        "IMPORTANT:",
        "- Return ONLY the JSON object, nothing else",
        "- Do not add any descriptions or commentary",
        "- Do not modify the original text",
        "- Do not create new text",
        "- The insertBefore value must be an exact copy of text from the blog post"
    })
    interface PostAgent {
        String enhancePostWithImages(String blogPost);
    }

    // Create the agent with tools from this class
    private final PostAgent agent = AiServices.builder(PostAgent.class)
        .chatLanguageModel(model)
        .tools(this)
        .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
        .build();

    private final GetImgAIClient imgClient = new GetImgAIClient();

    @Tool("Generates an image based on the provided prompt and returns the URL")
    public String generateImage(String prompt) {
        try {
            System.out.println("Generating image for: " + prompt); // Debug print
            String url = imgClient.generateImage(prompt);
            System.out.println("Generated URL: " + url); // Debug print
            return url;
        } catch (IOException e) {
            System.err.println("Error generating image: " + e.getMessage());
            return "Error generating image: " + e.getMessage();
        }
    }

    public String enhancePost(String blogPost) {
        // Get image placements from agent
        String imagePlacements = agent.enhancePostWithImages(blogPost);
        return imagePlacements;        
        // TODO: Parse JSON and insert images at specified locations
        // We can implement this part next
    }

    public static void main(String[] args) {
        PostWriterV2 postWriter = new PostWriterV2();
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