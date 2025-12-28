package com.bsmart.scoretracker.scraper;

import com.bsmart.scoretracker.scraper.providers.LiveScoreScraperProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class LiveScoreScraperParsingTest {

    private final LiveScoreScraperProvider scraper =
        new LiveScoreScraperProvider(() -> null);

    @Test
    @DisplayName("Parsing LiveScore: Penalty shootout scores are extracted")
    void testExtractPenaltyScoresFromFixture() throws Exception {
        String htmlContent = loadFixture("fixtures/livescore-penalty-shootout.html");

        String minute = (String) invokePrivate(scraper, "extractMinuteFromPage",
            new Class<?>[]{String.class}, htmlContent);
        String status = (String) invokePrivate(scraper, "extractStatusFromPage",
            new Class<?>[]{String.class, String.class}, htmlContent, minute);
        Integer penaltyHome = (Integer) invokePrivate(scraper, "extractPenaltyHomeScoreFromPage",
            new Class<?>[]{String.class}, htmlContent);
        Integer penaltyAway = (Integer) invokePrivate(scraper, "extractPenaltyAwayScoreFromPage",
            new Class<?>[]{String.class}, htmlContent);

        assertEquals("Pen", minute);
        assertEquals("LIVE", status);
        assertNotNull(penaltyHome);
        assertNotNull(penaltyAway);
        assertEquals(0, penaltyHome);
        assertEquals(1, penaltyAway);
    }

    @Test
    @DisplayName("Parsing LiveScore: AET → FT, ET → LIVE")
    void testExtractExtraTimeStatus() throws Exception {
        String aetPageSource = "{\"status\":\"AET\",\"eventStatus\":\"EventScheduled\"}";
        String etPageSource = "{\"status\":\"ET\",\"eventStatus\":\"EventScheduled\"}";

        String aetMinute = (String) invokePrivate(scraper, "extractMinuteFromPage",
            new Class<?>[]{String.class}, aetPageSource);
        String aetStatus = (String) invokePrivate(scraper, "extractStatusFromPage",
            new Class<?>[]{String.class, String.class}, aetPageSource, aetMinute);

        String etMinute = (String) invokePrivate(scraper, "extractMinuteFromPage",
            new Class<?>[]{String.class}, etPageSource);
        String etStatus = (String) invokePrivate(scraper, "extractStatusFromPage",
            new Class<?>[]{String.class, String.class}, etPageSource, etMinute);

        assertEquals("AET", aetMinute);
        assertEquals("FT", aetStatus);
        assertEquals("ET", etMinute);
        assertEquals("LIVE", etStatus);
    }

    private static Object invokePrivate(Object target, String methodName,
                                        Class<?>[] types, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, types);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static String loadFixture(String path) throws IOException {
        return Files.readString(Paths.get("src/test/resources/" + path));
    }
}
