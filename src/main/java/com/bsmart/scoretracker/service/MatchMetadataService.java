package com.bsmart.scoretracker.service;

import com.bsmart.scoretracker.dto.MatchMetadata;

public interface MatchMetadataService {
    /**
     * Extract match metadata (teams, competition, date, etc.) from OneFootball URL
     */
    MatchMetadata extractMetadataFromUrl(String url);
}
