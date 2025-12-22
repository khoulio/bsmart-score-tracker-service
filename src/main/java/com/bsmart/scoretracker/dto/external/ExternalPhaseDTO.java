package com.bsmart.scoretracker.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalPhaseDTO {

    private Long id;
    private String name;

    @JsonProperty("competitionId")
    private Long competitionId;

    private ExternalCompetitionDTO competition;

    @JsonProperty("isProlongationEnabled")
    private Boolean isProlongationEnabled;

    @JsonProperty("isMonetized")
    private Boolean isMonetized;

    private String created; // ISO-8601 format

    @JsonProperty("isFull")
    private Boolean isFull;

    @JsonProperty("isNextPhaseCreated")
    private Boolean isNextPhaseCreated;

    @JsonProperty("isStarted")
    private Boolean isStarted;

    private Integer multiply;

    private List<ExternalMatchDTO> rencontres; // List of matches
}
