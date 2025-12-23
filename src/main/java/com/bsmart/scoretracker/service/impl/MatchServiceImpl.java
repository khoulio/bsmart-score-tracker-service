package com.bsmart.scoretracker.service.impl;

import com.bsmart.scoretracker.dto.MatchDTO;
import com.bsmart.scoretracker.exception.ResourceNotFoundException;
import com.bsmart.scoretracker.model.Match;
import com.bsmart.scoretracker.model.MatchEvent;
import com.bsmart.scoretracker.model.Phase;
import com.bsmart.scoretracker.model.enums.EventType;
import com.bsmart.scoretracker.model.enums.MatchStatus;
import com.bsmart.scoretracker.repository.MatchRepository;
import com.bsmart.scoretracker.repository.PhaseRepository;
import com.bsmart.scoretracker.service.MatchService;
import com.bsmart.scoretracker.service.TrackingEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchServiceImpl implements MatchService {

    private final MatchRepository matchRepository;
    private final PhaseRepository phaseRepository;
    private final TrackingEngineService trackingEngineService;

    @Override
    @Transactional(readOnly = true)
    public List<MatchDTO> getAllMatches() {
        return matchRepository.findAll().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchDTO> getMatchesByPhase(Long phaseId) {
        return matchRepository.findByPhaseIdOrderByKickoffUtcAsc(phaseId).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchDTO> getMatchesByStatus(MatchStatus status) {
        return matchRepository.findByStatus(status).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MatchDTO getMatchById(Long id) {
        Match match = matchRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Match", id));
        return toDTO(match);
    }

    @Override
    @Transactional(readOnly = true)
    public MatchDTO getMatchByExternalId(Long externalId) {
        Match match = matchRepository.findByExternalId(externalId)
            .orElseThrow(() -> new ResourceNotFoundException("Match with externalId", externalId));
        return toDTO(match);
    }

    @Override
    @Transactional
    public MatchDTO createMatch(MatchDTO dto) {
        Phase phase = phaseRepository.findById(dto.getPhaseId())
            .orElseThrow(() -> new ResourceNotFoundException("Phase", dto.getPhaseId()));

        Match match = Match.builder()
            .phase(phase)
            .externalId(dto.getExternalId())
            .homeTeam(dto.getHomeTeam())
            .awayTeam(dto.getAwayTeam())
            .kickoffUtc(dto.getKickoffUtc())
            .provider(dto.getProvider())
            .matchUrl(dto.getMatchUrl())
            .trackingEnabled(dto.getTrackingEnabled() != null ? dto.getTrackingEnabled() : true)
            .status(MatchStatus.SCHEDULED)
            .build();

        Match saved = matchRepository.save(match);
        log.info("Created match {}: {} vs {}", saved.getId(), saved.getHomeTeam(), saved.getAwayTeam());
        return toDTO(saved);
    }

    @Override
    @Transactional
    public MatchDTO updateMatch(Long id, MatchDTO dto) {
        Match match = matchRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Match", id));

        if (dto.getPhaseId() != null && !dto.getPhaseId().equals(match.getPhase().getId())) {
            Phase phase = phaseRepository.findById(dto.getPhaseId())
                .orElseThrow(() -> new ResourceNotFoundException("Phase", dto.getPhaseId()));
            match.setPhase(phase);
        }

        if (dto.getExternalId() != null) {
            match.setExternalId(dto.getExternalId());
        }
        match.setHomeTeam(dto.getHomeTeam());
        match.setAwayTeam(dto.getAwayTeam());
        match.setKickoffUtc(dto.getKickoffUtc());
        match.setProvider(dto.getProvider());
        match.setMatchUrl(dto.getMatchUrl());
        match.setTrackingEnabled(dto.getTrackingEnabled());

        Match updated = matchRepository.save(match);
        log.info("Updated match {}", updated.getId());
        return toDTO(updated);
    }

    @Override
    @Transactional
    public void deleteMatch(Long id) {
        if (!matchRepository.existsById(id)) {
            throw new ResourceNotFoundException("Match", id);
        }
        matchRepository.deleteById(id);
        log.info("Deleted match {}", id);
    }

    @Override
    @Transactional
    public void enableTracking(Long id) {
        Match match = matchRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Match", id));

        match.setTrackingEnabled(true);
        matchRepository.save(match);
        log.info("Enabled tracking for match {}", id);
    }

    @Override
    @Transactional
    public void disableTracking(Long id) {
        Match match = matchRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Match", id));

        match.setTrackingEnabled(false);
        matchRepository.save(match);
        log.info("Disabled tracking for match {}", id);
    }

    @Override
    @Transactional
    public MatchDTO refreshMatch(Long id) {
        Match match = matchRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Match", id));

        trackingEngineService.trackMatch(match);
        matchRepository.save(match);

        log.info("Manually refreshed match {}", id);
        return toDTO(match);
    }

    @Override
    @Transactional
    public MatchDTO manualUpdate(Long id, Integer scoreHome, Integer scoreAway, MatchStatus status, Integer scoreHomeTAB, Integer scoreAwayTAB) {
        Match match = matchRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Match", id));

        log.info("Manually updating match {} ({} vs {}). New state: Score {} - {}, Status {}, Penalties: {} - {}",
                id, match.getHomeTeam(), match.getAwayTeam(), scoreHome, scoreAway, status, scoreHomeTAB, scoreAwayTAB);

        MatchEvent event = MatchEvent.builder()
                .match(match)
                .eventType(EventType.MANUAL_UPDATE)
                .oldStatus(match.getStatus())
                .newStatus(status)
                .oldScoreHome(match.getScoreHome())
                .oldScoreAway(match.getScoreAway())
                .newScoreHome(scoreHome)
                .newScoreAway(scoreAway)
                .triggeredBy("MANUAL")
                .build();
        match.getEvents().add(event);

        match.setScoreHome(scoreHome);
        match.setScoreAway(scoreAway);
        match.setStatus(status);

        // Set penalty shootout scores
        match.setScoreHomeTAB(scoreHomeTAB);
        match.setScoreAwayTAB(scoreAwayTAB);
        // Determine winners if penalties are provided and final score is draw
        if (scoreHomeTAB != null && scoreAwayTAB != null && scoreHome != null && scoreAway != null && scoreHome.equals(scoreAway)) {
            match.setWinnerHomeTAB(scoreHomeTAB > scoreAwayTAB);
            match.setWinnerAwayTAB(scoreAwayTAB > scoreHomeTAB);
        } else {
            match.setWinnerHomeTAB(null);
            match.setWinnerAwayTAB(null);
        }

        // When manually updating, we should probably reset any anti-flapping/candidate logic
        // to ensure the manual state sticks without being overridden by the next scrape.
        match.setStatusCandidate(null);
        match.setConsecutiveSameCandidate(0);
        match.setStatusCandidateSinceUtc(null);

        // Also, let's mark tracking as disabled to prevent the scraper from overwriting the manual data.
        // The user can re-enable it if needed.
        match.setTrackingEnabled(false);
        log.warn("Tracking for match {} has been disabled due to manual update.", id);

        Match updated = matchRepository.save(match);
        return toDTO(updated);
    }

    private MatchDTO toDTO(Match match) {
        return MatchDTO.builder()
            .id(match.getId())
            .phaseId(match.getPhase().getId())
            .phaseName(match.getPhase().getName())
            .competitionName(match.getPhase().getCompetition().getName())
            .homeTeam(match.getHomeTeam())
            .awayTeam(match.getAwayTeam())
            .kickoffUtc(match.getKickoffUtc())
            .provider(match.getProvider())
            .matchUrl(match.getMatchUrl())
            .trackingEnabled(match.getTrackingEnabled())
            .status(match.getStatus())
            .scoreHome(match.getScoreHome())
            .scoreAway(match.getScoreAway())
            .minute(match.getMinute())
            .rawStatus(match.getRawStatus())
            .lastFetchUtc(match.getLastFetchUtc())
            .errorCount(match.getErrorCount())
            .lastError(match.getLastError())
            .createdAt(match.getCreatedAt())
            .updatedAt(match.getUpdatedAt())
            // Sync fields
            .externalId(match.getExternalId())
            .teamDomicileId(match.getTeamDomicileId())
            .teamExterieurId(match.getTeamExterieurId())
            .isProlongationEnabled(match.getIsProlongationEnabled())
            .scoreHomeTAB(match.getScoreHomeTAB())
            .scoreAwayTAB(match.getScoreAwayTAB())
            .winnerHomeTAB(match.getWinnerHomeTAB())
            .winnerAwayTAB(match.getWinnerAwayTAB())
            .isMonetized(match.getIsMonetized())
            .isHalfTimeSend(match.getIsHalfTimeSend())
            .isEndHalfTimeSend(match.getIsEndHalfTimeSend())
            .isForTest(match.getIsForTest())
            .externalScoreProvider(match.getExternalScoreProvider())
            .lastSyncAt(match.getLastSyncAt())
            // Anti-flapping fields
            .statusCandidate(match.getStatusCandidate())
            .consecutiveSameCandidate(match.getConsecutiveSameCandidate())
            .statusCandidateSinceUtc(match.getStatusCandidateSinceUtc())
            .halfTimeSeen(match.getHalfTimeSeen())
            .build();
    }
}
