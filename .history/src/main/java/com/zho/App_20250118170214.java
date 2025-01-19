package com.zho;
import com.zho.ui.BlogCreationUI;

public class App {    

    
    

    public static void main(String[] args) {
        try {
            BlogCreationUI ui = new BlogCreationUI();
            ui.start();
        } catch (Exception e) {
            System.err.println("Error starting application: " + e.getMessage());
            e.printStackTrace();
        }
    }
}