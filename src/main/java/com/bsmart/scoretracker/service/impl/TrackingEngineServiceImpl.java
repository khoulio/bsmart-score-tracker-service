package com.bsmart.scoretracker.service.impl;

import com.bsmart.scoretracker.dto.MatchSnapshot;
import com.bsmart.scoretracker.exception.ScraperException;
import com.bsmart.scoretracker.model.Match;
import com.bsmart.scoretracker.model.enums.EventType;
import com.bsmart.scoretracker.model.enums.MatchStatus;
import com.bsmart.scoretracker.model.enums.ProviderType;
import com.bsmart.scoretracker.repository.MatchRepository;
import com.bsmart.scoretracker.scraper.MatchScraperProvider;
import com.bsmart.scoretracker.scraper.ScraperProviderFactory;
import com.bsmart.scoretracker.service.MatchEventService;
import com.bsmart.scoretracker.service.TrackingEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingEngineServiceImpl implements TrackingEngineService {

    private final ScraperProviderFactory scraperFactory;
    private final MatchRepository matchRepository;
    private final MatchEventService matchEventService;

    @Value("${tracking.anti-flapping.confirmations:3}")
    private int requiredConfirmations;

    @Value("${tracking.max-errors:5}")
    private int maxErrors;

    @Override
    @Transactional
    public void trackMatch(Match match) {
        log.debug("Tracking match {}: {} vs {}",
            match.getId(), match.getHomeTeam(), match.getAwayTeam());

        try {
            // Fetch data from provider
            MatchScraperProvider provider = scraperFactory.getProvider(match.getProvider());
            MatchSnapshot snapshot = provider.fetch(match.getMatchUrl());

            if (!snapshot.isFound()) {
                handleScrapeFailed(match, "Match data not found");
                return;
            }

            // CRITICAL: Check if match has started
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime kickoff = match.getKickoffUtc();
            boolean matchHasStarted = now.isAfter(kickoff);

            if (!matchHasStarted) {
                // Match hasn't started yet - ignore scraped data, keep as SCHEDULED
                log.info("Match {} hasn't started yet (kickoff: {}). Ignoring scraped data and keeping SCHEDULED status.",
                    match.getId(), kickoff);

                match.setLastFetchUtc(LocalDateTime.now());
                match.setErrorCount(0);
                match.setLastError(null);

                // Keep status as SCHEDULED and scores as null
                if (match.getStatus() == null) {
                    match.setStatus(MatchStatus.SCHEDULED);
                }

                matchRepository.save(match);
                return;
            }

            // Match has started - process normally
            // Normalize status
            MatchStatus normalizedStatus = normalizeStatus(
                snapshot.getStatus(), match.getProvider());

            // Update last fetch
            match.setLastFetchUtc(LocalDateTime.now());
            match.setRawStatus(snapshot.getRawStatus());
            match.setMinute(snapshot.getMinute());
            match.setErrorCount(0);
            match.setLastError(null);

            // Process status change with anti-flapping
            processStatusChange(match, normalizedStatus);

            // Process score change (pass status to allow FINISHED corrections)
            processScoreChange(match, snapshot.getHome(), snapshot.getAway(), normalizedStatus);

            matchRepository.save(match);

            log.info("SCRAPE_OK: Match {} - Status: {}, Score: {}:{}",
                match.getId(), normalizedStatus, snapshot.getHome(), snapshot.getAway());

        } catch (ScraperException e) {
            handleScrapeFailed(match, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error tracking match {}: {}",
                match.getId(), e.getMessage(), e);
            handleScrapeFailed(match, "Unexpected error: " + e.getMessage());
        }
    }

    private void processStatusChange(Match match, MatchStatus newStatus) {
        MatchStatus currentStatus = match.getStatus();

        // EXCEPTION: Auto-correction for wrongly marked FINISHED matches
        // MUST BE FIRST - before the terminal state check!
        if (currentStatus == MatchStatus.FINISHED &&
            (newStatus == MatchStatus.IN_PLAY || newStatus == MatchStatus.PAUSED) &&
            match.getMinute() != null && !match.getMinute().isEmpty()) {

            log.warn("AUTO-CORRECTION: Match {} was marked FINISHED but is actually {} (minute: {}). Forcing correction and re-enabling tracking.",
                match.getId(), newStatus, match.getMinute());

            // Force the correction immediately (bypass anti-flapping)
            match.setStatus(newStatus);
            match.setTrackingEnabled(true); // Re-enable tracking
            match.setStatusCandidate(null);
            match.setConsecutiveSameCandidate(0);

            matchEventService.createEvent(match, EventType.STATUS_CHANGE,
                currentStatus, newStatus, null, null, null, null,
                match.getMinute(), match.getRawStatus(), "AUTO_CORRECTION");

            log.info("Status changed for match {}: {} -> {} (AUTO-CORRECTED)",
                match.getId(), currentStatus, newStatus);
            return;
        }

        // Check if status is terminal (after auto-correction check!)
        if (currentStatus == MatchStatus.FINISHED) {
            log.debug("Match {} already finished, ignoring status change", match.getId());
            return;
        }

        // Same status, no change needed
        if (currentStatus == newStatus) {
            // Reset candidate if status is stable, but NOT if it's the one we are tracking
            if (match.getStatusCandidate() != null && !newStatus.equals(match.getStatusCandidate())) {
                match.setStatusCandidate(null);
                match.setConsecutiveSameCandidate(0);
                match.setStatusCandidateSinceUtc(null);
            }
            return;
        }

        // Validate transition
        if (!isValidTransition(currentStatus, newStatus)) {
            log.warn("Invalid status transition: {} -> {} for match {}",
                currentStatus, newStatus, match.getId());
            return;
        }

        // Anti-flapping logic
        if (match.getStatusCandidate() == null ||
            !match.getStatusCandidate().equals(newStatus)) {
            // New candidate status
            match.setStatusCandidate(newStatus);
            match.setConsecutiveSameCandidate(1);
            match.setStatusCandidateSinceUtc(LocalDateTime.now());

            log.debug("New status candidate for match {}: {} (1/{})",
                match.getId(), newStatus, requiredConfirmations);

            matchEventService.createEvent(match, EventType.ANTI_FLAPPING_ACTIVATED,
                currentStatus, newStatus, null, null, null, null,
                match.getMinute(), match.getRawStatus(), "SCHEDULER");

        } else {
            // Same candidate, increment counter
            match.setConsecutiveSameCandidate(
                match.getConsecutiveSameCandidate() + 1);

            log.debug("Status candidate confirmed for match {}: {} ({}/{})",
                match.getId(), newStatus, match.getConsecutiveSameCandidate(),
                requiredConfirmations);

            // Check if we have enough confirmations (or FINISHED with strong proof)
            boolean shouldApply = match.getConsecutiveSameCandidate() >= requiredConfirmations;

            // FINISHED can be applied with 1 confirmation if we have strong proof
            if (newStatus == MatchStatus.FINISHED && match.getConsecutiveSameCandidate() >= 1) {
                shouldApply = true;
            }

            if (shouldApply) {
                // Apply status change
                applyStatusChange(match, newStatus);
            }
        }
    }

    private void applyStatusChange(Match match, MatchStatus newStatus) {
        MatchStatus oldStatus = match.getStatus();
        match.setStatus(newStatus);

        // Reset anti-flapping
        match.setStatusCandidate(null);
        match.setConsecutiveSameCandidate(0);
        match.setStatusCandidateSinceUtc(null);

        log.info("Status changed for match {}: {} -> {}",
            match.getId(), oldStatus, newStatus);

        // Create audit event
        matchEventService.createEvent(match, EventType.STATUS_CHANGE,
            oldStatus, newStatus, null, null, null, null,
            match.getMinute(), match.getRawStatus(), "SCHEDULER");

        // Disable tracking if finished
        if (newStatus == MatchStatus.FINISHED) {
            match.setTrackingEnabled(false);
            log.info("Tracking disabled for finished match {}", match.getId());
        }
    }

    private void processScoreChange(Match match, Integer newHome, Integer newAway, MatchStatus newStatus) {
        Integer oldHome = match.getScoreHome();
        Integer oldAway = match.getScoreAway();

        // Check if scores changed
        boolean homeChanged = !Objects.equals(oldHome, newHome);
        boolean awayChanged = !Objects.equals(oldAway, newAway);

        if (homeChanged || awayChanged) {
            // Check for score inversion correction (0:1 -> 1:0 is a fix, not rollback)
            boolean isInversionCorrection =
                oldHome != null && oldAway != null &&
                newHome != null && newAway != null &&
                oldHome.equals(newAway) && oldAway.equals(newHome);

            if (isInversionCorrection) {
                log.warn("Score inversion correction for match {}: {}:{} -> {}:{} (inverting)",
                    match.getId(), oldHome, oldAway, newHome, newAway);
                // Allow the correction
            } else if (newStatus == MatchStatus.FINISHED) {
                // FINISHED status has authoritative final score - allow correction even if it looks like rollback
                log.warn("FINISHED score correction for match {}: {}:{} -> {}:{} (final score is authoritative)",
                    match.getId(), oldHome, oldAway, newHome, newAway);
                // Allow the correction
            } else if (isScoreRollback(oldHome, oldAway, newHome, newAway)) {
                // Normal rollback protection (only during live play)
                log.warn("Score rollback detected for match {}: {}:{} -> {}:{}",
                    match.getId(), oldHome, oldAway, newHome, newAway);
                return;
            }

            match.setScoreHome(newHome);
            match.setScoreAway(newAway);

            log.info("Score changed for match {}: {}:{} -> {}:{}",
                match.getId(), oldHome, oldAway, newHome, newAway);

            // Create audit event
            matchEventService.createEvent(match, EventType.SCORE_CHANGE,
                null, null, oldHome, oldAway, newHome, newAway,
                match.getMinute(), match.getRawStatus(), "SCHEDULER");
        }
    }

    private boolean isValidTransition(MatchStatus from, MatchStatus to) {
        // SCHEDULED -> IN_PLAY, PAUSED, FINISHED (tolerated: started late)
        // IN_PLAY -> PAUSED, FINISHED
        // PAUSED -> IN_PLAY, FINISHED
        // FINISHED -> (terminal, no transitions)

        if (from == MatchStatus.FINISHED) {
            return false; // Terminal state
        }

        if (from == MatchStatus.SCHEDULED) {
            return to == MatchStatus.IN_PLAY || to == MatchStatus.PAUSED ||
                   to == MatchStatus.FINISHED;
        }

        if (from == MatchStatus.IN_PLAY) {
            return to == MatchStatus.PAUSED || to == MatchStatus.FINISHED;
        }

        if (from == MatchStatus.PAUSED) {
            return to == MatchStatus.IN_PLAY || to == MatchStatus.FINISHED;
        }

        return false;
    }

    private boolean isScoreRollback(Integer oldHome, Integer oldAway,
                                    Integer newHome, Integer newAway) {
        // Detect if new score is less than old score (rollback)
        if (oldHome == null || oldAway == null || newHome == null || newAway == null) {
            return false;
        }
        return newHome < oldHome || newAway < oldAway;
    }

    @Override
    public MatchStatus normalizeStatus(String rawStatus, ProviderType providerType) {
        if (rawStatus == null) {
            return MatchStatus.SCHEDULED;
        }

        String normalized = rawStatus.toUpperCase().trim();

        // Provider-specific normalization
        switch (providerType) {
            case ONE_FOOTBALL:
                return normalizeOneFootballStatus(normalized);
            case LIVE_SCORE:
                return normalizeLiveScoreStatus(normalized);
            default:
                return MatchStatus.SCHEDULED;
        }
    }

    private MatchStatus normalizeOneFootballStatus(String status) {
        if (status.contains("FT") || status.contains("FINISHED") ||
            status.contains("FULL TIME") || status.contains("FULLTIME")) {
            return MatchStatus.FINISHED;
        }
        if (status.contains("HT") || status.contains("HALF TIME") ||
            status.contains("HALFTIME")) {
            return MatchStatus.PAUSED;
        }
        if (status.contains("LIVE") || status.contains("'") ||
            status.matches(".*\\d+.*")) {
            return MatchStatus.IN_PLAY;
        }
        return MatchStatus.SCHEDULED;
    }

    private MatchStatus normalizeLiveScoreStatus(String status) {
        // Check HALFTIME before FT to avoid matching "FT" in "HALFTIME"
        if (status.contains("HALFTIME") || status.contains("HALF TIME") ||
            status.contains("HT")) {
            return MatchStatus.PAUSED;
        }
        if (status.contains("FT") || status.contains("FINISHED") ||
            status.contains("FULL TIME")) {
            return MatchStatus.FINISHED;
        }
        if (status.contains("LIVE") || status.matches(".*\\d+'.*")) {
            return MatchStatus.IN_PLAY;
        }
        return MatchStatus.SCHEDULED;
    }

    private void handleScrapeFailed(Match match, String errorMessage) {
        match.setErrorCount(match.getErrorCount() + 1);
        match.setLastError(errorMessage);
        match.setLastFetchUtc(LocalDateTime.now());

        if (match.getErrorCount() >= maxErrors) {
            log.error("SCRAPE_FAIL: Match {} exceeded max errors ({}), disabling tracking",
                match.getId(), maxErrors);
            match.setTrackingEnabled(false);

            matchEventService.createEvent(match, EventType.ERROR_DETECTED,
                null, null, null, null, null, null,
                null, errorMessage, "SCHEDULER");
        } else {
            log.warn("SCRAPE_FAIL: Match {} error ({}/{}): {}",
                match.getId(), match.getErrorCount(), maxErrors, errorMessage);
        }

        matchRepository.save(match);
    }
}
