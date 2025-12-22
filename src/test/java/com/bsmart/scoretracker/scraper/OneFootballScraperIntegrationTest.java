package com.bsmart.scoretracker.scraper;

import com.bsmart.scoretracker.dto.MatchSnapshot;
import com.bsmart.scoretracker.scraper.providers.OneFootballScraperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests d'intégration pour OneFootballScraperProvider
 * Utilise des fixtures HTML au lieu de vraies pages web
 */
class OneFootballScraperIntegrationTest {

    @Mock
    private WebDriver webDriver;

    @Mock
    private WebElement bodyElement;

    private OneFootballScraperProvider scraper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        scraper = new OneFootballScraperProvider(webDriver);
    }

    @Test
    @DisplayName("Scraping OneFootball: Match en cours (45')")
    void testScrapeOneLiveMatch() throws IOException {
        // Charger le HTML fixture
        String htmlContent = loadFixture("fixtures/onefootball-live-match.html");

        // Mock WebDriver behavior
        when(webDriver.getPageSource()).thenReturn(htmlContent);
        when(webDriver.findElement(By.tagName("body"))).thenReturn(bodyElement);

        // Execute scraping
        MatchSnapshot result = scraper.fetch("https://onefootball.com/test");

        // Assertions
        assertTrue(result.isFound());
        assertEquals("LIVE", result.getStatus());
        assertEquals(1, result.getHome()); // Note: Inverted in scraper
        assertEquals(0, result.getAway());
        assertEquals("45'", result.getMinute());

        // Verify WebDriver was called
        verify(webDriver).get("https://onefootball.com/test");
        verify(webDriver, atLeastOnce()).getPageSource();
    }

    @Test
    @DisplayName("Scraping OneFootball: Match terminé (Full time)")
    void testScrapeOneFinishedMatch() throws IOException {
        // Charger le HTML fixture
        String htmlContent = loadFixture("fixtures/onefootball-finished-match.html");

        // Mock WebDriver behavior
        when(webDriver.getPageSource()).thenReturn(htmlContent);
        when(webDriver.findElement(By.tagName("body"))).thenReturn(bodyElement);

        // Execute scraping
        MatchSnapshot result = scraper.fetch("https://onefootball.com/test");

        // Assertions
        assertTrue(result.isFound());
        assertEquals("FT", result.getStatus());
        assertEquals(2, result.getHome()); // Note: Inverted in scraper
        assertEquals(0, result.getAway());
        assertEquals("Full time", result.getMinute());
    }

    @Test
    @DisplayName("Scraping OneFootball: Erreur réseau")
    void testScrapeOneFootballNetworkError() {
        // Simuler une erreur réseau
        when(webDriver.getPageSource()).thenThrow(new RuntimeException("Network error"));

        // Execute scraping - should throw ScraperException
        assertThrows(Exception.class, () -> {
            scraper.fetch("https://onefootball.com/test");
        });
    }

    /**
     * Charge un fichier fixture HTML
     */
    private String loadFixture(String path) throws IOException {
        return Files.readString(
            Paths.get("src/test/resources/" + path)
        );
    }
}
