package com.bsmart.scoretracker.service;

import com.bsmart.scoretracker.model.enums.MatchStatus;
import com.bsmart.scoretracker.model.enums.ProviderType;
import com.bsmart.scoretracker.service.impl.TrackingEngineServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour la logique métier du TrackingEngine
 * PAS de dépendances externes (pas de Selenium, pas de WebDriver)
 */
class TrackingEngineLogicTest {

    private TrackingEngineServiceImpl trackingEngine;

    @BeforeEach
    void setUp() {
        // On testera les méthodes publiques via reflection ou en extrayant la logique
        // Pour l'instant, on va créer des tests pour normalizeStatus qui est public
        trackingEngine = new TrackingEngineServiceImpl(null, null, null);
    }

    @Test
    @DisplayName("OneFootball: 'LIVE' → IN_PLAY")
    void testNormalizeOneFootballLiveStatus() {
        MatchStatus result = trackingEngine.normalizeStatus("LIVE", ProviderType.ONE_FOOTBALL);
        assertEquals(MatchStatus.IN_PLAY, result);
    }

    @Test
    @DisplayName("OneFootball: 'FT' → FINISHED")
    void testNormalizeOneFootballFinishedStatus() {
        MatchStatus result = trackingEngine.normalizeStatus("FT", ProviderType.ONE_FOOTBALL);
        assertEquals(MatchStatus.FINISHED, result);
    }

    @Test
    @DisplayName("OneFootball: 'Full time' → FINISHED")
    void testNormalizeOneFootballFullTimeStatus() {
        MatchStatus result = trackingEngine.normalizeStatus("Full time", ProviderType.ONE_FOOTBALL);
        assertEquals(MatchStatus.FINISHED, result);
    }

    @Test
    @DisplayName("OneFootball: 'HT' → PAUSED")
    void testNormalizeOneFootballHalfTimeStatus() {
        MatchStatus result = trackingEngine.normalizeStatus("HT", ProviderType.ONE_FOOTBALL);
        assertEquals(MatchStatus.PAUSED, result);
    }

    @Test
    @DisplayName("OneFootball: '45' (minute) → IN_PLAY")
    void testNormalizeOneFootballMinuteStatus() {
        MatchStatus result = trackingEngine.normalizeStatus("45'", ProviderType.ONE_FOOTBALL);
        assertEquals(MatchStatus.IN_PLAY, result);
    }

    @Test
    @DisplayName("LiveScore: 'LIVE' → IN_PLAY")
    void testNormalizeLiveScoreLiveStatus() {
        MatchStatus result = trackingEngine.normalizeStatus("LIVE", ProviderType.LIVE_SCORE);
        assertEquals(MatchStatus.IN_PLAY, result);
    }

    @Test
    @DisplayName("LiveScore: 'FINISHED' → FINISHED")
    void testNormalizeLiveScoreFinishedStatus() {
        MatchStatus result = trackingEngine.normalizeStatus("FINISHED", ProviderType.LIVE_SCORE);
        assertEquals(MatchStatus.FINISHED, result);
    }

    @Test
    @DisplayName("LiveScore: 'HALFTIME' → PAUSED")
    void testNormalizeLiveScoreHalfTimeStatus() {
        MatchStatus result = trackingEngine.normalizeStatus("HALFTIME", ProviderType.LIVE_SCORE);
        assertEquals(MatchStatus.PAUSED, result);
    }

    @Test
    @DisplayName("Status null → SCHEDULED")
    void testNormalizeNullStatus() {
        MatchStatus result = trackingEngine.normalizeStatus(null, ProviderType.ONE_FOOTBALL);
        assertEquals(MatchStatus.SCHEDULED, result);
    }

    @Test
    @DisplayName("Status inconnu → SCHEDULED")
    void testNormalizeUnknownStatus() {
        MatchStatus result = trackingEngine.normalizeStatus("UNKNOWN_STATUS", ProviderType.ONE_FOOTBALL);
        assertEquals(MatchStatus.SCHEDULED, result);
    }
}
