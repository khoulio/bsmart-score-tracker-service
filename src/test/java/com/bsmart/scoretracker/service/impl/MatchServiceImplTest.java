package com.bsmart.scoretracker.service.impl;

import com.bsmart.scoretracker.dto.MatchDTO;
import com.bsmart.scoretracker.exception.ResourceNotFoundException;
import com.bsmart.scoretracker.model.Competition;
import com.bsmart.scoretracker.model.Match;
import com.bsmart.scoretracker.model.Phase;
import com.bsmart.scoretracker.model.enums.MatchStatus;
import com.bsmart.scoretracker.model.enums.ProviderType;
import com.bsmart.scoretracker.repository.PhaseRepository;
import com.bsmart.scoretracker.repository.MatchRepository;
import com.bsmart.scoretracker.service.TrackingEngineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchServiceImplTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private PhaseRepository phaseRepository;

    @Mock
    private TrackingEngineService trackingEngineService;

    @InjectMocks
    private MatchServiceImpl matchService;

    private Competition competition;
    private Phase phase;
    private Match match1;
    private Match match2;

    @BeforeEach
    void setUp() {
        competition = Competition.builder()
            .id(1L)
            .code("LIGUE1")
            .name("Ligue 1")
            .country("France")
            .build();

        phase = Phase.builder()
            .id(1L)
            .competition(competition)
            .name("Journée 1")
            .trackingEnabled(true)
            .build();

        match1 = Match.builder()
            .id(1L)
            .phase(phase)
            .homeTeam("PSG")
            .awayTeam("Lyon")
            .kickoffUtc(LocalDateTime.now().plusDays(1))
            .provider(ProviderType.ONE_FOOTBALL)
            .matchUrl("https://onefootball.com/match/1")
            .trackingEnabled(true)
            .status(MatchStatus.SCHEDULED)
            .errorCount(0)
            .build();

        match2 = Match.builder()
            .id(2L)
            .phase(phase)
            .homeTeam("Marseille")
            .awayTeam("Monaco")
            .kickoffUtc(LocalDateTime.now().plusDays(2))
            .provider(ProviderType.ONE_FOOTBALL)
            .matchUrl("https://onefootball.com/match/2")
            .trackingEnabled(true)
            .status(MatchStatus.IN_PLAY)
            .scoreHome(1)
            .scoreAway(0)
            .minute("35")
            .errorCount(0)
            .build();
    }

    @Test
    void testGetAllMatches() {
        // Given
        when(matchRepository.findAll()).thenReturn(Arrays.asList(match1, match2));

        // When
        List<MatchDTO> result = matchService.getAllMatches();

        // Then
        assertEquals(2, result.size());
        assertEquals("PSG", result.get(0).getHomeTeam());
        assertEquals("Marseille", result.get(1).getHomeTeam());
        verify(matchRepository).findAll();
    }

    @Test
    void testGetMatchesByPhase() {
        // Given
        when(matchRepository.findByPhaseIdOrderByKickoffUtcAsc(1L)).thenReturn(Arrays.asList(match1, match2));

        // When
        List<MatchDTO> result = matchService.getMatchesByPhase(1L);

        // Then
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getPhaseId());
        verify(matchRepository).findByPhaseIdOrderByKickoffUtcAsc(1L);
    }

    @Test
    void testGetMatchesByStatus() {
        // Given
        when(matchRepository.findByStatus(MatchStatus.IN_PLAY)).thenReturn(Arrays.asList(match2));

        // When
        List<MatchDTO> result = matchService.getMatchesByStatus(MatchStatus.IN_PLAY);

        // Then
        assertEquals(1, result.size());
        assertEquals(MatchStatus.IN_PLAY, result.get(0).getStatus());
        assertEquals(1, result.get(0).getScoreHome());
        verify(matchRepository).findByStatus(MatchStatus.IN_PLAY);
    }

    @Test
    void testGetMatchById_Success() {
        // Given
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match1));

        // When
        MatchDTO result = matchService.getMatchById(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("PSG", result.getHomeTeam());
        assertEquals("Lyon", result.getAwayTeam());
        verify(matchRepository).findById(1L);
    }

    @Test
    void testGetMatchById_NotFound() {
        // Given
        when(matchRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> matchService.getMatchById(999L));
        verify(matchRepository).findById(999L);
    }

    @Test
    void testCreateMatch_Success() {
        // Given
        MatchDTO dto = MatchDTO.builder()
            .phaseId(1L)
            .homeTeam("Nice")
            .awayTeam("Lens")
            .kickoffUtc(LocalDateTime.now().plusDays(3))
            .provider(ProviderType.ONE_FOOTBALL)
            .matchUrl("https://onefootball.com/match/3")
            .trackingEnabled(true)
            .build();

        Match newMatch = Match.builder()
            .id(3L)
            .phase(phase)
            .homeTeam("Nice")
            .awayTeam("Lens")
            .kickoffUtc(dto.getKickoffUtc())
            .provider(dto.getProvider())
            .matchUrl(dto.getMatchUrl())
            .trackingEnabled(true)
            .status(MatchStatus.SCHEDULED)
            .build();

        when(phaseRepository.findById(1L)).thenReturn(Optional.of(phase));
        when(matchRepository.save(any(Match.class))).thenReturn(newMatch);

        // When
        MatchDTO result = matchService.createMatch(dto);

        // Then
        assertNotNull(result);
        assertEquals(3L, result.getId());
        assertEquals("Nice", result.getHomeTeam());
        assertEquals(MatchStatus.SCHEDULED, result.getStatus());
        verify(phaseRepository).findById(1L);
        verify(matchRepository).save(any(Match.class));
    }

    @Test
    void testCreateMatch_PhaseNotFound() {
        // Given
        MatchDTO dto = MatchDTO.builder()
            .phaseId(999L)
            .homeTeam("Nice")
            .awayTeam("Lens")
            .build();

        when(phaseRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> matchService.createMatch(dto));
        verify(phaseRepository).findById(999L);
        verify(matchRepository, never()).save(any());
    }

    @Test
    void testCreateMatch_DefaultTrackingEnabled() {
        // Given - DTO without trackingEnabled
        MatchDTO dto = MatchDTO.builder()
            .phaseId(1L)
            .homeTeam("Nice")
            .awayTeam("Lens")
            .kickoffUtc(LocalDateTime.now().plusDays(3))
            .provider(ProviderType.ONE_FOOTBALL)
            .matchUrl("https://onefootball.com/match/3")
            .build();

        when(phaseRepository.findById(1L)).thenReturn(Optional.of(phase));
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> {
            Match m = invocation.getArgument(0);
            assertTrue(m.getTrackingEnabled());
            return m;
        });

        // When
        matchService.createMatch(dto);

        // Then
        verify(matchRepository).save(argThat(match -> match.getTrackingEnabled() == true));
    }

    @Test
    void testUpdateMatch_Success() {
        // Given
        MatchDTO dto = MatchDTO.builder()
            .phaseId(1L)
            .homeTeam("PSG Updated")
            .awayTeam("Lyon Updated")
            .kickoffUtc(LocalDateTime.now().plusDays(5))
            .provider(ProviderType.LIVE_SCORE)
            .matchUrl("https://livescore.com/match/1")
            .trackingEnabled(false)
            .build();

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match1));
        when(matchRepository.save(any(Match.class))).thenReturn(match1);

        // When
        MatchDTO result = matchService.updateMatch(1L, dto);

        // Then
        assertEquals("PSG Updated", match1.getHomeTeam());
        assertEquals("Lyon Updated", match1.getAwayTeam());
        assertEquals(ProviderType.LIVE_SCORE, match1.getProvider());
        assertFalse(match1.getTrackingEnabled());
        verify(matchRepository).findById(1L);
        verify(matchRepository).save(match1);
    }

    @Test
    void testUpdateMatch_ChangePhase() {
        // Given
        Phase newPhase = Phase.builder()
            .id(2L)
            .competition(competition)
            .name("Journée 2")
            .build();

        MatchDTO dto = MatchDTO.builder()
            .phaseId(2L)
            .homeTeam("PSG")
            .awayTeam("Lyon")
            .kickoffUtc(match1.getKickoffUtc())
            .provider(match1.getProvider())
            .matchUrl(match1.getMatchUrl())
            .trackingEnabled(true)
            .build();

        when(matchRepository.findById(1L)).thenReturn(Optional.of(match1));
        when(phaseRepository.findById(2L)).thenReturn(Optional.of(newPhase));
        when(matchRepository.save(any(Match.class))).thenReturn(match1);

        // When
        matchService.updateMatch(1L, dto);

        // Then
        assertEquals(newPhase, match1.getPhase());
        verify(phaseRepository).findById(2L);
    }

    @Test
    void testUpdateMatch_NotFound_CreatesMatch() {
        // Given
        MatchDTO dto = MatchDTO.builder()
            .homeTeam("PSG")
            .awayTeam("Lyon")
            .matchUrl("https://onefootball.com/match/1")
            .trackingEnabled(true)
            .build();
        when(matchRepository.findById(999L)).thenReturn(Optional.empty());
        when(phaseRepository.findAll()).thenReturn(List.of(phase));
        Match saved = Match.builder().id(10L).phase(phase).build();
        when(matchRepository.save(any(Match.class))).thenReturn(saved);

        // When
        MatchDTO result = matchService.updateMatch(999L, dto);

        // Then
        assertEquals(10L, result.getId());
        verify(matchRepository).findById(999L);
        verify(phaseRepository).findAll();
        verify(matchRepository).save(any(Match.class));
    }

    @Test
    void testDeleteMatch_Success() {
        // Given
        when(matchRepository.existsById(1L)).thenReturn(true);

        // When
        matchService.deleteMatch(1L);

        // Then
        verify(matchRepository).existsById(1L);
        verify(matchRepository).deleteById(1L);
    }

    @Test
    void testDeleteMatch_NotFound() {
        // Given
        when(matchRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> matchService.deleteMatch(999L));
        verify(matchRepository).existsById(999L);
        verify(matchRepository, never()).deleteById(anyLong());
    }

    @Test
    void testEnableTracking_Success() {
        // Given
        match1.setTrackingEnabled(false);
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match1));
        when(matchRepository.save(any(Match.class))).thenReturn(match1);

        // When
        matchService.enableTracking(1L);

        // Then
        assertTrue(match1.getTrackingEnabled());
        verify(matchRepository).findById(1L);
        verify(matchRepository).save(match1);
    }

    @Test
    void testEnableTracking_NotFound() {
        // Given
        when(matchRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> matchService.enableTracking(999L));
    }

    @Test
    void testDisableTracking_Success() {
        // Given
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match1));
        when(matchRepository.save(any(Match.class))).thenReturn(match1);

        // When
        matchService.disableTracking(1L);

        // Then
        assertFalse(match1.getTrackingEnabled());
        verify(matchRepository).findById(1L);
        verify(matchRepository).save(match1);
    }

    @Test
    void testDisableTracking_NotFound() {
        // Given
        when(matchRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> matchService.disableTracking(999L));
    }

    @Test
    void testRefreshMatch_Success() {
        // Given
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match1));
        when(matchRepository.save(any(Match.class))).thenReturn(match1);
        doNothing().when(trackingEngineService).trackMatch(match1);

        // When
        MatchDTO result = matchService.refreshMatch(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(matchRepository).findById(1L);
        verify(trackingEngineService).trackMatch(match1);
        verify(matchRepository).save(match1);
    }

    @Test
    void testRefreshMatch_NotFound() {
        // Given
        when(matchRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> matchService.refreshMatch(999L));
        verify(trackingEngineService, never()).trackMatch(any());
    }

    @Test
    void testDeleteFinishedMatches_Success() {
        // Given
        Match finishedMatch = Match.builder()
            .id(3L)
            .phase(phase)
            .homeTeam("Team A")
            .awayTeam("Team B")
            .kickoffUtc(LocalDateTime.now().minusDays(1))
            .provider(ProviderType.ONE_FOOTBALL)
            .matchUrl("https://onefootball.com/match/3")
            .trackingEnabled(false)
            .status(MatchStatus.FINISHED)
            .errorCount(0)
            .build();

        List<Match> finishedMatches = Arrays.asList(finishedMatch);
        when(matchRepository.findByStatus(MatchStatus.FINISHED)).thenReturn(finishedMatches);
        doNothing().when(matchRepository).deleteAll(anyList());

        // When
        matchService.deleteFinishedMatches();

        // Then
        verify(matchRepository, times(1)).findByStatus(MatchStatus.FINISHED);
        verify(matchRepository, times(1)).deleteAll(finishedMatches);
    }
}
