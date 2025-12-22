package com.bsmart.scoretracker.controller.api;

import com.bsmart.scoretracker.dto.CompetitionDTO;
import com.bsmart.scoretracker.service.CompetitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/competitions")
@RequiredArgsConstructor
@Tag(name = "Competitions", description = "Competition management API")
public class CompetitionController {

    private final CompetitionService competitionService;

    @GetMapping
    @Operation(summary = "Get all competitions")
    public ResponseEntity<List<CompetitionDTO>> getAllCompetitions() {
        return ResponseEntity.ok(competitionService.getAllCompetitions());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get competition by ID")
    public ResponseEntity<CompetitionDTO> getCompetitionById(@PathVariable Long id) {
        return ResponseEntity.ok(competitionService.getCompetitionById(id));
    }

    @PostMapping
    @Operation(summary = "Create new competition")
    public ResponseEntity<CompetitionDTO> createCompetition(@Valid @RequestBody CompetitionDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(competitionService.createCompetition(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update competition")
    public ResponseEntity<CompetitionDTO> updateCompetition(
            @PathVariable Long id,
            @Valid @RequestBody CompetitionDTO dto) {
        return ResponseEntity.ok(competitionService.updateCompetition(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete competition")
    public ResponseEntity<Void> deleteCompetition(@PathVariable Long id) {
        competitionService.deleteCompetition(id);
        return ResponseEntity.noContent().build();
    }
}
