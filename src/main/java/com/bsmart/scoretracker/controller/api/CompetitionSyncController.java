package com.bsmart.scoretracker.controller.api;

import com.bsmart.scoretracker.dto.external.ExternalCompetitionDTO;
import com.bsmart.scoretracker.service.CompetitionSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sync/competitions")
@RequiredArgsConstructor
@Tag(name = "Competition Sync", description = "Synchronisation des compétitions avec WECANPRONO-SERVICE")
public class CompetitionSyncController {

    private final CompetitionSyncService syncService;

    @PostMapping("/all")
    @Operation(summary = "Synchroniser toutes les compétitions")
    public ResponseEntity<Map<String, Object>> syncAllCompetitions() {
        int syncedCount = syncService.synchronizeAllCompetitions();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("syncedCount", syncedCount);
        response.put("message", syncedCount + " compétitions synchronisées avec succès");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{externalId}")
    @Operation(summary = "Synchroniser une compétition spécifique")
    public ResponseEntity<Map<String, Object>> syncCompetition(@PathVariable Long externalId) {
        boolean success = syncService.synchronizeCompetition(externalId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ?
            "Compétition synchronisée avec succès" :
            "Erreur lors de la synchronisation");

        return success ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/external")
    @Operation(summary = "Récupérer les compétitions depuis l'API externe (sans synchroniser)")
    public ResponseEntity<List<ExternalCompetitionDTO>> fetchExternalCompetitions() {
        List<ExternalCompetitionDTO> competitions = syncService.fetchExternalCompetitions();
        return ResponseEntity.ok(competitions);
    }
}
