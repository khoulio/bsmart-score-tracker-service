package com.bsmart.scoretracker.scheduler;

import com.bsmart.scoretracker.service.CompetitionSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler pour synchroniser automatiquement les compétitions
 * depuis WECANPRONO-SERVICE
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    value = "sync.competitions.enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class CompetitionSyncScheduler {

    private final CompetitionSyncService syncService;

    /**
     * Synchronisation automatique toutes les 6 heures
     */
    @Scheduled(fixedDelayString = "${sync.competitions.interval:21600000}") // 6 heures par défaut
    public void scheduledSync() {
        log.info("Starting scheduled competition synchronization");

        try {
            int syncedCount = syncService.synchronizeAllCompetitions();
            log.info("Scheduled sync completed: {} competitions synchronized", syncedCount);
        } catch (Exception e) {
            log.error("Error during scheduled competition sync: {}", e.getMessage(), e);
        }
    }
}
