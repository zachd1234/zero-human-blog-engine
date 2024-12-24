package com.zho.content;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class LogoGenerator {
    private final IconFetcher iconFetcher;
    private final AIContentGenerator ai;
    
    public LogoGenerator() throws Exception {
        this.iconFetcher = new IconFetcher();
        this.ai = new AIContentGenerator();
    }
    
    public BufferedImage generateLogo(String topic) throws Exception {
        // Generate blog name
        String blogName = ai.generateBlogName(topic);
        System.out.println("Generated blog name: " + blogName);
        
        // Get icon and create logo
        BufferedImage icon = iconFetcher.fetchRelatedIcon(topic);
        
        // Create combined logo
        int width = 1000; // Increased width to accommodate larger text and icon
        int height = 300; // Increased height to accommodate larger text and icon
        BufferedImage logo = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = logo.createGraphics();
        
        // Set up rendering hints
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Draw white background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);
        
        // Draw icon
        g2d.drawImage(icon, 20, 20, 240, 240, null); // Increased icon size
        
        // Draw text
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 72)); // Increased font size
        FontMetrics fm = g2d.getFontMetrics();
        int textY = (height - fm.getHeight()) / 2 + fm.getAscent();
        g2d.drawString(blogName, 280, textY); // Adjusted text position to accommodate larger icon
        
        g2d.dispose();
        return logo;
    }
    
    // Test method
    public static void main(String[] args) {
        try {
            LogoGenerator generator = new LogoGenerator();
            String[] topics = {"coffee brewing", "digital marketing", "travel photography"};
            
            for (String topic : topics) {
                System.out.println("\nGenerating logo for topic: " + topic);
                BufferedImage logo = generator.generateLogo(topic);
                
                String fileName = topic.replace(" ", "-") + "-logo.png";
                String desktopPath = System.getProperty("user.home") + "/Desktop/" + fileName;
                ImageIO.write(logo, "png", new File(desktopPath));
                System.out.println("Logo saved to: " + desktopPath);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}