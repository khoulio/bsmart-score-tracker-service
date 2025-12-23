package com.bsmart.scoretracker.scheduler;

import com.bsmart.scoretracker.model.Match;
import com.bsmart.scoretracker.model.enums.MatchStatus;
import com.bsmart.scoretracker.repository.MatchRepository;
import com.bsmart.scoretracker.service.TrackingEngineService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MatchTrackingScheduler {

    private final MatchRepository matchRepository;
    private final TrackingEngineService trackingEngine;

    /**
     * Initial scan at application startup to detect ongoing matches
     */
    @PostConstruct
    public void initialScan() {
        log.info("=== INITIAL MATCH SCAN AT STARTUP ===");

        try {
            // Wait a bit for application to fully initialize
            Thread.sleep(5000);

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime fourHoursAgo = now.minusHours(4);
            LocalDateTime oneHourFromNow = now.plusHours(1);

            // Find all SCHEDULED matches that might actually be in progress
            List<Match> potentiallyLiveMatches = matchRepository
                .findByTrackingEnabledTrueAndStatusAndKickoffUtcBetween(
                    MatchStatus.SCHEDULED, fourHoursAgo, oneHourFromNow);

            log.info("Found {} SCHEDULED matches between {} and {} - scanning for live matches",
                potentiallyLiveMatches.size(), fourHoursAgo, oneHourFromNow);

            for (Match match : potentiallyLiveMatches) {
                try {
                    log.info("Initial scan: Checking match {} - {} vs {} (kickoff: {})",
                        match.getId(), match.getHomeTeam(), match.getAwayTeam(), match.getKickoffUtc());
                    trackingEngine.trackMatch(match);
                } catch (Exception e) {
                    log.error("Error during initial scan of match {}: {}",
                        match.getId(), e.getMessage());
                }
            }

            log.info("=== INITIAL SCAN COMPLETED ===");
        } catch (Exception e) {
            log.error("Error during initial scan: {}", e.getMessage(), e);
        }
    }

    /**
     * High-frequency tracking: IN_PLAY matches (every 15 seconds)
     */
    @Scheduled(fixedDelay = 15000, initialDelay = 5000)
    public void trackLiveMatches() {
        log.debug("Running live match tracking cycle");

        List<Match> liveMatches = matchRepository.findByTrackingEnabledTrueAndStatusIn(
            Arrays.asList(MatchStatus.IN_PLAY));

        log.info("Tracking {} LIVE matches", liveMatches.size());

        for (Match match : liveMatches) {
            try {
                trackingEngine.trackMatch(match);
            } catch (Exception e) {
                log.error("Error tracking live match {}: {}",
                    match.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Medium-frequency tracking: PAUSED matches (every 45 seconds)
     */
    @Scheduled(fixedDelay = 45000, initialDelay = 10000)
    public void trackHalfTimeMatches() {
        log.debug("Running half-time match tracking cycle");

        List<Match> halfTimeMatches = matchRepository.findByTrackingEnabledTrueAndStatusIn(
            Arrays.asList(MatchStatus.PAUSED));

        log.info("Tracking {} PAUSED matches", halfTimeMatches.size());

        for (Match match : halfTimeMatches) {
            try {
                trackingEngine.trackMatch(match);
            } catch (Exception e) {
                log.error("Error tracking half-time match {}: {}",
                    match.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Low-frequency tracking: SCHEDULED matches near kickoff (every 60 seconds)
     * Extended window: 4 hours before to 1 hour after current time
     * This catches matches that started but weren't detected yet
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 15000)
    public void trackScheduledMatchesNearKickoff() {
        log.debug("Running scheduled match tracking cycle (near kickoff)");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fourHoursAgo = now.minusHours(4); // EXTENDED from 2 hours
        LocalDateTime oneHourFromNow = now.plusHours(1);

        List<Match> scheduledMatches = matchRepository
            .findByTrackingEnabledTrueAndStatusAndKickoffUtcBetween(
                MatchStatus.SCHEDULED, fourHoursAgo, oneHourFromNow);

        log.info("Tracking {} SCHEDULED matches near kickoff ({}h to +1h)",
            scheduledMatches.size(), fourHoursAgo.getHour());

        for (Match match : scheduledMatches) {
            try {
                trackingEngine.trackMatch(match);
            } catch (Exception e) {
                log.error("Error tracking scheduled match {}: {}",
                    match.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Very low-frequency tracking: SCHEDULED matches far from kickoff (every 10 minutes)
     */
    @Scheduled(fixedDelay = 600000, initialDelay = 20000)
    public void trackScheduledMatchesFarFromKickoff() {
        log.debug("Running scheduled match tracking cycle (far from kickoff)");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourFromNow = now.plusHours(1);
        LocalDateTime tomorrow = now.plusDays(1);

        List<Match> scheduledMatches = matchRepository
            .findByTrackingEnabledTrueAndStatusAndKickoffUtcBetween(
                MatchStatus.SCHEDULED, oneHourFromNow, tomorrow);

        log.info("Tracking {} SCHEDULED matches far from kickoff", scheduledMatches.size());

        for (Match match : scheduledMatches) {
            try {
                trackingEngine.trackMatch(match);
            } catch (Exception e) {
                log.error("Error tracking far scheduled match {}: {}",
                    match.getId(), e.getMessage(), e);
            }
        }
    }
}
