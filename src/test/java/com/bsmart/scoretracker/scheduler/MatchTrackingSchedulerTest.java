package com.bsmart.scoretracker.scheduler;

import com.bsmart.scoretracker.model.Competition;
import com.bsmart.scoretracker.model.Match;
import com.bsmart.scoretracker.model.enums.MatchStatus;
import com.bsmart.scoretracker.model.enums.ProviderType;
import com.bsmart.scoretracker.model.Phase;
import com.bsmart.scoretracker.repository.MatchRepository;
import com.bsmart.scoretracker.service.TrackingEngineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchTrackingSchedulerTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private TrackingEngineService trackingEngine;

    @InjectMocks
    private MatchTrackingScheduler scheduler;

    private Competition competition;
    private Match liveMatch1;
    private Match liveMatch2;
    private Match halfTimeMatch;
    private Match scheduledMatchNear;
    private Match scheduledMatchFar;

    @BeforeEach
    void setUp() {
        competition = Competition.builder()
            .id(1L)
            .code("LIGUE1")
            .name("Ligue 1")
            .build();

        Phase phase = Phase.builder()
            .id(1L)
            .competition(competition)
            .name("Journée 1")
            .build();

        LocalDateTime now = LocalDateTime.now();

        liveMatch1 = Match.builder()
            .id(1L)
            .phase(phase)
            .homeTeam("PSG")
            .awayTeam("Lyon")
            .kickoffUtc(now.minusMinutes(30))
            .provider(ProviderType.ONE_FOOTBALL)
            .matchUrl("https://onefootball.com/match/1")
            .trackingEnabled(true)
            .status(MatchStatus.IN_PLAY)
            .build();

        liveMatch2 = Match.builder()
            .id(2L)
            .phase(phase)
            .homeTeam("Marseille")
            .awayTeam("Monaco")
            .kickoffUtc(now.minusMinutes(45))
            .provider(ProviderType.ONE_FOOTBALL)
            .matchUrl("https://onefootball.com/match/2")
            .trackingEnabled(true)
            .status(MatchStatus.IN_PLAY)
            .build();

        halfTimeMatch = Match.builder()
            .id(3L)
            .phase(phase)
            .homeTeam("Nice")
            .awayTeam("Lens")
            .kickoffUtc(now.minusMinutes(50))
            .provider(ProviderType.ONE_FOOTBALL)
            .matchUrl("https://onefootball.com/match/3")
            .trackingEnabled(true)
            .status(MatchStatus.HALF_TIME)
            .build();

        scheduledMatchNear = Match.builder()
            .id(4L)
            .phase(phase)
            .homeTeam("Lille")
            .awayTeam("Rennes")
            .kickoffUtc(now.plusMinutes(30))
            .provider(ProviderType.ONE_FOOTBALL)
            .matchUrl("https://onefootball.com/match/4")
            .trackingEnabled(true)
            .status(MatchStatus.SCHEDULED)
            .build();

        scheduledMatchFar = Match.builder()
            .id(5L)
            .phase(phase)
            .homeTeam("Nantes")
            .awayTeam("Toulouse")
            .kickoffUtc(now.plusHours(3))
            .provider(ProviderType.ONE_FOOTBALL)
            .matchUrl("https://onefootball.com/match/5")
            .trackingEnabled(true)
            .status(MatchStatus.SCHEDULED)
            .build();
    }

    @Test
    void testTrackLiveMatches_Success() {
        // Given
        when(matchRepository.findByTrackingEnabledTrueAndStatusIn(Arrays.asList(MatchStatus.IN_PLAY)))
            .thenReturn(Arrays.asList(liveMatch1, liveMatch2));
        doNothing().when(trackingEngine).trackMatch(any(Match.class));

        // When
        scheduler.trackLiveMatches();

        // Then
        verify(matchRepository).findByTrackingEnabledTrueAndStatusIn(Arrays.asList(MatchStatus.IN_PLAY));
        verify(trackingEngine).trackMatch(liveMatch1);
        verify(trackingEngine).trackMatch(liveMatch2);
        verify(trackingEngine, times(2)).trackMatch(any(Match.class));
    }

    @Test
    void testTrackLiveMatches_NoMatches() {
        // Given
        when(matchRepository.findByTrackingEnabledTrueAndStatusIn(Arrays.asList(MatchStatus.IN_PLAY)))
            .thenReturn(Collections.emptyList());

        // When
        scheduler.trackLiveMatches();

        // Then
        verify(matchRepository).findByTrackingEnabledTrueAndStatusIn(Arrays.asList(MatchStatus.IN_PLAY));
        verify(trackingEngine, never()).trackMatch(any());
    }

    @Test
    void testTrackLiveMatches_ErrorHandling() {
        // Given
        when(matchRepository.findByTrackingEnabledTrueAndStatusIn(Arrays.asList(MatchStatus.IN_PLAY)))
            .thenReturn(Arrays.asList(liveMatch1, liveMatch2));

        // First match throws exception
        doThrow(new RuntimeException("Scraping failed"))
            .when(trackingEngine).trackMatch(liveMatch1);
        doNothing().when(trackingEngine).trackMatch(liveMatch2);

        // When
        scheduler.trackLiveMatches();

        // Then - Should continue processing other matches
        verify(trackingEngine).trackMatch(liveMatch1);
        verify(trackingEngine).trackMatch(liveMatch2);
        verify(trackingEngine, times(2)).trackMatch(any(Match.class));
    }

    @Test
    void testTrackHalfTimeMatches_Success() {
        // Given
        when(matchRepository.findByTrackingEnabledTrueAndStatusIn(Arrays.asList(MatchStatus.HALF_TIME)))
            .thenReturn(Arrays.asList(halfTimeMatch));
        doNothing().when(trackingEngine).trackMatch(any(Match.class));

        // When
        scheduler.trackHalfTimeMatches();

        // Then
        verify(matchRepository).findByTrackingEnabledTrueAndStatusIn(Arrays.asList(MatchStatus.HALF_TIME));
        verify(trackingEngine).trackMatch(halfTimeMatch);
    }

    @Test
    void testTrackHalfTimeMatches_NoMatches() {
        // Given
        when(matchRepository.findByTrackingEnabledTrueAndStatusIn(Arrays.asList(MatchStatus.HALF_TIME)))
            .thenReturn(Collections.emptyList());

        // When
        scheduler.trackHalfTimeMatches();

        // Then
        verify(matchRepository).findByTrackingEnabledTrueAndStatusIn(Arrays.asList(MatchStatus.HALF_TIME));
        verify(trackingEngine, never()).trackMatch(any());
    }

    @Test
    void testTrackScheduledMatchesNearKickoff_Success() {
        // Given
        ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        when(matchRepository.findByTrackingEnabledTrueAndStatusAndKickoffUtcBetween(
            eq(MatchStatus.SCHEDULED), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(scheduledMatchNear));

        doNothing().when(trackingEngine).trackMatch(any(Match.class));

        // When
        scheduler.trackScheduledMatchesNearKickoff();

        // Then
        verify(matchRepository).findByTrackingEnabledTrueAndStatusAndKickoffUtcBetween(
            eq(MatchStatus.SCHEDULED), startCaptor.capture(), endCaptor.capture());
        verify(trackingEngine).trackMatch(scheduledMatchNear);

        // Verify time window (now-10min to now+1h)
        LocalDateTime start = startCaptor.getValue();
        LocalDateTime end = endCaptor.getValue();
        LocalDateTime now = LocalDateTime.now();

        // Allow some tolerance for test execution time (5 seconds)
        long minutesFromStart = java.time.Duration.between(start, now).toMinutes();
        long minutesToEnd = java.time.Duration.between(now, end).toMinutes();

        assertEquals(10, minutesFromStart, 1); // Should be ~10 minutes before now
        assertEquals(60, minutesToEnd, 1); // Should be ~60 minutes after now
    }

    @Test
    void testTrackScheduledMatchesNearKickoff_NoMatches() {
        // Given
        when(matchRepository.findByTrackingEnabledTrueAndStatusAndKickoffUtcBetween(
            eq(MatchStatus.SCHEDULED), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());

        // When
        scheduler.trackScheduledMatchesNearKickoff();

        // Then
        verify(trackingEngine, never()).trackMatch(any());
    }

    @Test
    void testTrackScheduledMatchesFarFromKickoff_Success() {
        // Given
        ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        when(matchRepository.findByTrackingEnabledTrueAndStatusAndKickoffUtcBetween(
            eq(MatchStatus.SCHEDULED), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(scheduledMatchFar));

        doNothing().when(trackingEngine).trackMatch(any(Match.class));

        // When
        scheduler.trackScheduledMatchesFarFromKickoff();

        // Then
        verify(matchRepository).findByTrackingEnabledTrueAndStatusAndKickoffUtcBetween(
            eq(MatchStatus.SCHEDULED), startCaptor.capture(), endCaptor.capture());
        verify(trackingEngine).trackMatch(scheduledMatchFar);

        // Verify time window (now+1h to now+24h)
        LocalDateTime start = startCaptor.getValue();
        LocalDateTime end = endCaptor.getValue();
        LocalDateTime now = LocalDateTime.now();

        long hoursToStart = java.time.Duration.between(now, start).toHours();
        long hoursToEnd = java.time.Duration.between(now, end).toHours();

        // Allow some tolerance due to test execution time
        assertTrue(hoursToStart >= 0 && hoursToStart <= 2,
            () -> "Start should be ~1 hour from now, got: " + hoursToStart);
        assertTrue(hoursToEnd >= 23 && hoursToEnd <= 25,
            () -> "End should be ~24 hours from now, got: " + hoursToEnd);
    }

    @Test
    void testTrackScheduledMatchesFarFromKickoff_NoMatches() {
        // Given
        when(matchRepository.findByTrackingEnabledTrueAndStatusAndKickoffUtcBetween(
            eq(MatchStatus.SCHEDULED), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());

        // When
        scheduler.trackScheduledMatchesFarFromKickoff();

        // Then
        verify(trackingEngine, never()).trackMatch(any());
    }

    @Test
    void testTrackScheduledMatchesFarFromKickoff_ErrorHandling() {
        // Given
        when(matchRepository.findByTrackingEnabledTrueAndStatusAndKickoffUtcBetween(
            eq(MatchStatus.SCHEDULED), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(scheduledMatchFar));

        doThrow(new RuntimeException("Network error"))
            .when(trackingEngine).trackMatch(scheduledMatchFar);

        // When
        scheduler.trackScheduledMatchesFarFromKickoff();

        // Then - Should not crash, just log error
        verify(trackingEngine).trackMatch(scheduledMatchFar);
    }

    @Test
    void testAllScheduledMethodsCallCorrectRepositoryQueries() {
        // Given
        when(matchRepository.findByTrackingEnabledTrueAndStatusIn(anyList()))
            .thenReturn(Collections.emptyList());
        when(matchRepository.findByTrackingEnabledTrueAndStatusAndKickoffUtcBetween(
            any(), any(), any()))
            .thenReturn(Collections.emptyList());

        // When
        scheduler.trackLiveMatches();
        scheduler.trackHalfTimeMatches();
        scheduler.trackScheduledMatchesNearKickoff();
        scheduler.trackScheduledMatchesFarFromKickoff();

        // Then
        verify(matchRepository, times(2)).findByTrackingEnabledTrueAndStatusIn(anyList());
        verify(matchRepository, times(2)).findByTrackingEnabledTrueAndStatusAndKickoffUtcBetween(
            any(), any(), any());
    }
}
