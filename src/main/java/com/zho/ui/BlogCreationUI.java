package com.zho.ui;

import java.util.Scanner;
import com.zho.business.BlogCreationService;
import com.zho.model.BlogRequest;

public class BlogCreationUI {
    private final Scanner scanner;
    private final BlogCreationService blogService;

    public BlogCreationUI() {
        this.scanner = new Scanner(System.in);
        this.blogService = new BlogCreationService();
    }

    public void start() {
        System.out.println("Welcome to Blog Creator!");
        System.out.println("------------------------");
        
        System.out.print("Enter your blog topic: ");
        String topic = scanner.nextLine();
        
        System.out.print("Enter a description of your blog: ");
        String description = scanner.nextLine();
        
        BlogRequest request = new BlogRequest(topic, description);
        
        try {
            blogService.AutoBlogEngine(request);
            System.out.println("Blog created successfully!");
        } catch (Exception e) {
            System.err.println("Error creating blog: " + e.getMessage());
        }
    }
} 