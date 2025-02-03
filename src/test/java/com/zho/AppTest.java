package com.zho;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import com.zho.model.Site;

public class AppTest {

    @Before
    public void setUp() {
        // Any necessary setup before each test
    }

    @Test
    public void testGetCurrentSite() {
        Site currentSite = Site.getCurrentSite();
        assertNotNull("Current site should not be null", currentSite);
        assertTrue("Current site should be one of the defined sites", 
            currentSite == Site.MAIN || currentSite == Site.TEST);
    }

    @Test
    public void testGetSiteById_ValidId() {
        Site site = Site.getSitebyId(1); // Assuming ID 1 corresponds to MAIN
        assertNotNull("Site with ID 1 should not be null", site);
        assertEquals("Expected site should be MAIN", Site.MAIN, site);
    }

    @Test
    public void testGetSiteById_InvalidId() {
        Site site = Site.getSitebyId(999); // Assuming this ID does not exist
        assertNull("Site with ID 999 should be null", site);
    }

    @Test
    public void testSwitchSite() {
        Site initialSite = Site.getCurrentSite();
        Site newSite = Site.TEST; // Assuming TEST is a valid site
        Site.SwitchSite(newSite);
        
        Site currentSite = Site.getCurrentSite();
        assertEquals("Current site should be switched to TEST", newSite, currentSite);
        
        // Optionally switch back to the initial site
        Site.SwitchSite(initialSite);
    }

    @Test
    public void testSiteCount() {
        int expectedCount = 3; // Updated to match the actual number of sites
        int actualCount = Site.values().length;
        assertEquals("Total number of sites should match expected count", expectedCount, actualCount);
    }

    @Test
    public void testActiveSiteStatus() {
        Site activeSite = Site.getCurrentSite();
        assertTrue("Current site should be active", activeSite.isActive());
    }
}