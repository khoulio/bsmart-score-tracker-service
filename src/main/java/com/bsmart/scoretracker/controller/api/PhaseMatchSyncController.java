package com.bsmart.scoretracker.controller.api;

import com.bsmart.scoretracker.dto.external.ExternalPhaseDTO;
import com.bsmart.scoretracker.service.PhaseMatchSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sync/phases-matches")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Phase & Match Synchronization", description = "APIs pour synchroniser les phases et matches depuis WECANPRONO-SERVICE")
public class PhaseMatchSyncController {

    private final PhaseMatchSyncService syncService;

    @GetMapping("/external/{competitionId}")
    @Operation(summary = "Récupérer les phases et matches externes",
            description = "Récupère les phases et matches depuis WECANPRONO-SERVICE sans les synchroniser en base")
    public ResponseEntity<List<ExternalPhaseDTO>> getExternalPhasesWithMatches(@PathVariable Long competitionId) {
        log.info("Fetching external phases with matches for competition {}", competitionId);
        List<ExternalPhaseDTO> phases = syncService.fetchExternalPhasesWithMatches(competitionId);
        return ResponseEntity.ok(phases);
    }

    @PostMapping("/competition/{competitionId}")
    @Operation(summary = "Synchroniser toutes les phases d'une compétition",
            description = "Synchronise toutes les phases et leurs matches pour une compétition donnée")
    public ResponseEntity<Map<String, Object>> syncAllPhasesForCompetition(@PathVariable Long competitionId) {
        log.info("Synchronizing all phases and matches for competition {}", competitionId);

        try {
            int syncedCount = syncService.synchronizePhasesAndMatchesForCompetition(competitionId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("competitionId", competitionId);
            response.put("syncedPhaseCount", syncedCount);
            response.put("message", syncedCount + " phases synchronisées avec succès");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error synchronizing competition {}: {}", competitionId, e.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("competitionId", competitionId);
            response.put("error", e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/phase/{competitionId}/{externalPhaseId}")
    @Operation(summary = "Synchroniser une phase spécifique",
            description = "Synchronise une phase et ses matches depuis WECANPRONO-SERVICE")
    public ResponseEntity<Map<String, Object>> syncSpecificPhase(
            @PathVariable Long competitionId,
            @PathVariable Long externalPhaseId) {

        log.info("Synchronizing phase {} for competition {}", externalPhaseId, competitionId);

        try {
            boolean success = syncService.synchronizePhase(competitionId, externalPhaseId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("competitionId", competitionId);
            response.put("externalPhaseId", externalPhaseId);
            response.put("message", success
                    ? "Phase synchronisée avec succès"
                    : "Échec de la synchronisation");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error synchronizing phase {}: {}", externalPhaseId, e.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("competitionId", competitionId);
            response.put("externalPhaseId", externalPhaseId);
            response.put("error", e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/all")
    @Operation(summary = "Synchroniser toutes les compétitions",
            description = "Synchronise les phases et matches de toutes les compétitions ayant un externalId")
    public ResponseEntity<Map<String, Object>> syncAllCompetitions() {
        log.info("Synchronizing all competitions with externalId");

        try {
            int totalSynced = syncService.synchronizeAllSyncedCompetitions();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalSyncedPhases", totalSynced);
            response.put("message", totalSynced + " phases synchronisées au total");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error synchronizing all competitions: {}", e.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }
}
