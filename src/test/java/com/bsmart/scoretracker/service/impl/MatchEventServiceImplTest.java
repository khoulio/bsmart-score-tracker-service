package com.bsmart.scoretracker.service.impl;

import com.bsmart.scoretracker.dto.MatchEventDTO;
import com.bsmart.scoretracker.model.Competition;
import com.bsmart.scoretracker.model.Match;
import com.bsmart.scoretracker.model.MatchEvent;
import com.bsmart.scoretracker.model.Phase;
import com.bsmart.scoretracker.model.enums.EventType;
import com.bsmart.scoretracker.model.enums.MatchStatus;
import com.bsmart.scoretracker.model.enums.ProviderType;
import com.bsmart.scoretracker.repository.MatchEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchEventServiceImplTest {

    @Mock
    private MatchEventRepository matchEventRepository;

    @InjectMocks
    private MatchEventServiceImpl matchEventService;

    private Match match;
    private MatchEvent event1;
    private MatchEvent event2;

    @BeforeEach
    void setUp() {
        Competition competition = Competition.builder()
            .id(1L)
            .code("LIGUE1")
            .name("Ligue 1")
            .build();

        Phase phase = Phase.builder()
            .id(1L)
            .competition(competition)
            .name("Journée 1")
            .build();

        match = Match.builder()
            .id(1L)
            .phase(phase)
            .homeTeam("PSG")
            .awayTeam("Lyon")
            .provider(ProviderType.ONE_FOOTBALL)
            .status(MatchStatus.IN_PLAY)
            .build();

        event1 = MatchEvent.builder()
            .id(1L)
            .match(match)
            .timestamp(LocalDateTime.now().minusMinutes(10))
            .eventType(EventType.STATUS_CHANGE)
            .oldStatus(MatchStatus.SCHEDULED)
            .newStatus(MatchStatus.IN_PLAY)
            .triggeredBy("SCHEDULER")
            .build();

        event2 = MatchEvent.builder()
            .id(2L)
            .match(match)
            .timestamp(LocalDateTime.now().minusMinutes(5))
            .eventType(EventType.SCORE_CHANGE)
            .oldScoreHome(0)
            .oldScoreAway(0)
            .newScoreHome(1)
            .newScoreAway(0)
            .minute("23")
            .triggeredBy("SCHEDULER")
            .build();
    }

    @Test
    void testGetEventsByMatch() {
        // Given
        when(matchEventRepository.findByMatchIdOrderByTimestampDesc(1L))
            .thenReturn(Arrays.asList(event2, event1));

        // When
        List<MatchEventDTO> result = matchEventService.getEventsByMatch(1L);

        // Then
        assertEquals(2, result.size());
        assertEquals(EventType.SCORE_CHANGE, result.get(0).getEventType());
        assertEquals(EventType.STATUS_CHANGE, result.get(1).getEventType());
        assertEquals(1L, result.get(0).getMatchId());
        verify(matchEventRepository).findByMatchIdOrderByTimestampDesc(1L);
    }

    @Test
    void testCreateEvent_StatusChange() {
        // Given
        ArgumentCaptor<MatchEvent> eventCaptor = ArgumentCaptor.forClass(MatchEvent.class);
        when(matchEventRepository.save(any(MatchEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        matchEventService.createEvent(
            match,
            EventType.STATUS_CHANGE,
            MatchStatus.SCHEDULED,
            MatchStatus.IN_PLAY,
            null, null, null, null,
            null,
            "LIVE",
            "SCHEDULER"
        );

        // Then
        verify(matchEventRepository).save(eventCaptor.capture());
        MatchEvent capturedEvent = eventCaptor.getValue();

        assertEquals(match, capturedEvent.getMatch());
        assertEquals(EventType.STATUS_CHANGE, capturedEvent.getEventType());
        assertEquals(MatchStatus.SCHEDULED, capturedEvent.getOldStatus());
        assertEquals(MatchStatus.IN_PLAY, capturedEvent.getNewStatus());
        assertEquals("LIVE", capturedEvent.getRawStatus());
        assertEquals("SCHEDULER", capturedEvent.getTriggeredBy());
        // Note: timestamp is set via @PrePersist, which only runs during actual persistence
        // In this test with mocked repository, timestamp will be null
    }

    @Test
    void testCreateEvent_ScoreChange() {
        // Given
        ArgumentCaptor<MatchEvent> eventCaptor = ArgumentCaptor.forClass(MatchEvent.class);
        when(matchEventRepository.save(any(MatchEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        matchEventService.createEvent(
            match,
            EventType.SCORE_CHANGE,
            null, null,
            0, 0, 1, 0,
            "23",
            "LIVE",
            "SCHEDULER"
        );

        // Then
        verify(matchEventRepository).save(eventCaptor.capture());
        MatchEvent capturedEvent = eventCaptor.getValue();

        assertEquals(EventType.SCORE_CHANGE, capturedEvent.getEventType());
        assertEquals(0, capturedEvent.getOldScoreHome());
        assertEquals(0, capturedEvent.getOldScoreAway());
        assertEquals(1, capturedEvent.getNewScoreHome());
        assertEquals(0, capturedEvent.getNewScoreAway());
        assertEquals("23", capturedEvent.getMinute());
    }

    @Test
    void testCreateEvent_TrackingEnabled() {
        // Given
        when(matchEventRepository.save(any(MatchEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        matchEventService.createEvent(
            match,
            EventType.TRACKING_ENABLED,
            null, null,
            null, null, null, null,
            null, null,
            "ADMIN"
        );

        // Then
        verify(matchEventRepository).save(argThat(event ->
            event.getEventType() == EventType.TRACKING_ENABLED &&
            event.getTriggeredBy().equals("ADMIN")
        ));
    }

    @Test
    void testCreateEvent_TrackingDisabled() {
        // Given
        when(matchEventRepository.save(any(MatchEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        matchEventService.createEvent(
            match,
            EventType.TRACKING_DISABLED,
            null, null,
            null, null, null, null,
            null, null,
            "ADMIN"
        );

        // Then
        verify(matchEventRepository).save(argThat(event ->
            event.getEventType() == EventType.TRACKING_DISABLED
        ));
    }

    @Test
    void testCreateEvent_ErrorDetected() {
        // Given
        when(matchEventRepository.save(any(MatchEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        matchEventService.createEvent(
            match,
            EventType.ERROR_DETECTED,
            null, null,
            null, null, null, null,
            null,
            "Network timeout",
            "SCHEDULER"
        );

        // Then
        verify(matchEventRepository).save(argThat(event ->
            event.getEventType() == EventType.ERROR_DETECTED &&
            event.getRawStatus().equals("Network timeout")
        ));
    }

    @Test
    void testCreateEvent_ManualRefresh() {
        // Given
        when(matchEventRepository.save(any(MatchEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        matchEventService.createEvent(
            match,
            EventType.MANUAL_REFRESH,
            null, null,
            null, null, null, null,
            null, null,
            "USER"
        );

        // Then
        verify(matchEventRepository).save(argThat(event ->
            event.getEventType() == EventType.MANUAL_REFRESH &&
            event.getTriggeredBy().equals("USER")
        ));
    }

    @Test
    void testCreateEvent_AntiFlappingActivated() {
        // Given
        when(matchEventRepository.save(any(MatchEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        matchEventService.createEvent(
            match,
            EventType.ANTI_FLAPPING_ACTIVATED,
            MatchStatus.SCHEDULED,
            MatchStatus.IN_PLAY,
            null, null, null, null,
            null,
            "Candidate set: IN_PLAY",
            "SCHEDULER"
        );

        // Then
        verify(matchEventRepository).save(argThat(event ->
            event.getEventType() == EventType.ANTI_FLAPPING_ACTIVATED &&
            event.getRawStatus().contains("Candidate set")
        ));
    }

    @Test
    void testToDTOMapping() {
        // Given
        when(matchEventRepository.findByMatchIdOrderByTimestampDesc(1L))
            .thenReturn(Arrays.asList(event1));

        // When
        List<MatchEventDTO> result = matchEventService.getEventsByMatch(1L);

        // Then
        assertEquals(1, result.size());
        MatchEventDTO dto = result.get(0);

        assertEquals(event1.getId(), dto.getId());
        assertEquals(event1.getMatch().getId(), dto.getMatchId());
        assertEquals(event1.getEventType(), dto.getEventType());
        assertEquals(event1.getOldStatus(), dto.getOldStatus());
        assertEquals(event1.getNewStatus(), dto.getNewStatus());
        assertEquals(event1.getTriggeredBy(), dto.getTriggeredBy());
        assertNotNull(dto.getTimestamp());
    }
}
