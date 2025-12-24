package com.bsmart.scoretracker.controller.api;

import com.bsmart.scoretracker.dto.MatchDTO;
import com.bsmart.scoretracker.dto.MatchMetadata;
import com.bsmart.scoretracker.dto.external.WecanpronoMatchDTO;
import com.bsmart.scoretracker.model.enums.MatchStatus;
import com.bsmart.scoretracker.service.MatchMetadataService;
import com.bsmart.scoretracker.service.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
@Tag(name = "Matches", description = "Match management and tracking API")
public class MatchController {

    private final MatchService matchService;
    private final MatchMetadataService metadataService;

    @GetMapping
    @Operation(summary = "Get all matches or filter by status/competition")
    public ResponseEntity<List<MatchDTO>> getMatches(
            @RequestParam(required = false) Long phaseId,
            @RequestParam(required = false) MatchStatus status) {

        if (phaseId != null) {
            return ResponseEntity.ok(matchService.getMatchesByPhase(phaseId));
        }
        if (status != null) {
            return ResponseEntity.ok(matchService.getMatchesByStatus(status));
        }
        return ResponseEntity.ok(matchService.getAllMatches());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get match by ID")
    public ResponseEntity<MatchDTO> getMatchById(@PathVariable Long id) {
        return ResponseEntity.ok(matchService.getMatchById(id));
    }

    @GetMapping("/external/{externalId}")
    @Operation(summary = "Get match by external ID (rencontre_id from WECANPRONO)",
               description = "Permet à WECANPRONO de récupérer le score live via le rencontre_id")
    public ResponseEntity<MatchDTO> getMatchByExternalId(@PathVariable Long externalId) {
        return ResponseEntity.ok(matchService.getMatchByExternalId(externalId));
    }

    @PostMapping
    @Operation(summary = "Create new match")
    public ResponseEntity<MatchDTO> createMatch(@Valid @RequestBody MatchDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(matchService.createMatch(dto));
    }

    @PostMapping("/wecanprono")
    @Operation(summary = "Create or update match from Wecanprono",
            description = "Allows Wecanprono to create or update a match using its own data structure. " +
                          "The match is identified by the provider URL for updates.")
    public ResponseEntity<MatchDTO> createOrUpdateMatchFromWecanprono(@Valid @RequestBody WecanpronoMatchDTO dto) {
        return ResponseEntity.ok(matchService.createOrUpdateMatchFromWecanprono(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update match")
    public ResponseEntity<MatchDTO> updateMatch(
            @PathVariable Long id,
            @Valid @RequestBody MatchDTO dto) {
        return ResponseEntity.ok(matchService.updateMatch(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete match")
    public ResponseEntity<Void> deleteMatch(@PathVariable Long id) {
        matchService.deleteMatch(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/track/enable")
    @Operation(summary = "Enable tracking for match")
    public ResponseEntity<Void> enableTracking(@PathVariable Long id) {
        matchService.enableTracking(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/track/disable")
    @Operation(summary = "Disable tracking for match")
    public ResponseEntity<Void> disableTracking(@PathVariable Long id) {
        matchService.disableTracking(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/refresh")
    @Operation(summary = "Force refresh match data")
    public ResponseEntity<MatchDTO> refreshMatch(@PathVariable Long id) {
        return ResponseEntity.ok(matchService.refreshMatch(id));
    }

    @PostMapping("/extract-metadata")
    @Operation(summary = "Extract match metadata from OneFootball URL",
               description = "Parse OneFootball URL and extract team names, competition, date, venue automatically")
    public ResponseEntity<MatchMetadata> extractMetadata(@RequestParam String url) {
        return ResponseEntity.ok(metadataService.extractMetadataFromUrl(url));
    }
}
