package com.bsmart.scoretracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhaseDTO {

    private Long id;
    private Long competitionId;
    private String competitionName;
    private String name;
    private String stage;
    private Integer matchDay;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean trackingEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Sync fields (read-only in manual creation)
    private Long externalId;
    private Boolean isProlongationEnabled;
    private Boolean isMonetized;
    private Boolean isFull;
    private Boolean isNextPhaseCreated;
    private Boolean isStarted;
    private Integer multiply;
    private LocalDateTime lastSyncAt;
}
