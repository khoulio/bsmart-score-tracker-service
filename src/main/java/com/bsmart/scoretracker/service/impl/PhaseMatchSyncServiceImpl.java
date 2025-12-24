package com.bsmart.scoretracker.service.impl;

import com.bsmart.scoretracker.client.WecanpronoMatchApiClient;
import com.bsmart.scoretracker.dto.external.ExternalMatchDTO;
import com.bsmart.scoretracker.dto.external.ExternalPhaseDTO;
import com.bsmart.scoretracker.model.Competition;
import com.bsmart.scoretracker.model.Match;
import com.bsmart.scoretracker.model.Phase;
import com.bsmart.scoretracker.model.enums.MatchStatus;
import com.bsmart.scoretracker.model.enums.ProviderType;
import com.bsmart.scoretracker.repository.CompetitionRepository;
import com.bsmart.scoretracker.repository.MatchRepository;
import com.bsmart.scoretracker.repository.PhaseRepository;
import com.bsmart.scoretracker.service.PhaseMatchSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhaseMatchSyncServiceImpl implements PhaseMatchSyncService {

    private final WecanpronoMatchApiClient matchApiClient;
    private final CompetitionRepository competitionRepository;
    private final PhaseRepository phaseRepository;
    private final MatchRepository matchRepository;






    @Override
    @Transactional
    public int synchronizeAllSyncedCompetitions() {
        log.info("Synchronizing all competitions with externalId");

        List<Competition> syncedCompetitions = competitionRepository.findAll().stream()
                .filter(c -> c.getExternalId() != null)
                .toList();

        int totalSynced = 0;


        log.info("Total phases synchronized across all competitions: {}", totalSynced);
        return totalSynced;
    }

    private void syncPhase(Competition competition, ExternalPhaseDTO externalPhase) {
        // Find or create Phase
        Optional<Phase> phaseOpt = phaseRepository.findByCompetitionIdAndName(
                competition.getId(), externalPhase.getName());

        Phase phase = phaseOpt.orElseGet(() -> {
            Phase newPhase = Phase.builder()
                    .competition(competition)
                    .name(externalPhase.getName())
                    .trackingEnabled(true)
                    .build();
            return phaseRepository.save(newPhase);
        });

        log.debug("Syncing matches from phase: {} to phase entity ID: {}",
                  externalPhase.getName(), phase.getId());

        if (externalPhase.getRencontres() != null && !externalPhase.getRencontres().isEmpty()) {
            syncMatches(phase, externalPhase.getRencontres());
        }
    }

    private void syncMatches(Phase phase, List<ExternalMatchDTO> externalMatches) {
        for (ExternalMatchDTO externalMatch : externalMatches) {
            try {
                syncMatch(phase, externalMatch);
            } catch (Exception e) {
                log.error("Error syncing match {} for phase {}: {}",
                        externalMatch.getId(), phase.getId(), e.getMessage());
            }
        }
    }

    private void syncMatch(Phase phase, ExternalMatchDTO externalMatch) {
        // Find or create match
        Optional<Match> existingOpt = matchRepository.findByPhaseIdAndExternalId(
                phase.getId(), externalMatch.getId());

        Match match = existingOpt.orElse(new Match());
        boolean isNewMatch = match.getId() == null;

        match.setPhase(phase);
        match.setExternalId(externalMatch.getId());

        // Team names
        String homeTeam = externalMatch.getTeamDomicile() != null
                ? externalMatch.getTeamDomicile().getClearName()
                : "Team " + externalMatch.getTeamDomicileId();
        String awayTeam = externalMatch.getTeamExterieur() != null
                ? externalMatch.getTeamExterieur().getClearName()
                : "Team " + externalMatch.getTeamExterieurId();

        match.setHomeTeam(homeTeam);
        match.setAwayTeam(awayTeam);
        match.setTeamDomicileId(externalMatch.getTeamDomicileId());
        match.setTeamExterieurId(externalMatch.getTeamExterieurId());

        // Kickoff date
        if (externalMatch.getDate() != null) {
            try {
                OffsetDateTime offsetDateTime = OffsetDateTime.parse(externalMatch.getDate(),
                        DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                match.setKickoffUtc(offsetDateTime.toLocalDateTime());
            } catch (Exception e) {
                log.warn("Could not parse date for match {}: {}", externalMatch.getId(), externalMatch.getDate());
            }
        }

        // Status mapping
        MatchStatus status = mapExternalStatus(externalMatch.getStatus(), externalMatch.getIsEnd(), externalMatch.getIsBegin());
        match.setStatus(status);

        // Scores - ONLY update from external if match is synced (not scraped)
        if (externalMatch.getScoreTeamDomicile() != null) {
            match.setScoreHome(externalMatch.getScoreTeamDomicile());
        }
        if (externalMatch.getScoreTeamExterieur() != null) {
            match.setScoreAway(externalMatch.getScoreTeamExterieur());
        }

        // External sync fields
        match.setIsProlongationEnabled(externalMatch.getIsProlongationEnabled());
        match.setIsMonetized(externalMatch.getIsMonetized());
        match.setIsHalfTimeSend(externalMatch.getIsHalfTimeSend());
        match.setIsEndHalfTimeSend(externalMatch.getIsEndHalfTimeSend());
        match.setIsForTest(externalMatch.getIsForTest());
        match.setExternalScoreProvider(externalMatch.getScoreProvider());
        match.setLastSyncAt(LocalDateTime.now());

        // For new matches, set default provider and URL
        if (isNewMatch) {
            match.setProvider(ProviderType.ONE_FOOTBALL); // Default provider
            match.setMatchUrl("https://onefootball.com"); // Will be updated manually or by scraper config
            match.setTrackingEnabled(false); // Disabled by default until URL is configured
        }

        matchRepository.save(match);
        log.debug("Synced match: {} vs {}", homeTeam, awayTeam);
    }

    private MatchStatus mapExternalStatus(String externalStatus, Boolean isEnd, Boolean isBegin) {
        if (externalStatus == null) {
            return MatchStatus.SCHEDULED;
        }

        return switch (externalStatus.toUpperCase()) {
            case "FINISHED" -> MatchStatus.FINISHED;
            case "IN_PLAY", "LIVE" -> MatchStatus.IN_PLAY;
            case "HALF_TIME", "HALFTIME" -> MatchStatus.PAUSED;
            default -> {
                if (Boolean.TRUE.equals(isEnd)) {
                    yield MatchStatus.FINISHED;
                } else if (Boolean.TRUE.equals(isBegin)) {
                    yield MatchStatus.IN_PLAY;
                } else {
                    yield MatchStatus.SCHEDULED;
                }
            }
        };
    }

}
