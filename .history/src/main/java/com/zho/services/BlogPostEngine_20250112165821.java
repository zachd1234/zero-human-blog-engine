package com.zho.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BlogPostEngine {

    public String createNewBlogPost(String keyword) throws IOException {
        // First generate the outline
        String outline = generateOutline(keyword);
        System.out.println("Generated outline:\n" + outline);

        // Parse outline into sections (you'll need to implement this)
        List<String> sectionTitles = parseSectionTitles(outline);
        
        // Generate each section
        StringBuilder fullPost = new StringBuilder();
        for (String sectionTitle : sectionTitles) {
            System.out.println("Generating section: " + sectionTitle);
            String section = generateSection(sectionTitle, keyword);
            fullPost.append(section).append("\n\n");
        }

        return fullPost.toString();
    }

    private List<String> parseSectionTitles(String outline) {
        // Split outline into lines and extract section titles
        List<String> titles = new ArrayList<>();
        for (String line : outline.split("\n")) {
            if (line.matches("^\\d+\\..*")) {  // Lines starting with numbers
                titles.add(line.replaceFirst("^\\d+\\.\\s*", "").trim());
            }
        }
        return titles;
    }
}