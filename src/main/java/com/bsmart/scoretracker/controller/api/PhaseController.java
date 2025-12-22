package com.bsmart.scoretracker.controller.api;

import com.bsmart.scoretracker.dto.PhaseDTO;
import com.bsmart.scoretracker.service.PhaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/phases")
@RequiredArgsConstructor
@Tag(name = "Phases", description = "Phase management API")
public class PhaseController {

    private final PhaseService phaseService;

    @GetMapping
    @Operation(summary = "Get all phases or by competition")
    public ResponseEntity<List<PhaseDTO>> getPhases(
            @RequestParam(required = false) Long competitionId) {
        if (competitionId != null) {
            return ResponseEntity.ok(phaseService.getPhasesByCompetition(competitionId));
        }
        return ResponseEntity.ok(phaseService.getAllPhases());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get phase by ID")
    public ResponseEntity<PhaseDTO> getPhaseById(@PathVariable Long id) {
        return ResponseEntity.ok(phaseService.getPhaseById(id));
    }

    @GetMapping("/{id}/matches")
    @Operation(summary = "Get matches for a phase")
    public ResponseEntity<List<PhaseDTO>> getPhaseMatches(@PathVariable Long id) {
        // This will be handled by MatchController
        return ResponseEntity.ok().build();
    }

    @PostMapping
    @Operation(summary = "Create new phase")
    public ResponseEntity<PhaseDTO> createPhase(@Valid @RequestBody PhaseDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(phaseService.createPhase(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update phase")
    public ResponseEntity<PhaseDTO> updatePhase(
            @PathVariable Long id,
            @Valid @RequestBody PhaseDTO dto) {
        return ResponseEntity.ok(phaseService.updatePhase(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete phase")
    public ResponseEntity<Void> deletePhase(@PathVariable Long id) {
        phaseService.deletePhase(id);
        return ResponseEntity.noContent().build();
    }
}
