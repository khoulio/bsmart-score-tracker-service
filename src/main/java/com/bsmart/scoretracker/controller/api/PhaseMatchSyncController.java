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
