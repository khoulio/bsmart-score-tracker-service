package com.bsmart.scoretracker.scheduler;

import com.bsmart.scoretracker.service.PhaseMatchSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "sync.phases-matches", name = "enabled", havingValue = "true", matchIfMissing = false)
public class PhaseMatchSyncScheduler {

    private final PhaseMatchSyncService syncService;

    /**
     * Synchronise automatiquement les phases et matches de toutes les compétitions
     * Cadence configurable via sync.phases-matches.interval (défaut: 12 heures)
     */
    @Scheduled(fixedDelayString = "${sync.phases-matches.interval:43200000}") // 12 heures par défaut
    public void scheduledSynchronization() {
        log.info("=== Starting scheduled phase and match synchronization ===");

        try {
            int totalSynced = syncService.synchronizeAllSyncedCompetitions();
            log.info("=== Scheduled sync completed: {} phases synchronized ===", totalSynced);

        } catch (Exception e) {
            log.error("Error during scheduled phase/match synchronization: {}", e.getMessage(), e);
        }
    }
}
