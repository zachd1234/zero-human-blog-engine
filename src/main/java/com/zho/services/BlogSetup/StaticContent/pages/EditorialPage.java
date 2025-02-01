package com.zho.services.BlogSetup.StaticContent.pages;

import java.io.IOException;
import org.apache.hc.core5.http.ParseException;
import org.json.JSONObject;
import com.zho.model.BlogRequest;
import com.zho.model.Image;
import com.zho.api.OpenAIClient;
import com.zho.api.wordpress.WordPressBlockClient;
import java.util.List;

public class EditorialPage implements StaticPage {
    private final WordPressBlockClient wpClient;
    private final OpenAIClient openAIClient;
    private Integer pageId;
    private static final String PAGE_SLUG = "editorial-guidelines";

    public EditorialPage(WordPressBlockClient wpClient) {
        this.wpClient = wpClient;
        this.openAIClient = new OpenAIClient();
        try {
            JSONObject page = wpClient.getPageByUrl(PAGE_SLUG);
            this.pageId = page.getInt("id");
        } catch (Exception e) {
            System.err.println("Error getting editorial page ID: " + e.getMessage());
        }
    }

    @Override
    public String getPageName() {
        return "Editorial Guidelines";
    }

    @Override
    public void updateStaticContent(BlogRequest request, List<Image> images) throws IOException, ParseException {
        String siteTitle = wpClient.getSiteTitle();
        
        String prompt = String.format(
            "Generate a structured **Editorial Policy & Review Methodology** for a blog.\n\n" +
            "🔹 **Blog Details**:\n" +
            "- **Blog Name**: %s\n" +
            "- **Blog Topic**: %s\n" +
            "- **Blog Description**: %s\n\n" +
            
            "🔹 **Editorial Guidelines**:\n" +
            "- Clearly state **the commitment to accuracy, trust, and unbiased content**\n" +
            "- Explain how content is researched, written, and reviewed\n" +
            "- Include a **review methodology**, detailing product testing and expert input\n" +
            "- **Cite sources** relevant to %s for credibility\n\n" +
            
            "🔹 **Key Sections to Include**:\n" +
            "1️⃣ **Commitment to Accuracy & Trust** → A clear statement about editorial integrity\n" +
            "2️⃣ **Content Creation Process** → How articles, reviews, and guides are developed\n" +
            "3️⃣ **Review & Verification Standards** → How information is fact-checked and verified\n" +
            "4️⃣ **Sources & Citations** → Where information is sourced from\n" +
            "5️⃣ **Corrections & Updates** → The policy for content revisions\n" +
            "6️⃣ **Ethics & Independence Statement** → Transparency on affiliate links and sponsorships\n" +
            "7️⃣ **Contact & Feedback** → Mention that readers can visit the Contact page for inquiries (Don't link to it)\n\n" +
            
            "Format the content in clean HTML with proper headings (h2, h3) and paragraphs. " +
            "Do NOT include any DOCTYPE, html, head, body, or title tags.\n\n" +
            "Make it specific to %s content and industry standards.",
            
            siteTitle,                    // Blog Name
            request.getTopic(),           // Blog Topic
            request.getDescription(),     // Blog Description
            request.getTopic(),           // For citing sources
            request.getTopic()            // For final specificity
        );

        String guidelines = openAIClient.callOpenAI(prompt);
        wpClient.updatePageContent(getPageId(), guidelines);
    }

    @Override
    public int getRequiredImageCount() {
        return 0; // Update if images are needed
    }

    @Override
    public int getPageId() {
        return pageId != null ? pageId : -1;
    }
    
    public static void main(String[] args) {
        try {
            // Create test instances
            WordPressBlockClient wpClient = new WordPressBlockClient();
            EditorialPage editorialPage = new EditorialPage(wpClient);
            
            // Create a test BlogRequest
            BlogRequest request = new BlogRequest(
                "Rucking",
                "All things rucking. Guides, gear reviews, training plans, and expert tips to help you succeed with rucking"
            );
            
            // Print initial info
            System.out.println("Page ID: " + editorialPage.getPageId());
            System.out.println("Page Name: " + editorialPage.getPageName());
            
            // Update the content
            System.out.println("Updating editorial guidelines...");
            editorialPage.updateStaticContent(request, null);
            System.out.println("Update complete!");
            
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 