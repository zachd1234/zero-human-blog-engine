package com.zho.services;

import com.zho.model.BlogRequest;
import com.zho.api.unsplash.UnsplashClient;
import com.zho.model.UnsplashImage;
import org.json.JSONObject;
import java.io.IOException;
import org.apache.hc.core5.http.ParseException;

public class ImagePopulatorService {
    private final UnsplashClient unsplashClient;
    private final WordPressMediaClient mediaClient;

    public ImagePopulatorService(UnsplashClient unsplashClient, WordPressMediaClient mediaClient) {
        this.unsplashClient = unsplashClient;
        this.mediaClient = mediaClient;
    }

    public UnsplashImage findAndUploadHeaderImage(BlogRequest request) throws IOException, ParseException {
        String searchQuery = request.getTopic() + " " + request.getDescription();
        UnsplashImage image = unsplashClient.searchAndGetRandomImage(searchQuery);
        
        if (image != null) {
            int mediaId = mediaClient.uploadImageFromUrl(image.getUrl(), image.getAlt());
            image.setWordPressMediaId(mediaId);
        }
        
        return image;
    }

    public void populateImages(BlogRequest request) throws IOException, ParseException {
        UnsplashImage headerImage = findAndUploadHeaderImage(request);
        // Add more image population tasks as needed
    }
} 