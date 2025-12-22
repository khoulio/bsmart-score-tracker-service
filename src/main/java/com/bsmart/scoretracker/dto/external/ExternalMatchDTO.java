package com.bsmart.scoretracker.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalMatchDTO {

    private Long id;

    @JsonProperty("teamDomicile")
    private ExternalTeamDTO teamDomicile;

    @JsonProperty("teamExterieur")
    private ExternalTeamDTO teamExterieur;

    @JsonProperty("teamDomicileId")
    private Long teamDomicileId;

    @JsonProperty("teamExterieurId")
    private Long teamExterieurId;

    @JsonProperty("phaseId")
    private Long phaseId;

    @JsonProperty("isProlongationEnabled")
    private Boolean isProlongationEnabled;

    @JsonProperty("isMonetized")
    private Boolean isMonetized;

    @JsonProperty("scoreTeamDomicile")
    private Integer scoreTeamDomicile;

    @JsonProperty("scoreTeamExterieur")
    private Integer scoreTeamExterieur;

    @JsonProperty("scoreTeamDomicileTAB")
    private Integer scoreTeamDomicileTAB;

    @JsonProperty("scoreTeamExterieurTAB")
    private Integer scoreTeamExterieurTAB;

    private String date; // ISO-8601 format

    @JsonProperty("isEnd")
    private Boolean isEnd;

    @JsonProperty("isBegin")
    private Boolean isBegin;

    @JsonProperty("teamDomicileWinner")
    private Boolean teamDomicileWinner;

    @JsonProperty("teamExterieurWinner")
    private Boolean teamExterieurWinner;

    @JsonProperty("isForAfrica")
    private Boolean isForAfrica;

    @JsonProperty("scoreProvider")
    private String scoreProvider;

    private String status; // FINISHED, IN_PLAY, SCHEDULED, etc.

    @JsonProperty("isHalfTimeSend")
    private Boolean isHalfTimeSend;

    @JsonProperty("isEndHalfTimeSend")
    private Boolean isEndHalfTimeSend;

    @JsonProperty("isForTest")
    private Boolean isForTest;

    private String statistics;

    @JsonProperty("statisticsWithName")
    private String statisticsWithName;

    @JsonProperty("timeAgoInString")
    private String timeAgoInString;
}
