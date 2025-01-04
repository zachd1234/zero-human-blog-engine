package com.zho.services;

import com.zho.model.BlogRequest;
import com.zho.api.wordpress.WordPressMediaClient;
import com.zho.api.NounProjectClient;
import org.json.JSONObject;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.apache.hc.core5.http.ParseException;

public class LogoAndFaviconService {
    private final WordPressMediaClient mediaClient;
    private final NounProjectClient nounClient;

    public LogoAndFaviconService(WordPressMediaClient mediaClient) {
        this.mediaClient = mediaClient;
        this.nounClient = new NounProjectClient();
    }

    public void generateAndUploadBranding(BlogRequest request) throws IOException, ParseException {
        // Get icon from Noun Project
        JSONObject searchResult = nounClient.searchIcons(request.getTopic());
        
        if (searchResult.has("icons") && searchResult.getJSONArray("icons").length() > 0) {
            String iconUrl = searchResult.getJSONArray("icons")
                .getJSONObject(0)
                .getString("thumbnail_url");
            
            // Download and get the tinted icon
            byte[] iconData = nounClient.downloadIcon(iconUrl);
            
            // Convert byte array to BufferedImage
            BufferedImage iconImage = ImageIO.read(new ByteArrayInputStream(iconData));
            
            // Update favicon using the icon
            mediaClient.updateFavicon(iconImage);
        } else {
            throw new IOException("No icons found for topic: " + request.getTopic());
        }
    }
} 