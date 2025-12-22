package com.bsmart.scoretracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchSnapshot {

    private String status;      // Raw status from provider
    private Integer home;        // Home score
    private Integer away;        // Away score
    private String minute;       // Match minute
    private String rawStatus;    // Additional status info
    private boolean found;       // Was match data found
}
