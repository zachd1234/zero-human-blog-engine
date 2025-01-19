package com.zho;
import java.sql.SQLException;

import com.zho.services.DatabaseService;
import com.zho.ui.BlogCreationUI;

public class App {    

    
    private static void toggleBlogStatus(String[] args) {
        DatabaseService db = new DatabaseService();
        try {
            db.toggleBlogStatus();
        } catch (SQLException e) {
            System.err.println("Error toggling blog status: " + e.getMessage());
            e.printStackTrace();
        }
    }

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