package com.bsmart.scoretracker.controller.api;

import com.bsmart.scoretracker.dto.MatchEventDTO;
import com.bsmart.scoretracker.service.MatchEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches/{matchId}/events")
@RequiredArgsConstructor
@Tag(name = "Match Events", description = "Match event audit API")
public class MatchEventController {

    private final MatchEventService matchEventService;

    @GetMapping
    @Operation(summary = "Get all events for a match")
    public ResponseEntity<List<MatchEventDTO>> getMatchEvents(@PathVariable Long matchId) {
        return ResponseEntity.ok(matchEventService.getEventsByMatch(matchId));
    }
}
