package com.zho;

import java.sql.SQLException;
import com.zho.services.DatabaseService;

public class ToggleBlog {

    public static void main(String[] args) {
        DatabaseService db = new DatabaseService();
        try {
            db.toggleBlogStatus();
            System.out.println(" Blog Status: " + db.isBlogActive());
        } catch (SQLException e) {
            System.err.println("Error toggling blog status: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
