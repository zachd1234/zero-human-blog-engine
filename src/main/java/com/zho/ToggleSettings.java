package com.zho;

import com.zho.model.Site;

public class ToggleSettings {


    public static void switchSite(Site newSite) {
        Site.SwitchSite(newSite);
    }
    
    public static void main(String[] args) {
       //ACTIVATE THE METHODS YOU WISH TO USE
        switchSite(Site.MAIN);

    }
}
