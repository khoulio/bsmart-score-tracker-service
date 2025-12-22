package com.bsmart.scoretracker.service;

import com.bsmart.scoretracker.model.Match;
import com.bsmart.scoretracker.model.enums.MatchStatus;
import com.bsmart.scoretracker.model.enums.ProviderType;

public interface TrackingEngineService {

    /**
     * Tracks a single match by fetching and updating its data
     * @param match The match to track
     */
    void trackMatch(Match match);

    /**
     * Normalizes provider-specific status to MatchStatus
     * @param rawStatus Raw status from provider
     * @param providerType The provider type
     * @return Normalized MatchStatus
     */
    MatchStatus normalizeStatus(String rawStatus, ProviderType providerType);
}
