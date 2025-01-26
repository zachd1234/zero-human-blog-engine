package com.zho;

import java.sql.SQLException;
import com.zho.services.DatabaseService;
import com.zho.model.Site;

public class ToggleSettings {


    public static void switchSite(Site newSite) {
        Site.SwitchSite(newSite);
    }
    
    public static void ToggleBlogStatus() {
        DatabaseService db = new DatabaseService();
        try {
            db.toggleBlogStatus();
            System.out.println(" Blog Status: " + db.isBlogActive());
        } catch (SQLException e) {
            System.err.println("Error toggling blog status: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
       //ACTIVATE THE METHODS YOU WISH TO USE
        //ToggleBlogStatus();
        switchSite(Site.MAIN);
    }
}
