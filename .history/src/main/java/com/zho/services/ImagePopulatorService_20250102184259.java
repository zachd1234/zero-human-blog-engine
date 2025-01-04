package com.zho.services;

import com.zho.model.BlogRequest;
import com.zho.api.UnsplashClient;
import com.zho.api.wordpress.WordPressMediaClient;
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

    public void populateImages(BlogRequest request) throws IOException, ParseException {

    }
} 