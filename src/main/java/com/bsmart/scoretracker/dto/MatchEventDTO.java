package com.bsmart.scoretracker.dto;

import com.bsmart.scoretracker.model.enums.EventType;
import com.bsmart.scoretracker.model.enums.MatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchEventDTO {

    private Long id;
    private Long matchId;
    private LocalDateTime timestamp;
    private EventType eventType;
    private MatchStatus oldStatus;
    private MatchStatus newStatus;
    private Integer oldScoreHome;
    private Integer oldScoreAway;
    private Integer newScoreHome;
    private Integer newScoreAway;
    private String minute;
    private String rawStatus;
    private String triggeredBy;
}
