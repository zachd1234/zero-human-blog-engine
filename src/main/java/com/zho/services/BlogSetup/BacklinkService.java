package com.zho.services.BlogSetup;

public class BacklinkService {
    /**
     * Given blog topic → urls
     * Given blog topic → template
     * Given urls → email and personalization 
     * Given emails, personalization and template, assemble emails 
     * Given assembled emails, send them 
     * When there is a response, negotiate and have tools at disposal to either call a human or write a post
     */

     public String[] getUrls(String blogTopic) {
        return new String[] {
            "https://www.google.com",
            "https://www.yahoo.com",
            "https://www.bing.com"
        };
     }
     
     
}

