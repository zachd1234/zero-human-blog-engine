package com.zho.content;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.color.ColorSpace;

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
        int width = 1000;
        int height = 300;
        BufferedImage logo = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = logo.createGraphics();
        
        // Set up rendering hints
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Draw white background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);
        
        // Draw icon in teal color (#049F82)
        ColorConvertOp tealFilter = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_sRGB), null);
        BufferedImage tintedIcon = tealFilter.filter(icon, null);
        g2d.setColor(new Color(0x04, 0x9F, 0x82));  // Teal color
        g2d.drawImage(tintedIcon, 20, 20, 240, 240, null);
        
        // Draw text in dark gray (#222222)
        g2d.setColor(new Color(0x22, 0x22, 0x22));  // Dark gray
        g2d.setFont(new Font("Inherit", Font.BOLD, 72));
        FontMetrics fm = g2d.getFontMetrics();
        int textY = (height - fm.getHeight()) / 2 + fm.getAscent();
        g2d.drawString(blogName, 280, textY);
        
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