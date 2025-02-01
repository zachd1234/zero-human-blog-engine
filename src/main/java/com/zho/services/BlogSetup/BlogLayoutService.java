package com.zho.services.BlogSetup;

import com.zho.api.OpenAIClient;
import com.zho.api.wordpress.WordPressBlockClient;
import com.zho.model.BlogRequest;
import com.zho.model.Site;

public class BlogLayoutService {
    private final OpenAIClient openAIClient;
    private final WordPressBlockClient wpClient;

    public BlogLayoutService() {
        this.openAIClient = new OpenAIClient();
        this.wpClient = new WordPressBlockClient();
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

    public static void main(String[] args) {
        BlogLayoutService service = new BlogLayoutService();
        try {
            // List all FAQs first
            
            BlogRequest request = new BlogRequest("Rucking", "All things rucking. Guides, gear reviews, training plans, and expert tips to help you succeed with rucking");
            service.updateCommitmentAccordion(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}