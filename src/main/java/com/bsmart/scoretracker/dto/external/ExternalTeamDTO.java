package com.bsmart.scoretracker.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalTeamDTO {

    private Long id;
    private String name;
    private String nameFr;
    private String url; // Logo URL
    private String slug;
    private String flag;
    private String urlSlogan;
    private Long avatarId;
    private Boolean isActive;
    private String clearName;
}
