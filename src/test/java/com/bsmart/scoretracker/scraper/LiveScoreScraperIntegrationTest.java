package com.bsmart.scoretracker.scraper;

import com.bsmart.scoretracker.dto.MatchSnapshot;
import com.bsmart.scoretracker.scraper.providers.LiveScoreScraperProvider;
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
 * Tests d'intégration pour LiveScoreScraperProvider
 * Utilise des fixtures HTML au lieu de vraies pages web
 */
class LiveScoreScraperIntegrationTest {

    @Mock
    private WebDriver webDriver;

    @Mock
    private WebElement bodyElement;

    private LiveScoreScraperProvider scraper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        scraper = new LiveScoreScraperProvider(webDriver);
    }

    @Test
    @DisplayName("Scraping LiveScore: Match en cours (50')")
    void testScrapeLiveMatch() throws IOException {
        // Charger le HTML fixture
        String htmlContent = loadFixture("fixtures/livescore-live-match.html");

        // Mock WebDriver behavior
        when(webDriver.getPageSource()).thenReturn(htmlContent);
        when(webDriver.findElement(By.tagName("body"))).thenReturn(bodyElement);

        // Execute scraping
        MatchSnapshot result = scraper.fetch("https://livescore.com/test");

        // Assertions
        assertTrue(result.isFound());
        assertEquals("LIVE", result.getStatus());
        assertEquals(1, result.getHome());
        assertEquals(0, result.getAway());
        assertEquals("50'", result.getMinute());

        // Verify WebDriver was called
        verify(webDriver).get("https://livescore.com/test");
        verify(webDriver, atLeastOnce()).getPageSource();
    }

    @Test
    @DisplayName("Scraping LiveScore: Match terminé (Full time)")
    void testScrapeFinishedMatch() throws IOException {
        // Charger le HTML fixture
        String htmlContent = loadFixture("fixtures/livescore-finished-match.html");

        // Mock WebDriver behavior
        when(webDriver.getPageSource()).thenReturn(htmlContent);
        when(webDriver.findElement(By.tagName("body"))).thenReturn(bodyElement);

        // Execute scraping
        MatchSnapshot result = scraper.fetch("https://livescore.com/test");

        // Assertions
        assertTrue(result.isFound());
        assertEquals("FT", result.getStatus());
        assertEquals(2, result.getHome());
        assertEquals(1, result.getAway());
        assertEquals("Full time", result.getMinute());
    }

    @Test
    @DisplayName("Scraping LiveScore: Mi-temps (Half time)")
    void testScrapeHalftimeMatch() throws IOException {
        // Charger le HTML fixture
        String htmlContent = loadFixture("fixtures/livescore-halftime.html");

        // Mock WebDriver behavior
        when(webDriver.getPageSource()).thenReturn(htmlContent);
        when(webDriver.findElement(By.tagName("body"))).thenReturn(bodyElement);

        // Execute scraping
        MatchSnapshot result = scraper.fetch("https://livescore.com/test");

        // Assertions
        assertTrue(result.isFound());
        assertEquals("HT", result.getStatus());
        assertEquals(1, result.getHome());
        assertEquals(0, result.getAway());
        assertEquals("Half time", result.getMinute());
    }

    @Test
    @DisplayName("BUG FIX: EventScheduled mais minute='59' → Doit détecter LIVE")
    void testEventScheduledButMinuteShowsLive() throws IOException {
        // Charger le HTML fixture qui reproduit le bug
        // eventStatus="EventScheduled" mais status="59'" et score=1-0
        String htmlContent = loadFixture("fixtures/livescore-scheduled-but-live.html");

        // Mock WebDriver behavior
        when(webDriver.getPageSource()).thenReturn(htmlContent);
        when(webDriver.findElement(By.tagName("body"))).thenReturn(bodyElement);

        // Execute scraping
        MatchSnapshot result = scraper.fetch("https://livescore.com/test");

        // Assertions - Le fix doit prioriser la minute sur eventStatus
        assertTrue(result.isFound());
        assertEquals("LIVE", result.getStatus(),
            "Le status doit être LIVE car minute='59' même si eventStatus='EventScheduled'");
        assertEquals(1, result.getHome());
        assertEquals(0, result.getAway());
        assertEquals("59'", result.getMinute());
    }

    @Test
    @DisplayName("Scraping LiveScore: Erreur réseau")
    void testScrapeLiveScoreNetworkError() {
        // Simuler une erreur réseau
        when(webDriver.getPageSource()).thenThrow(new RuntimeException("Network error"));

        // Execute scraping - should throw ScraperException
        assertThrows(Exception.class, () -> {
            scraper.fetch("https://livescore.com/test");
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
