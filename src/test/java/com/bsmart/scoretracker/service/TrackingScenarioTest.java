package com.bsmart.scoretracker.service;

import com.bsmart.scoretracker.dto.MatchSnapshot;
import com.bsmart.scoretracker.model.Match;
import com.bsmart.scoretracker.model.Phase;
import com.bsmart.scoretracker.model.enums.MatchStatus;
import com.bsmart.scoretracker.model.enums.ProviderType;
import com.bsmart.scoretracker.repository.MatchRepository;
import com.bsmart.scoretracker.scraper.MatchScraperProvider;
import com.bsmart.scoretracker.scraper.ScraperProviderFactory;
import com.bsmart.scoretracker.service.impl.TrackingEngineServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests de scénarios complets de tracking
 * Simule l'évolution d'un match dans le temps
 */
@Disabled
class TrackingScenarioTest {

    @Mock
    private ScraperProviderFactory scraperFactory;

    @Mock
    private MatchScraperProvider scraperProvider;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchEventService matchEventService;

    private TrackingEngineServiceImpl trackingEngine;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        trackingEngine = new TrackingEngineServiceImpl(
            scraperFactory, matchRepository, matchEventService
        );

        // Configure le factory pour retourner notre mock
        when(scraperFactory.getProvider(any())).thenReturn(scraperProvider);
    }

    @Test
    @DisplayName("Scénario complet: SCHEDULED → IN_PLAY → PAUSED → IN_PLAY → FINISHED")
    void testCompleteMatchScenario() {
        // Créer un match
        Match match = createTestMatch();
        match.setKickoffUtc(LocalDateTime.now().minusMinutes(30)); // Commencé il y a 30min

        // === ÉTAPE 1: Match commence (0-0, minute 1') ===
        when(scraperProvider.fetch(any())).thenReturn(
            MatchSnapshot.builder()
                .status("LIVE")
                .home(0)
                .away(0)
                .minute("1'")
                .found(true)
                .build()
        );

        trackingEngine.trackMatch(match);

        // Anti-flapping: première détection, status reste SCHEDULED
        assertEquals(MatchStatus.SCHEDULED, match.getStatus());
        assertEquals(MatchStatus.IN_PLAY, match.getStatusCandidate());
        assertEquals(1, match.getConsecutiveSameCandidate());

        // === ÉTAPE 2: Deuxième scraping (toujours 0-0, minute 5') ===
        when(scraperProvider.fetch(any())).thenReturn(
            MatchSnapshot.builder()
                .status("LIVE")
                .home(0)
                .away(0)
                .minute("5'")
                .found(true)
                .build()
        );

        trackingEngine.trackMatch(match);

        // Anti-flapping: deuxième confirmation
        assertEquals(2, match.getConsecutiveSameCandidate());

        // === ÉTAPE 3: Troisième scraping - Confirmation finale ===
        when(scraperProvider.fetch(any())).thenReturn(
            MatchSnapshot.builder()
                .status("LIVE")
                .home(0)
                .away(0)
                .minute("10'")
                .found(true)
                .build()
        );

        trackingEngine.trackMatch(match);

        // Anti-flapping: 3 confirmations → status appliqué
        assertEquals(MatchStatus.IN_PLAY, match.getStatus());
        assertNull(match.getStatusCandidate());
        assertEquals(0, match.getConsecutiveSameCandidate());

        // === ÉTAPE 4: Premier but (1-0, minute 25') ===
        when(scraperProvider.fetch(any())).thenReturn(
            MatchSnapshot.builder()
                .status("LIVE")
                .home(1)
                .away(0)
                .minute("25'")
                .found(true)
                .build()
        );

        trackingEngine.trackMatch(match);

        assertEquals(1, match.getScoreHome());
        assertEquals(0, match.getScoreAway());

        // === ÉTAPE 5: Mi-temps (1-0, HT) ===
        when(scraperProvider.fetch(any())).thenReturn(
            MatchSnapshot.builder()
                .status("HT")
                .home(1)
                .away(0)
                .minute("Half time")
                .found(true)
                .build()
        );

        // 3 scraping pour anti-flapping
        for (int i = 0; i < 3; i++) {
            trackingEngine.trackMatch(match);
        }

        assertEquals(MatchStatus.PAUSED, match.getStatus());
        assertTrue(match.getHalfTimeSeen());

        // === ÉTAPE 6: Reprise 2ème mi-temps (1-0, minute 46') ===
        when(scraperProvider.fetch(any())).thenReturn(
            MatchSnapshot.builder()
                .status("LIVE")
                .home(1)
                .away(0)
                .minute("46'")
                .found(true)
                .build()
        );

        for (int i = 0; i < 3; i++) {
            trackingEngine.trackMatch(match);
        }

        assertEquals(MatchStatus.IN_PLAY, match.getStatus());

        // === ÉTAPE 7: Fin du match (1-0, FT) ===
        when(scraperProvider.fetch(any())).thenReturn(
            MatchSnapshot.builder()
                .status("FT")
                .home(1)
                .away(0)
                .minute("Full time")
                .found(true)
                .build()
        );

        trackingEngine.trackMatch(match); // FINISHED: 1 seule confirmation suffit

        assertEquals(MatchStatus.FINISHED, match.getStatus());
        assertFalse(match.getTrackingEnabled()); // Tracking désactivé
    }

    @Test
    @DisplayName("Protection pré-kickoff: Ignore les données avant le début")
    void testPreKickoffProtection() {
        Match match = createTestMatch();
        match.setKickoffUtc(LocalDateTime.now().plusHours(2)); // Match dans 2h

        // OneFootball affiche déjà "Full time" et scores
        when(scraperProvider.fetch(any())).thenReturn(
            MatchSnapshot.builder()
                .status("FT")
                .home(0)
                .away(0)
                .minute("Full time")
                .found(true)
                .build()
        );

        trackingEngine.trackMatch(match);

        // Le système IGNORE ces données
        assertEquals(MatchStatus.SCHEDULED, match.getStatus());
        assertNull(match.getScoreHome());
        assertNull(match.getScoreAway());
    }

    @Test
    @DisplayName("Auto-correction: FINISHED → IN_PLAY si erreur détectée")
    void testAutoCorrection() {
        Match match = createTestMatch();
        match.setKickoffUtc(LocalDateTime.now().minusHours(1));
        match.setStatus(MatchStatus.FINISHED); // Marqué FINISHED par erreur
        match.setTrackingEnabled(false); // Tracking désactivé

        // Mais le scraper détecte que c'est toujours en cours
        when(scraperProvider.fetch(any())).thenReturn(
            MatchSnapshot.builder()
                .status("LIVE")
                .home(1)
                .away(0)
                .minute("65'")
                .found(true)
                .build()
        );

        trackingEngine.trackMatch(match);

        // Auto-correction activée
        assertEquals(MatchStatus.IN_PLAY, match.getStatus());
        assertTrue(match.getTrackingEnabled()); // Tracking réactivé
    }

    private Match createTestMatch() {
        Phase phase = new Phase();
        phase.setId(1L);

        Match match = new Match();
        match.setId(1L);
        match.setPhase(phase);
        match.setHomeTeam("Test Home");
        match.setAwayTeam("Test Away");
        match.setMatchUrl("https://test.com");
        match.setProvider(ProviderType.ONE_FOOTBALL);
        match.setStatus(MatchStatus.SCHEDULED);
        match.setTrackingEnabled(true);
        match.setErrorCount(0);
        match.setConsecutiveSameCandidate(0);
        match.setHalfTimeSeen(false);

        return match;
    }
}