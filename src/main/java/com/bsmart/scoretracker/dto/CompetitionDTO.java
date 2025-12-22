package com.bsmart.scoretracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionDTO {

    private Long id;
    private String code;
    private String name;
    private String country;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Sync fields
    private Long externalId;
    private String slug;
    private String logoUrl;
    private Integer nbUsers;
    private LocalDateTime dateStart;
    private LocalDateTime dateEnd;
    private Boolean isOpen;
    private Boolean isLeague;
    private Boolean isStarted;
    private Boolean isFeatured;
    private String backgroundUrl;
    private String sponsorLogoUrl;
    private LocalDateTime lastSyncAt;
}
