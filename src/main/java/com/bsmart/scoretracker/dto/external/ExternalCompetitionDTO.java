package com.bsmart.scoretracker.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour mapper la réponse de l'API WECANPRONO-SERVICE
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalCompetitionDTO {

    private Long id;
    private String name;
    private String slug;
    private String url; // Logo URL

    @JsonProperty("nbUsers")
    private Integer nbUsers;

    @JsonProperty("dateStart")
    private String dateStart;

    @JsonProperty("dateEnd")
    private String dateEnd;

    @JsonProperty("isOpen")
    private Boolean isOpen;

    @JsonProperty("isLeague")
    private Boolean isLeague;

    @JsonProperty("isFanGroupCreated")
    private Boolean isFanGroupCreated;

    @JsonProperty("isStarted")
    private Boolean isStarted;

    @JsonProperty("isPubBannerActivated")
    private Boolean isPubBannerActivated;

    @JsonProperty("isPubInterstitielActivated")
    private Boolean isPubInterstitielActivated;

    @JsonProperty("isPubRewardActivated")
    private Boolean isPubRewardActivated;

    @JsonProperty("nextMatch")
    private String nextMatch;

    @JsonProperty("isCalculateRank")
    private Boolean isCalculateRank;

    @JsonProperty("isFeaturedCompetition")
    private Boolean isFeaturedCompetition;

    @JsonProperty("isShowLiveActivity")
    private Boolean isShowLiveActivity;

    @JsonProperty("urlBackgroundForNotification")
    private String urlBackgroundForNotification;

    @JsonProperty("logoSponsor")
    private String logoSponsor;
}
