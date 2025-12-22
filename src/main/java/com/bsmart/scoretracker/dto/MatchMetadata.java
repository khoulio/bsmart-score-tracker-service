package com.bsmart.scoretracker.dto;

import com.bsmart.scoretracker.model.enums.ProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchMetadata {
    private String homeTeam;
    private String awayTeam;
    private String competition;
    private String venue;
    private LocalDateTime kickoffUtc;
    private String matchUrl;
    private ProviderType provider;  // Auto-detected from URL

    // Optional current data
    private Integer homeScore;
    private Integer awayScore;
    private String status;
    private String minute;
}
