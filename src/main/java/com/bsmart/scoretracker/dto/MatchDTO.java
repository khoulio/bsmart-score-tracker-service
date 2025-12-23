package com.bsmart.scoretracker.dto;

import com.bsmart.scoretracker.model.enums.MatchStatus;
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
public class MatchDTO {

    private Long id;
    private Long phaseId;
    private String phaseName;
    private String competitionName;
    private String homeTeam;
    private String awayTeam;
    private LocalDateTime kickoffUtc;
    private ProviderType provider;
    private String matchUrl;
    private Boolean trackingEnabled;
    private MatchStatus status;
    private Integer scoreHome;
    private Integer scoreAway;
    private String minute;
    private String rawStatus;
    private LocalDateTime lastFetchUtc;
    private Integer errorCount;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Sync fields (read-only in manual creation)
    private Long externalId;
    private Long teamDomicileId;
    private Long teamExterieurId;
    private Boolean isProlongationEnabled;
    private Integer scoreHomeTAB;
    private Integer scoreAwayTAB;
    private Boolean winnerHomeTAB;
    private Boolean winnerAwayTAB;
    private Boolean isMonetized;
    private Boolean isHalfTimeSend;
    private Boolean isEndHalfTimeSend;
    private Boolean isForTest;
    private String externalScoreProvider;
    private LocalDateTime lastSyncAt;

    // Anti-flapping fields (read-only)
    private MatchStatus statusCandidate;
    private Integer consecutiveSameCandidate;
    private LocalDateTime statusCandidateSinceUtc;
    private Boolean halfTimeSeen;
}
