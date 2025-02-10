package com.zho.services.BlogSetup;

import com.zho.api.OpenAIClient;
import com.zho.api.wordpress.WordPressBlockClient;
import com.zho.model.BlogRequest;
import com.zho.model.Site;
import com.zho.api.GetImgAIClient;
import com.zho.api.wordpress.WordPressMediaClient;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import com.zho.api.wordpress.WordPressSettingsClient;

public class BlogLayoutService {
    private final OpenAIClient openAIClient;
    private final WordPressBlockClient wpClient;
    private final GetImgAIClient getImgAIClient;
    private final WordPressMediaClient mediaClient;

    public BlogLayoutService() {
        this.openAIClient = new OpenAIClient();
        this.wpClient = new WordPressBlockClient();
        this.getImgAIClient = new GetImgAIClient();
        this.mediaClient = new WordPressMediaClient();
    }
  
    public void setUpLayout(BlogRequest blogRequest) {
        updateCommitmentAccordion(blogRequest);
        updateAuthorBlock(blogRequest);
    }

    public void updateCommitmentAccordion(BlogRequest blogRequest) {
        int maxRetries = 3;
        int currentTry = 0;
        
        while (currentTry < maxRetries) {
            try {
                int faqId = wpClient.getFirstAccordionFAQId();
                String baseUrl = Site.getCurrentSite().getUrl().replaceFirst("wp-json/wp/v2/", "");
                String siteTitle = wpClient.getSiteTitle();
                
                String prompt = String.format(
                    "Generate a commitment to readers statement (without any heading) for a blog.\n\n" +
                    "**Blog Details**:\n" +
                    "- Blog Name: %s\n" +
                    "- Blog Description: %s\n\n" +
                    "**Tone & Style**:\n" +
                    "- Keep it **trustworthy, professional, and clear**\n" +
                    "- Use an **engaging, authoritative yet approachable tone** that fits the niche\n\n" +
                    "**Content Guidelines**:\n" +
                    "- Emphasize **quality, accuracy, and transparency** in content creation\n" +
                    "- If the blog includes **product reviews**, state that reviews are **objective and unbiased**\n\n" +
                    "**Closing Line Format**:\n" +
                    "- End with this EXACT HTML (no markdown): \"Learn more about our editorial standards: <a href='%seditorial-guidelines/'>Editorial Policy</a>\"\n\n" +
                    "**Important**:\n" +
                    "- Do NOT include any heading\n" +
                    "- Use the exact HTML format for the link\n" +
                    "- Make sure the content flows naturally into the editorial policy link",
                    siteTitle,
                    blogRequest.getDescription(), 
                    baseUrl
                );
                
                String commitmentContent = openAIClient.callOpenAI(prompt);
                wpClient.updateAccordionFAQ(faqId, commitmentContent);
                return; // Success, exit the retry loop
                
            } catch (java.net.SocketTimeoutException e) {
                currentTry++;
                if (currentTry >= maxRetries) {
                    System.err.println("Error updating commitment accordion after " + maxRetries + " retries: " + e.getMessage());
                    e.printStackTrace();
                } else {
                    System.out.println("Timeout occurred, retrying... (Attempt " + (currentTry + 1) + " of " + maxRetries + ")");
                    try {
                        Thread.sleep(2000 * currentTry); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (Exception e) {
                System.err.println("Error updating commitment accordion: " + e.getMessage());
                e.printStackTrace();
                break; // Don't retry on non-timeout errors
            }
        }
    }

    public void updateAuthorBlock(BlogRequest blogRequest) {
        try {
            String siteTitle = wpClient.getSiteTitle();
            
            // Generate the structured name
            String namePrompt = String.format(
                "Generate a professional full name for the head writer of a blog about %s. " +
                "The name should sound credible and trustworthy for this niche. " +
                "Return ONLY in this exact format:\n" +
                "FirstName: [first name]\n" +
                "LastName: [last name]",
                blogRequest.getTopic()
            );
            
            String nameResponse = openAIClient.callOpenAI(namePrompt).trim();
            String firstName = nameResponse.split("FirstName: ")[1].split("\n")[0].trim();
            String lastName = nameResponse.split("LastName: ")[1].trim();
            String authorName = firstName + " " + lastName;
            
            // Generate a job title
            String titlePrompt = String.format(
                "Generate a job title for the head writer of a blog about %s. " +
                "The title could be creative and fun but ensure it is professional and simple. " +
                "Return only the title, nothing else.",
                blogRequest.getTopic()
            );
            
            String jobTitle = openAIClient.callOpenAI(titlePrompt).trim();
            
            // Then, generate the bio with the author name
            String bioPrompt = String.format(
                "Generate a professional author bio for a blog.\n\n" +
                "**Blog Details**:\n" +
                "- Blog Name: %s\n" +
                "- Blog Topic: %s\n" +
                "- Author Name: %s\n" +
                "- Job Title: %s\n\n" +
                "**Bio Guidelines**:\n" +
                "- Write in third person\n" +
                "- Keep it professional yet approachable\n" +
                "- Focus on expertise and authority in the niche\n" +
                "- Length: 2 sentences\n" +
                "- Highlight experience and qualifications relevant to %s\n\n" +
                "Return only the bio, nothing else",
                siteTitle,
                blogRequest.getDescription(),
                authorName,
                jobTitle,
                blogRequest.getTopic()
            );
            
            String authorBio = openAIClient.callOpenAI(bioPrompt);
            
            // Generate and upload the author image
            String authorImageUrl = generateAndUploadAuthorImage(blogRequest, authorName);
            
            // Generate the author URL using the first name in lowercase
            
            // Update the reusable block with new content and author URL
            wpClient.updateAuthorBlock(authorName, jobTitle, authorBio, authorImageUrl);
            
            // Update the WordPress admin user
            WordPressSettingsClient settingsClient = new WordPressSettingsClient();
            settingsClient.updateAdminUser(
                firstName,    // First Name
                lastName,     // Last Name
                jobTitle,     // Job Title
                authorBio     // Bio
            );
                    
        } catch (Exception e) {
            System.err.println("Error updating author block: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String generateAuthorImagePrompt(String topic) throws IOException {
        String prompt = String.format(
            "For a blog about %s, create a hyper-realistic headshot of the most stereotypical person who could be the head blog writer. " +
            "The focus should be on a close-up of their face and shoulders. Include these elements:\n" +
            "• Age, build, and complexion\n" +
            "• Personal style and clothing (emphasize what would be visible in a headshot)\n" +
            "• Facial features, expression, and hairstyle\n" +
            "• Lighting and atmosphere focused on the face (e.g., soft, natural lighting)\n" +
            "• Small, realistic details like makeup, skin textures, or freckles that add authenticity\n\n" +
            "Keep the description under 500 characters.",
            topic
        );
        
        return openAIClient.callGPT4(prompt);
    }

    private String generateAndUploadAuthorImage(BlogRequest blogRequest, String authorName) throws IOException {
        // Generate the image description
        String imageDescription = generateAuthorImagePrompt(blogRequest.getTopic() + ". The image exhibits a high level of realism, reminiscent of modern portrait photography, in 4K UHD quality, suitable for a profile picture or official use");
        
        // Generate the image
        String imageUrl = getImgAIClient.generateImage(
            imageDescription,
            1024,
            1024,
            4,
            null
        );
        
        // Download image locally
        String localImagePath = "/Users/zachderhake/Desktop/author_" + authorName.replaceAll("\\s+", "_") + ".jpg";
        downloadImage(imageUrl, localImagePath);
        
        // Upload to WordPress
        return mediaClient.uploadMediaFromFile(localImagePath, "Author Image - " + authorName);
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

    public static void main(String[] args) {
        BlogLayoutService service = new BlogLayoutService();
        try {
            // List all FAQs first
            
            BlogRequest request = new BlogRequest("Y Combinator", "Y Combinator News");
            service.updateAuthorBlock(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}